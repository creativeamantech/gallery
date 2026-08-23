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

package com.google.ai.edge.gallery.capabilities

import com.google.ai.edge.gallery.tools.ToolDefinition
import com.google.ai.edge.gallery.tools.ToolExecutionContext
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// A dummy ToolDefinition imitating the existing LiteRT-LM tools.
class DummyMathTool : ToolDefinition {
  override val alwaysAllow: Boolean = true
  override var executionContext: ToolExecutionContext? = null

  @Tool(description = "Adds two numbers together.")
  fun addNumbers(
    @ToolParam(description = "The first number.") arg1: String,
    @ToolParam(description = "The second number.") arg2: String
  ): Map<String, String> {
    val a = arg1.toIntOrNull() ?: 0
    val b = arg2.toIntOrNull() ?: 0
    return mapOf("result" to (a + b).toString(), "status" to "succeeded")
  }
}

class DummyFailingTool : ToolDefinition {
  override val alwaysAllow: Boolean = false
  override var executionContext: ToolExecutionContext? = null

  @Tool(description = "A tool that fails gracefully.")
  fun failTask(
    @ToolParam(description = "Reason for failure.") reason: String
  ): Map<String, String> {
    return mapOf("error" to "Intentional failure: $reason", "status" to "failed")
  }
}

class CapabilitySystemTest {

  @Test
  fun `adapter correctly extracts metadata and schema from ToolDefinition`() {
    val tool = DummyMathTool()
    val adapter = ToolCapabilityAdapter(tool)

    assertEquals("addNumbers", adapter.name)
    assertEquals("Adds two numbers together.", adapter.description)
    assertEquals(RiskLevel.SAFE, adapter.riskLevel)
    assertTrue(adapter.requiredPermissions.isEmpty())

    val schema = adapter.inputSchema
    assertEquals("object", schema.type)
    assertNotNull(schema.properties["arg1"])
    assertEquals("The first number.", schema.properties["arg1"]?.description)
    assertEquals("string", schema.properties["arg1"]?.type)

    assertNotNull(schema.properties["arg2"])
    assertEquals("The second number.", schema.properties["arg2"]?.description)
    
    assertTrue(schema.required.contains("arg1"))
    assertTrue(schema.required.contains("arg2"))
  }

  @Test
  fun `adapter handles alwaysAllow false correctly`() {
    val tool = DummyFailingTool()
    val adapter = ToolCapabilityAdapter(tool)

    assertEquals("failTask", adapter.name)
    assertEquals(RiskLevel.SYSTEM_MUTATION, adapter.riskLevel)
    assertTrue(adapter.requiredPermissions.isNotEmpty())
  }

  @Test
  fun `adapter executes successfully and maps unstructured return to CapabilityResult Success`() = runBlocking {
    val tool = DummyMathTool()
    val adapter = ToolCapabilityAdapter(tool)

    val result = adapter.execute(mapOf("arg1" to "10", "arg2" to "5"))
    assertTrue(result is CapabilityResult.Success)
    
    val successData = (result as CapabilityResult.Success).data
    assertEquals("15", successData["result"])
    assertEquals("succeeded", successData["status"])
  }

  @Test
  fun `adapter detects status failed and maps to CapabilityResult Error`() = runBlocking {
    val tool = DummyFailingTool()
    val adapter = ToolCapabilityAdapter(tool)

    val result = adapter.execute(mapOf("reason" to "Testing error parsing"))
    assertTrue(result is CapabilityResult.Error)
    
    val errorData = result as CapabilityResult.Error
    assertEquals("Intentional failure: Testing error parsing", errorData.message)
    assertNotNull(errorData.details)
    assertEquals("failed", errorData.details?.get("status"))
  }

  @Test
  fun `registry correctly stores and retrieves capabilities`() {
    val registry = DefaultCapabilityRegistry()
    val cap1 = ToolCapabilityAdapter(DummyMathTool())
    val cap2 = ToolCapabilityAdapter(DummyFailingTool())

    registry.register(cap1)
    registry.register(cap2)

    assertEquals(2, registry.getAllCapabilities().size)
    assertEquals(cap1, registry.getCapability("addNumbers"))
    assertEquals(cap2, registry.getCapability("failTask"))

    registry.unregister(cap1)
    assertEquals(1, registry.getAllCapabilities().size)
    assertEquals(null, registry.getCapability("addNumbers"))
  }

  @Test
  fun `dispatcher routes correctly and handles missing capabilities`() = runBlocking {
    val registry = DefaultCapabilityRegistry()
    registry.register(ToolCapabilityAdapter(DummyMathTool()))
    
    val dispatcher = DefaultCapabilityDispatcher(registry)

    // Successful dispatch
    val result1 = dispatcher.dispatch("addNumbers", mapOf("arg1" to "2", "arg2" to "3"))
    assertTrue(result1 is CapabilityResult.Success)
    assertEquals("5", (result1 as CapabilityResult.Success).data["result"])

    // Missing capability
    val result2 = dispatcher.dispatch("missingTool", mapOf())
    assertTrue(result2 is CapabilityResult.Error)
    assertTrue((result2 as CapabilityResult.Error).message.contains("not found in registry"))
  }
}
