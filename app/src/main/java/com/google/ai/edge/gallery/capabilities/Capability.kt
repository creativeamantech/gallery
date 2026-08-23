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

/**
 * Categorizes the potential impact and security risk of a capability.
 */
enum class RiskLevel {
  /** Harmless actions like reading public info or performing local calculations. */
  SAFE,
  /** Actions that read private or scoped user data. */
  DATA_READ,
  /** Actions that modify or write data, requiring careful handling. */
  DATA_WRITE,
  /** Actions that change system settings or application state. */
  SYSTEM_MUTATION,
  /** Irreversible or highly sensitive actions (e.g., payments, deleting accounts). */
  SENSITIVE
}



/**
 * Represents a structured property in the CapabilitySchema.
 * Loosely aligns with standard JSON Schema concepts for universal model compatibility.
 */
data class PropertySchema(
  val type: String, // e.g., "string", "number", "boolean", "object", "array"
  val description: String? = null,
  val properties: Map<String, PropertySchema>? = null,
  val items: PropertySchema? = null
)

/**
 * Represents the expected input arguments for a Capability.
 * Based on JSON Schema layout.
 */
data class CapabilitySchema(
  val type: String = "object",
  val properties: Map<String, PropertySchema>,
  val required: List<String> = emptyList()
)

/**
 * Structured result of a capability execution.
 */
sealed class CapabilityResult {
  /** Capability executed successfully and returned data. */
  data class Success(val data: Map<String, Any?>) : CapabilityResult()

  /** Capability failed to execute properly. */
  data class Error(
    val message: String,
    val details: Map<String, Any?>? = null
  ) : CapabilityResult()

  /** Capability execution blocked due to missing permissions. */
  data class PermissionRequired(val permissions: List<String>) : CapabilityResult()

  /** Capability execution blocked because user action is required. */
  data class UserActionRequired(val actionType: String) : CapabilityResult()
}

/**
 * A lightweight execution context for providing UI channels or headless state
 * down to capabilities. (Placeholder for future AndroidSystemFacade integration).
 */
interface CapabilityExecutionContext {
  val taskId: String
  val isHeadless: Boolean
}

/**
 * Universal abstraction for an agent tool, skill, or system integration.
 * It is completely decoupled from any specific LLM runtime (e.g., LiteRT-LM).
 */
interface Capability {
  /** The unique identity/name of the capability, exposed to the LLM. */
  val name: String

  /** A human-readable description telling the LLM what this capability does. */
  val description: String

  /** The structured JSON-like schema defining required and optional arguments. */
  val inputSchema: CapabilitySchema

  /** Risk classification to determine if UI confirmation might be needed. */
  val riskLevel: RiskLevel
    get() = RiskLevel.SAFE

  /** Android or domain-specific permissions required to execute this capability. */
  val requiredPermissions: List<String>
    get() = emptyList()

  /** Whether this capability is currently allowed to run in the current environment. */
  val isAvailable: Boolean
    get() = true

  /**
   * Evaluates if the capability is available, requires permissions, or needs user action.
   * Default implementation respects the legacy isAvailable boolean for backward compatibility.
   */
  fun checkAvailability(context: CapabilityExecutionContext? = null): CapabilityAvailability {
    return if (isAvailable) CapabilityAvailability.AVAILABLE else CapabilityAvailability.UNAVAILABLE
  }

  /**
   * Executes the capability asynchronously.
   *
   * @param arguments The parsed JSON arguments provided by the LLM.
   * @param executionContext Context containing task info and headless status.
   * @return A structured [CapabilityResult] indicating success or error.
   */
  suspend fun execute(
    arguments: Map<String, Any?>,
    executionContext: CapabilityExecutionContext? = null
  ): CapabilityResult
}
