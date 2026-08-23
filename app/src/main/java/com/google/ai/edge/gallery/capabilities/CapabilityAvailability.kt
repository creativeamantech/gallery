package com.google.ai.edge.gallery.capabilities

/**
 * Defines the availability state of a capability.
 */
enum class CapabilityAvailability {
  AVAILABLE,
  UNAVAILABLE,
  PERMISSION_REQUIRED,
  DEPENDENCY_MISSING,
  UNSUPPORTED,
  USER_ACTION_REQUIRED
}
