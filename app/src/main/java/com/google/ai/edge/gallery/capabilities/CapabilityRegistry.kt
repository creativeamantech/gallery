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

import java.util.concurrent.ConcurrentHashMap

/**
 * Registry for discovering and managing universal agent capabilities.
 */
interface CapabilityRegistry {
  /** Registers a capability for agent usage. */
  fun register(capability: Capability)

  /** Removes a capability from the registry. */
  fun unregister(capability: Capability)

  /** Retrieves a capability by its unique name, or null if not found. */
  fun getCapability(name: String): Capability?

  /** Returns all currently registered capabilities. */
  fun getAllCapabilities(): List<Capability>
}

/**
 * Default thread-safe implementation of [CapabilityRegistry].
 */
open class DefaultCapabilityRegistry : CapabilityRegistry {
  private val capabilities = ConcurrentHashMap<String, Capability>()

  override fun register(capability: Capability) {
    capabilities[capability.name] = capability
  }

  override fun unregister(capability: Capability) {
    capabilities.remove(capability.name)
  }

  override fun getCapability(name: String): Capability? {
    return capabilities[name]
  }

  override fun getAllCapabilities(): List<Capability> {
    return capabilities.values.toList()
  }
}
