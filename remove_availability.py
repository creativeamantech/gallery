path = 'app/src/main/java/com/google/ai/edge/gallery/capabilities/Capability.kt'
with open(path, 'r') as f:
    content = f.read()

availability_code = """/**
 * Defines the availability state of a capability.
 */
enum class CapabilityAvailability {
  AVAILABLE,
  UNAVAILABLE,
  PERMISSION_REQUIRED,
  DEPENDENCY_MISSING,
  UNSUPPORTED,
  USER_ACTION_REQUIRED
}"""
content = content.replace(availability_code + "\n", "")

with open(path, 'w') as f:
    f.write(content)
