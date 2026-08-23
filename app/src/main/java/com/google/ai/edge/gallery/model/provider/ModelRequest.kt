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

import com.google.ai.edge.gallery.capabilities.Capability

/**
 * A platform-neutral representation of the context required for a model to generate a response.
 */
data class ModelRequest(
    val systemInstruction: String? = null,
    val history: List<ModelMessage> = emptyList(),
    val capabilities: List<Capability> = emptyList(),
    val config: ModelGenerationConfig = ModelGenerationConfig()
)

data class ModelGenerationConfig(
    val temperature: Float? = null,
    val maxTokens: Int? = null,
    val topK: Int? = null,
    val topP: Float? = null
)
