package com.google.ai.edge.gallery.capabilities.android

import com.google.ai.edge.gallery.capabilities.Capability
import com.google.ai.edge.gallery.capabilities.CapabilityAvailability
import com.google.ai.edge.gallery.capabilities.CapabilityExecutionContext
import com.google.ai.edge.gallery.capabilities.CapabilityResult
import com.google.ai.edge.gallery.capabilities.CapabilitySchema
import com.google.ai.edge.gallery.capabilities.RiskLevel

class AndroidAppDiscoveryCapability(
    private val host: AndroidCapabilityHost
) : Capability {

    override val name: String = "get_installed_applications"
    override val description: String = "Returns a list of applications installed on the Android device."
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    
    override val inputSchema: CapabilitySchema = CapabilitySchema(
        type = "object",
        properties = emptyMap()
    )

    override fun checkAvailability(context: CapabilityExecutionContext?): CapabilityAvailability {
        return CapabilityAvailability.AVAILABLE
    }

    override suspend fun execute(
        arguments: Map<String, Any?>,
        executionContext: CapabilityExecutionContext?
    ): CapabilityResult {
        return try {
            val apps = host.getInstalledApplications().map {
                mapOf(
                    "package_name" to it.packageName,
                    "label" to it.label,
                    "is_launchable" to it.isLaunchable
                )
            }
            CapabilityResult.Success(mapOf("applications" to apps))
        } catch (e: Exception) {
            CapabilityResult.Error("Failed to list installed applications: ${e.message}")
        }
    }
}
