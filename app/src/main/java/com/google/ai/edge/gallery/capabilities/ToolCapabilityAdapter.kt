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
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.valueParameters

/**
 * An adapter that wraps an existing [ToolDefinition] (coupled to LiteRT-LM annotations)
 * and exposes it as a universal [Capability].
 *
 * This allows the new abstraction to reuse existing tools without breaking
 * their backward compatibility or modifying their native bindings.
 */
class ToolCapabilityAdapter(
  private val toolDefinition: ToolDefinition
) : Capability {

  private val targetMethod: KFunction<*>?
  private val toolAnnotation: Tool?

  override val name: String
  override val description: String
  override val inputSchema: CapabilitySchema

  init {
    // Scan the ToolDefinition class for a method annotated with LiteRT-LM's @Tool
    var foundMethod: KFunction<*>? = null
    var foundAnnotation: Tool? = null

    for (method in toolDefinition::class.memberFunctions) {
      val annotation = method.findAnnotation<Tool>()
      if (annotation != null) {
        foundMethod = method
        foundAnnotation = annotation
        break
      }
    }

    targetMethod = foundMethod
    toolAnnotation = foundAnnotation

    if (foundMethod != null && foundAnnotation != null) {
      name = foundMethod.name
      description = foundAnnotation.description

      val properties = mutableMapOf<String, PropertySchema>()
      val required = mutableListOf<String>()

      // Inspect parameters to build the CapabilitySchema
      foundMethod.valueParameters.forEach { param ->
        val paramAnnotation = param.findAnnotation<ToolParam>()
        val paramName = param.name ?: "unknown_param"
        val paramDesc = paramAnnotation?.description ?: ""
        
        // Very basic type mapping for this phase
        val typeStr = when (param.type.classifier) {
          String::class -> "string"
          Int::class -> "number"
          Boolean::class -> "boolean"
          else -> "string" // default fallback
        }

        properties[paramName] = PropertySchema(type = typeStr, description = paramDesc)
        if (!param.isOptional) {
          required.add(paramName)
        }
      }

      inputSchema = CapabilitySchema(
        type = "object",
        properties = properties,
        required = required
      )
    } else {
      // Fallback if no annotated method is found (should not happen in valid tools)
      name = toolDefinition.javaClass.simpleName
      description = "Unknown tool"
      inputSchema = CapabilitySchema(properties = emptyMap())
    }
  }

  // Preserve the explicit bypass from the existing ToolDefinition
  override val requiredPermissions: List<String>
    get() = if (toolDefinition.alwaysAllow) emptyList() else listOf("SYSTEM_DEFAULT")

  override val riskLevel: RiskLevel
    get() = if (toolDefinition.alwaysAllow) RiskLevel.SAFE else RiskLevel.SYSTEM_MUTATION

  override suspend fun execute(
    arguments: Map<String, Any?>,
    executionContext: CapabilityExecutionContext?
  ): CapabilityResult = withContext(Dispatchers.IO) {
    if (targetMethod == null) {
      return@withContext CapabilityResult.Error("No @Tool annotated method found on ${toolDefinition.javaClass.name}")
    }

    try {
      // Map JSON arguments to the method's expected parameter order
      val argsToPass = targetMethod.valueParameters.associateWith { param ->
        val passedValue = arguments[param.name]
        if (param.type.classifier == String::class && passedValue != null && passedValue !is String) {
           passedValue.toString()
        } else {
           passedValue
        }
      }

      // We use callBy so Kotlin automatically binds `this` parameter and named parameters
      val callArgs = argsToPass.toMutableMap()
      // `callBy` requires the instance to be passed if it's an instance method, but targetMethod is bound if called on instance?
      // No, memberFunctions are not bound. We must provide the instance parameter.
      callArgs[targetMethod.parameters[0]] = toolDefinition

      val rawResult = targetMethod.callBy(callArgs)

      // The existing tools typically return a Map<String, String> or Map<String, Any>
      val structuredData = when (rawResult) {
        is Map<*, *> -> {
          val resultData = mutableMapOf<String, Any?>()
          for ((k, v) in rawResult) {
            if (k is String) resultData[k] = v
          }
          resultData
        }
        else -> mapOf("result" to rawResult?.toString())
      }

      // Check if it's a known error shape from MCP/RunJs/etc tools.
      if (structuredData["status"] == "failed" && structuredData.containsKey("error")) {
        return@withContext CapabilityResult.Error(
          structuredData["error"].toString(),
          structuredData
        )
      }

      CapabilityResult.Success(structuredData)
    } catch (e: Exception) {
      val rootCause = generateSequence(e.cause) { it.cause }.lastOrNull() ?: e
      CapabilityResult.Error("Failed to execute $name: ${rootCause.message}")
    }
  }
}
