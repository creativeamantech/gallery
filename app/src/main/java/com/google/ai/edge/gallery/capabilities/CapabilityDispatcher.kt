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
 * Minimal abstraction for routing execution to the correct Capability.
 * Parallel to the existing ToolDispatcher.
 */
interface CapabilityDispatcher {
  /**
   * Resolves the capability by name and executes it with the provided arguments.
   *
   * @param capabilityName The name of the capability to invoke.
   * @param arguments The structured arguments from the LLM.
   * @param context The current execution context (taskId, headless status, etc.).
   * @return A structured [CapabilityResult] or an Error result if not found/unavailable.
   */
  suspend fun dispatch(
    capabilityName: String,
    arguments: Map<String, Any?>,
    context: CapabilityExecutionContext? = null
  ): CapabilityResult
}

/**
 * Default implementation that routes calls using a [CapabilityRegistry].
 */
open class DefaultCapabilityDispatcher(
  private val registry: CapabilityRegistry
) : CapabilityDispatcher {

  override suspend fun dispatch(
    capabilityName: String,
    arguments: Map<String, Any?>,
    context: CapabilityExecutionContext?
  ): CapabilityResult {
    val capability = registry.getCapability(capabilityName)
      ?: return CapabilityResult.Error("Capability '$capabilityName' not found in registry.")

    if (!capability.isAvailable) {
      return CapabilityResult.Error("Capability '$capabilityName' is currently unavailable.")
    }

    // New phase 4 check
    val availability = capability.checkAvailability(context)
    when (availability) {
      CapabilityAvailability.UNAVAILABLE,
      CapabilityAvailability.DEPENDENCY_MISSING,
      CapabilityAvailability.UNSUPPORTED -> return CapabilityResult.Error("Capability '$capabilityName' is currently unavailable. State: $availability")
      CapabilityAvailability.PERMISSION_REQUIRED -> return CapabilityResult.PermissionRequired(capability.requiredPermissions)
      CapabilityAvailability.USER_ACTION_REQUIRED -> return CapabilityResult.UserActionRequired("USER_ACTION_NEEDED")
      CapabilityAvailability.AVAILABLE -> { /* proceed */ }
    }

    return try {
      capability.execute(arguments, context)
    } catch (e: Exception) {
      CapabilityResult.Error("Exception executing '$capabilityName': ${e.message}")
    }
  }
}
