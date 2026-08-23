package com.google.ai.edge.gallery.capabilities.android

data class AppInfo(
    val packageName: String,
    val label: String,
    val isLaunchable: Boolean
)

interface AndroidCapabilityHost {
    /** Returns list of installed apps (read-only) */
    fun getInstalledApplications(): List<AppInfo>
    
    /** Checks if a system permission is currently granted */
    fun checkPermission(permission: String): Boolean
    
    /** Securely routes actions already validated by IntentHandler */
    suspend fun executeIntentAction(action: String, parameters: String): String
}
