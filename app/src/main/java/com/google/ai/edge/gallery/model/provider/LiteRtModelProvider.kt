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

package com.google.ai.edge.gallery.model.provider

import android.graphics.BitmapFactory
import com.google.ai.edge.gallery.agent.sessions.LlmSessionManager
import com.google.ai.edge.gallery.data.Model
import java.util.UUID
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Adapter that provides a universal ModelProvider API over the existing LiteRT-LM execution engine.
 *
 * LEGACY NATIVE ORCHESTRATION MODE:
 * LiteRT-LM currently owns the native tool-calling loop. It does not support dynamically
 * injecting Universal Capability schemas at inference time without Kotlin reflection and its
 * internal ToolProvider framework.
 *
 * Therefore, this adapter preserves `ModelRequest.capabilities` in the API, but currently
 * ignores it. Tools must still be bound via @Tool and ToolDefinition in the existing setup.
 *
 * It will emit [ModelEvent.TextChunk] while background native execution handles tools silently.
 */
class LiteRtModelProvider(
    private val sessionManager: LlmSessionManager,
    private val model: Model,
    private val sessionId: String = UUID.randomUUID().toString()
) : ModelProvider {

    override fun generate(request: ModelRequest): Flow<ModelEvent> = callbackFlow {
        // Find the last user input from the history, as LiteRT-LM's generateResponse
        // takes a simple input string (and handles its own history internally via the sessionId).
        val lastUserMessage = request.history.lastOrNull { it.role == MessageRole.USER }
        
        val inputText = lastUserMessage?.content?.filterIsInstance<ModelContent.Text>()
            ?.joinToString("\n") { it.text } ?: ""

        val images = lastUserMessage?.content?.filterIsInstance<ModelContent.Image>()
            ?.map { BitmapFactory.decodeByteArray(it.data, 0, it.data.size) } 
            ?: emptyList()
            
        val audios = lastUserMessage?.content?.filterIsInstance<ModelContent.Audio>()
            ?.map { it.data } 
            ?: emptyList()

        // We aren't passing request.capabilities here because LiteRT-LM receives its tools
        // at session creation time via `ToolProvider`. Future phases will reconstruct the 
        // session with universal capabilities if needed.
        try {
            sessionManager.generateResponse(
                sessionId = sessionId,
                model = model,
                input = inputText,
                resultListener = { partialResult, done, partialThinkingResult ->
                    if (partialResult.isNotEmpty() || !partialThinkingResult.isNullOrEmpty()) {
                        trySend(ModelEvent.TextChunk(
                            text = partialResult,
                            thinking = partialThinkingResult
                        ))
                    }
                    if (done) {
                        trySend(ModelEvent.Completion())
                        close()
                    }
                },
                cleanUpListener = {
                    // Internal cleanup complete
                },
                onError = { message ->
                    trySend(ModelEvent.Error(message))
                    close(IllegalStateException(message))
                },
                images = images,
                audioClips = audios,
                extraContext = null
            )
        } catch (e: Exception) {
            trySend(ModelEvent.Error(e.message ?: "Unknown error", e))
            close(e)
        }

        awaitClose {
            sessionManager.stopResponse(sessionId, model)
        }
    }
}
