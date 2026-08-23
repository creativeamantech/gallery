import sys

path = 'app/src/main/java/com/google/ai/edge/gallery/capabilities/Capability.kt'
with open(path, 'r') as f:
    content = f.read()

# 1. Add CapabilityAvailability
if "enum class CapabilityAvailability" not in content:
    availability_code = """
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
"""
    content = content.replace("/**\n * Represents a structured property", availability_code + "\n/**\n * Represents a structured property")

# 2. Add CapabilityResult.PermissionRequired and UserActionRequired
if "PermissionRequired" not in content:
    result_code = """  ) : CapabilityResult()

  /** Capability execution blocked due to missing permissions. */
  data class PermissionRequired(val permissions: List<String>) : CapabilityResult()

  /** Capability execution blocked because user action is required. */
  data class UserActionRequired(val actionType: String) : CapabilityResult()
}"""
    content = content.replace("  ) : CapabilityResult()\n}", result_code)

# 3. Add checkAvailability
if "fun checkAvailability" not in content:
    check_code = """  /** Whether this capability is currently allowed to run in the current environment. */
  val isAvailable: Boolean
    get() = true

  /**
   * Evaluates if the capability is available, requires permissions, or needs user action.
   * Default implementation respects the legacy isAvailable boolean for backward compatibility.
   */
  fun checkAvailability(context: CapabilityExecutionContext? = null): CapabilityAvailability {
    return if (isAvailable) CapabilityAvailability.AVAILABLE else CapabilityAvailability.UNAVAILABLE
  }"""
    content = content.replace("  /** Whether this capability is currently allowed to run in the current environment. */\n  val isAvailable: Boolean\n    get() = true", check_code)

with open(path, 'w') as f:
    f.write(content)

