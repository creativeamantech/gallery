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

sealed interface ModelEvent {
    /** A chunk of generated text (or thinking/reasoning text). */
    data class TextChunk(
        val text: String,
        val thinking: String? = null
    ) : ModelEvent

    /** The model pauses text generation and explicitly requests a capability execution. */
    data class ToolCallRequestEvent(
        val request: ToolCallRequest
    ) : ModelEvent

    /** The model has finished generating. */
    data class Completion(
        val reason: String? = null
    ) : ModelEvent

    /** A fatal error occurred during generation. */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : ModelEvent
}
