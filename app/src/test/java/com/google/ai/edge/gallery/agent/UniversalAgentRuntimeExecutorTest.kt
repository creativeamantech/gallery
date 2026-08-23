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
import android.content.ContextWrapper
import com.google.ai.edge.gallery.capabilities.Capability
import com.google.ai.edge.gallery.capabilities.CapabilityDispatcher
import com.google.ai.edge.gallery.capabilities.CapabilityExecutionContext
import com.google.ai.edge.gallery.capabilities.CapabilityRegistry
import com.google.ai.edge.gallery.capabilities.CapabilityResult
import com.google.ai.edge.gallery.capabilities.CapabilitySchema
import com.google.ai.edge.gallery.capabilities.PropertySchema
import com.google.ai.edge.gallery.capabilities.RiskLevel
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.model.provider.ModelEvent
import com.google.ai.edge.gallery.model.provider.ModelProvider
import com.google.ai.edge.gallery.model.provider.ModelRequest
import com.google.ai.edge.gallery.model.provider.StructuredData
import com.google.ai.edge.gallery.model.provider.ToolCallRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeModelProvider(
    private val emitSequence: List<ModelEvent>
) : ModelProvider {
    var generateCallCount = 0
    var lastRequest: ModelRequest? = null

    override fun generate(request: ModelRequest): Flow<ModelEvent> = flow {
        lastRequest = request
        val event = emitSequence.getOrNull(generateCallCount)
        generateCallCount++
        if (event != null) {
            emit(event)
        } else {
            emit(ModelEvent.Completion())
        }
    }
}

class FakeCapabilityRegistry : CapabilityRegistry {
    private val caps = mutableMapOf<String, Capability>()
    override fun register(capability: Capability) { caps[capability.name] = capability }
    override fun unregister(capability: Capability) { caps.remove(capability.name) }
    override fun getCapability(name: String): Capability? = caps[name]
    override fun getAllCapabilities(): List<Capability> = caps.values.toList()
}

class FakeCapabilityDispatcher(private val registry: CapabilityRegistry) : CapabilityDispatcher {
    override suspend fun dispatch(
        capabilityName: String,
        arguments: Map<String, Any?>,
        context: CapabilityExecutionContext?
    ): CapabilityResult {
        val cap = registry.getCapability(capabilityName) ?: return CapabilityResult.Error("Not found")
        return cap.execute(arguments, context)
    }
}

class UniversalAgentRuntimeExecutorTest {
    
    private val dummyModel = Model(name = "test", downloadFileName = "test.tflite", url = "", sizeInBytes = 0)
    private val dummyConfig = AgentRuntimeConfig(
        model = dummyModel,
        taskId = "test_task",
        sessionId = "test_session",
        supportImage = false,
        supportAudio = false
    )
    private val dummyContext = AgentExecutionContext()
    private val dummyRequest = AgentRequest(query = "Do a test")

    private val androidContextDummy = ContextWrapper(null)

    @Test
    fun `executor emits text and completes safely`() = runBlocking {
        val provider = FakeModelProvider(listOf(
            ModelEvent.TextChunk("Hello World"),
            ModelEvent.Completion()
        ))
        
        val executor = UniversalAgentRuntimeExecutor(
            modelProviderFactory = { provider },
            capabilityRegistry = FakeCapabilityRegistry(),
            capabilityDispatcher = FakeCapabilityDispatcher(FakeCapabilityRegistry())
        )
        executor.initialize(androidContextDummy, dummyConfig) {}

        val events = executor.executeStream(dummyContext, dummyRequest).toList()
        
        assertTrue(events[0] is AgentEvent.LoopInitiated)
        assertTrue(events[1] is AgentEvent.StreamToken)
        assertEquals("Hello World", (events[1] as AgentEvent.StreamToken).token)
        assertTrue(events[2] is AgentEvent.StreamToken) // the completion token
        assertTrue(events[3] is AgentEvent.LoopTerminated)
        assertEquals("Hello World", (events[3] as AgentEvent.LoopTerminated).finalResponse)
        
        assertEquals(2, provider.generateCallCount) // Initial call + trailing call returning completion
    }

    @Test
    fun `executor intercepts tool call, dispatches capability, and appends result`() = runBlocking {
        val registry = FakeCapabilityRegistry()
        
        var capabilityExecuted = false
        registry.register(object : Capability {
            override val name = "test_capability"
            override val description = "A test"
            override val inputSchema = CapabilitySchema(type = "object", properties = emptyMap(), required = emptyList())
            override val requiredPermissions = emptyList<String>()
            override val riskLevel = RiskLevel.SAFE
            override val isAvailable = true
            override suspend fun execute(
                arguments: Map<String, Any?>,
                context: CapabilityExecutionContext?
            ): CapabilityResult {
                capabilityExecuted = true
                return CapabilityResult.Success(mapOf("status" to "success"))
            }
        })

        // 1st generate call returns a tool call.
        // 2nd generate call returns completion.
        val provider = FakeModelProvider(listOf(
            ModelEvent.ToolCallRequestEvent(
                ToolCallRequest("id_1", "test_capability", StructuredData.ObjectValue(emptyMap()))
            ),
            ModelEvent.Completion()
        ))
        
        val executor = UniversalAgentRuntimeExecutor(
            modelProviderFactory = { provider },
            capabilityRegistry = registry,
            capabilityDispatcher = FakeCapabilityDispatcher(registry)
        )
        executor.initialize(androidContextDummy, dummyConfig) {}

        val events = executor.executeStream(dummyContext, dummyRequest).toList()

        assertTrue(capabilityExecuted)
        
        val requestedEvent = events.find { it is AgentEvent.CapabilityRequested } as? AgentEvent.CapabilityRequested
        assertEquals("test_capability", requestedEvent?.name)
        
        val completedEvent = events.find { it is AgentEvent.CapabilityCompleted } as? AgentEvent.CapabilityCompleted
        assertEquals("test_capability", completedEvent?.name)
        assertEquals("success", completedEvent?.resultData?.get("status"))
        
        assertEquals(2, provider.generateCallCount)
        assertEquals(3, provider.lastRequest?.history?.size) // USER, MODEL (tool call), TOOL (tool result)
    }

    @Test
    fun `legacy LiteRT provider emits no tool calls and does not trigger dispatcher`() = runBlocking {
        // Simulates Mode A / Mode B interaction. LiteRT provider returns text, handles tools internally.
        val registry = FakeCapabilityRegistry()
        val provider = FakeModelProvider(listOf(
            ModelEvent.TextChunk("Handled internally"),
            ModelEvent.Completion()
        ))
        
        val executor = UniversalAgentRuntimeExecutor(
            modelProviderFactory = { provider },
            capabilityRegistry = registry,
            capabilityDispatcher = FakeCapabilityDispatcher(registry)
        )
        executor.initialize(androidContextDummy, dummyConfig) {}

        val events = executor.executeStream(dummyContext, dummyRequest).toList()
        
        val toolEvents = events.filter { it is AgentEvent.CapabilityRequested }
        assertTrue("No tool events should be emitted since Provider didn't request them", toolEvents.isEmpty())
        assertEquals(2, provider.generateCallCount)
    }
}
