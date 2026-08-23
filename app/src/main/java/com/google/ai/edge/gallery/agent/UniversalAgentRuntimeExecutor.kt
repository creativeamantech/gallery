/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ai.edge.gallery.agent

import android.content.Context
import android.util.Log
import com.google.ai.edge.gallery.capabilities.CapabilityDispatcher
import com.google.ai.edge.gallery.capabilities.CapabilityRegistry
import com.google.ai.edge.gallery.capabilities.CapabilityResult
import com.google.ai.edge.gallery.capabilities.CapabilityExecutionContext
import com.google.ai.edge.gallery.model.provider.MessageRole
import com.google.ai.edge.gallery.model.provider.ModelContent
import com.google.ai.edge.gallery.model.provider.ModelEvent
import com.google.ai.edge.gallery.model.provider.ModelMessage
import com.google.ai.edge.gallery.model.provider.ModelProvider
import com.google.ai.edge.gallery.model.provider.ModelRequest
import com.google.ai.edge.gallery.model.provider.ToolCallRequest
import com.google.ai.edge.gallery.model.provider.toNative
import com.google.ai.edge.gallery.model.provider.toStructuredData
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.takeWhile

private const val TAG = "UniversalAgentRuntimeExecutor"

/**
 * Universal Agent Runtime Executor.
 * 
 * Orchestrates the ReAct loop strictly using the model-independent [ModelProvider] and [CapabilityDispatcher].
 * It depends on Kotlin Coroutines and [AgentState] to manage iterative reasoning without coupling to
 * any particular model vendor.
 */
class UniversalAgentRuntimeExecutor(
    private val modelProviderFactory: (AgentRuntimeConfig) -> ModelProvider,
    private val capabilityRegistry: CapabilityRegistry,
    private val capabilityDispatcher: CapabilityDispatcher,
    private val maxIterations: Int = 10,
    private val maxToolCalls: Int = 20,
    private val maxRetries: Int = 3
) : AgentRuntimeExecutor {

    private val activeJob = AtomicReference<Job?>(null)
    private var modelProvider: ModelProvider? = null
    private var activeConfig: AgentRuntimeConfig? = null

    override suspend fun initialize(
        context: Context,
        config: AgentRuntimeConfig,
        onDone: (String) -> Unit
    ) {
        try {
            activeConfig = config
            modelProvider = modelProviderFactory(config)
            onDone("")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize UniversalAgentRuntimeExecutor", e)
            onDone(e.message ?: "Unknown initialization error")
        }
    }

    override fun executeStream(
        context: AgentExecutionContext,
        request: AgentRequest
    ): Flow<AgentEvent> = flow {
        val currentJob = currentCoroutineContext()[Job]
        activeJob.set(currentJob)

        try {
            val taskId = activeConfig?.taskId ?: ""
            val state = AgentState(taskId = taskId)
            val initialContents = mutableListOf<ModelContent>(ModelContent.Text(request.query))
            request.attachments.mapNotNull { AndroidModelContentMapper.map(it) }.forEach { initialContents.add(it) }

            state.history.add(ModelMessage(MessageRole.USER, initialContents))
            state.status = AgentStatus.RUNNING
            emit(AgentEvent.LoopInitiated(request))

            val provider = modelProvider ?: error("UniversalAgentRuntimeExecutor not initialized")

            // Main Orchestration Loop
            while (state.status == AgentStatus.RUNNING || state.status == AgentStatus.WAITING_FOR_CAPABILITY) {
                if (state.iterationCount >= maxIterations) {
                    emit(AgentEvent.Error("Maximum iterations ($maxIterations) reached."))
                    state.status = AgentStatus.FAILED
                    break
                }
                state.iterationCount++
                state.status = AgentStatus.RUNNING

                val modelRequest = ModelRequest(
                    systemInstruction = null, // Can be injected via config in the future if required
                    history = state.history.toList(),
                    capabilities = capabilityRegistry.getAllCapabilities()
                )

                var pendingToolCall: ToolCallRequest? = null

                try {
                    provider.generate(modelRequest).takeWhile { event ->
                        when (event) {
                            is ModelEvent.TextChunk -> {
                                state.finalOutput.append(event.text)
                                emit(AgentEvent.StreamToken(event.text, event.thinking, false))
                                true
                            }
                            is ModelEvent.ToolCallRequestEvent -> {
                                pendingToolCall = event.request
                                false // Break the inner collection stream to handle the capability
                            }
                            is ModelEvent.Completion -> {
                                state.status = AgentStatus.COMPLETED
                                emit(AgentEvent.StreamToken("", null, true))
                                false // End collection naturally
                            }
                            is ModelEvent.Error -> {
                                emit(AgentEvent.Error(event.message))
                                state.status = AgentStatus.FAILED
                                false // End collection on error
                            }
                        }
                    }.collect { /* consumed by takeWhile */ }
                } catch (e: CancellationException) {
                    throw e // Let it propagate down to the outer catch
                } catch (e: Exception) {
                    emit(AgentEvent.Error("Generation error: ${e.message}"))
                    state.status = AgentStatus.FAILED
                    break
                }

                // If the model requested a tool execution, handle it outside the flow collection
                if (pendingToolCall != null) {
                    val toolCall = pendingToolCall!!
                    state.status = AgentStatus.WAITING_FOR_CAPABILITY
                    state.toolCallCount++

                    if (state.toolCallCount > maxToolCalls) {
                        emit(AgentEvent.Error("Maximum capability calls ($maxToolCalls) reached."))
                        state.status = AgentStatus.FAILED
                        break
                    }

                    emit(AgentEvent.CapabilityRequested(toolCall.name, toolCall.arguments.toNative() as? Map<String, Any?> ?: emptyMap()))
                    state.history.add(ModelMessage(MessageRole.MODEL, listOf(ModelContent.ToolCall(toolCall))))

                    val capability = capabilityRegistry.getCapability(toolCall.name)
                    
                    val toolResultData = if (capability == null) {
                        emit(AgentEvent.CapabilityFailed(toolCall.name, "Capability not found"))
                        mapOf("error" to "Capability not found").toStructuredData()
                    } else {
                        emit(AgentEvent.CapabilityStarted(toolCall.name))
                        
                        // Execute through dispatcher
                        val capContext = object : CapabilityExecutionContext {
                            override val taskId: String = state.taskId
                            override val isHeadless: Boolean = false
                        }
                        val dispatchResult = capabilityDispatcher.dispatch(
                            capabilityName = toolCall.name,
                            arguments = toolCall.arguments.toNative() as? Map<String, Any?> ?: emptyMap(),
                            context = capContext
                        )
                        
                        when (dispatchResult) {
                            is CapabilityResult.Success -> {
                                emit(AgentEvent.CapabilityCompleted(toolCall.name, dispatchResult.data))
                                dispatchResult.data.toStructuredData()
                            }
                            is CapabilityResult.Error -> {
                                emit(AgentEvent.CapabilityFailed(toolCall.name, dispatchResult.message))
                                state.retryCount++
                                if (state.retryCount > maxRetries) {
                                    emit(AgentEvent.Error("Maximum capability retries ($maxRetries) reached."))
                                    state.status = AgentStatus.FAILED
                                }
                                mapOf("error" to dispatchResult.message).toStructuredData()
                            }
                            is CapabilityResult.PermissionRequired -> {
                                emit(AgentEvent.CapabilityFailed(toolCall.name, "Permission required"))
                                mapOf("error" to "Permission required", "status" to "permission_required").toStructuredData()
                            }
                            is CapabilityResult.UserActionRequired -> {
                                emit(AgentEvent.CapabilityFailed(toolCall.name, "User action required"))
                                mapOf("error" to "User action required", "status" to "user_action_required").toStructuredData()
                            }
                        }
                    }

                    // Append the tool result to the history so the model can process it in the next loop
                    if (state.status != AgentStatus.FAILED) {
                        state.history.add(ModelMessage(MessageRole.TOOL, listOf(ModelContent.ToolResult(toolCall.id, toolResultData))))
                    }
                }
            }

            if (state.status == AgentStatus.COMPLETED) {
                emit(AgentEvent.LoopTerminated(state.finalOutput.toString()))
            }
        } catch (e: CancellationException) {
            emit(AgentEvent.AgentCancelled)
            throw e
        } catch (e: Exception) {
            emit(AgentEvent.Error(e.message ?: "Unknown error"))
        } finally {
            activeJob.compareAndSet(currentCoroutineContext()[Job], null)
        }
    }

    override suspend fun execute(
        context: AgentExecutionContext,
        request: AgentRequest
    ): AgentResponse {
        var finalOutput = ""
        var isSuccess = true
        try {
            executeStream(context, request).collect { event ->
                when (event) {
                    is AgentEvent.LoopTerminated -> {
                        finalOutput = event.finalResponse
                        isSuccess = true
                    }
                    is AgentEvent.Error -> {
                        finalOutput = event.errorMessage
                        isSuccess = false
                    }
                    else -> {}
                }
            }
        } catch (e: CancellationException) {
            isSuccess = false
            finalOutput = "Agent cancelled"
        }
        return AgentResponse(output = finalOutput, isSuccessful = isSuccess)
    }

    override fun interrupt() {
        activeJob.getAndSet(null)?.cancel(CancellationException("Interrupted by user"))
    }

    override suspend fun resetSession(config: AgentRuntimeConfig) {
        activeConfig = config
        modelProvider = modelProviderFactory(config)
    }

    override fun cleanUp(onDone: () -> Unit) {
        interrupt()
        modelProvider = null
        activeConfig = null
        onDone()
    }
}
