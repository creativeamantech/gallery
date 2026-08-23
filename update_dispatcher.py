path = 'app/src/main/java/com/google/ai/edge/gallery/capabilities/CapabilityDispatcher.kt'
with open(path, 'r') as f:
    content = f.read()

replacement = """    if (!capability.isAvailable) {
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

    return try {"""
    
if "val availability = capability.checkAvailability" not in content:
    content = content.replace("""    if (!capability.isAvailable) {
      return CapabilityResult.Error("Capability '$capabilityName' is currently unavailable.")
    }

    return try {""", replacement)

with open(path, 'w') as f:
    f.write(content)
