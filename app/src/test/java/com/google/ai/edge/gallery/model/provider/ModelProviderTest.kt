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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeModelProvider : ModelProvider {
    var generateCount = 0
    var lastRequest: ModelRequest? = null

    override fun generate(request: ModelRequest): Flow<ModelEvent> = flow {
        generateCount++
        lastRequest = request
        
        emit(ModelEvent.TextChunk("Hello"))
        emit(ModelEvent.TextChunk(" World"))
        
        // Fake a tool call event for testing
        val toolRequest = ToolCallRequest(
            id = "call_123",
            name = "testTool",
            arguments = StructuredData.ObjectValue(mapOf("key" to StructuredData.StringValue("value")))
        )
        emit(ModelEvent.ToolCallRequestEvent(toolRequest))
        
        emit(ModelEvent.Completion("stop"))
    }
}

class ModelProviderTest {

    @Test
    fun `ModelRequest constructs correctly with history and config`() {
        val request = ModelRequest(
            systemInstruction = "You are a helpful assistant.",
            history = listOf(
                ModelMessage(
                    role = MessageRole.USER,
                    content = listOf(ModelContent.Text("Hi there"))
                )
            ),
            config = ModelGenerationConfig(temperature = 0.7f)
        )

        assertEquals("You are a helpful assistant.", request.systemInstruction)
        assertEquals(1, request.history.size)
        assertEquals(MessageRole.USER, request.history[0].role)
        assertEquals(0.7f, request.config.temperature)
    }

    @Test
    fun `ModelContent sealed classes work correctly`() {
        val text = ModelContent.Text("Some text")
        val image = ModelContent.Image(byteArrayOf(1, 2, 3), "image/png")
        
        assertEquals("Some text", text.text)
        assertEquals(3, image.data.size)
        assertEquals("image/png", image.mimeType)
    }

    @Test
    fun `StructuredData constructs correctly`() {
        val data = StructuredData.ObjectValue(
            mapOf(
                "number" to StructuredData.NumberValue(42.0),
                "bool" to StructuredData.BooleanValue(true),
                "array" to StructuredData.ArrayValue(listOf(StructuredData.Null))
            )
        )

        assertEquals(3, data.fields.size)
        assertTrue(data.fields["number"] is StructuredData.NumberValue)
        assertEquals(42.0, (data.fields["number"] as StructuredData.NumberValue).value, 0.0)
    }

    @Test
    fun `FakeModelProvider streams events correctly`() = runBlocking {
        val provider = FakeModelProvider()
        val request = ModelRequest(history = emptyList())
        
        val events = provider.generate(request).toList()
        
        assertEquals(1, provider.generateCount)
        assertEquals(4, events.size) // Text, Text, ToolCall, Completion
        
        assertTrue(events[0] is ModelEvent.TextChunk)
        assertEquals("Hello", (events[0] as ModelEvent.TextChunk).text)
        
        assertTrue(events[1] is ModelEvent.TextChunk)
        assertEquals(" World", (events[1] as ModelEvent.TextChunk).text)
        
        assertTrue(events[2] is ModelEvent.ToolCallRequestEvent)
        val toolCall = (events[2] as ModelEvent.ToolCallRequestEvent).request
        assertEquals("call_123", toolCall.id)
        assertEquals("testTool", toolCall.name)
        
        assertTrue(events[3] is ModelEvent.Completion)
    }
}
