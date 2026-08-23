package com.google.ai.edge.gallery.capabilities.web

import com.google.ai.edge.gallery.capabilities.Capability
import com.google.ai.edge.gallery.capabilities.CapabilityAvailability
import com.google.ai.edge.gallery.capabilities.CapabilityExecutionContext
import com.google.ai.edge.gallery.capabilities.CapabilityResult
import com.google.ai.edge.gallery.capabilities.CapabilitySchema
import com.google.ai.edge.gallery.capabilities.RiskLevel

class WebObservationCapability(
    private val provider: WebObservationProvider
) : Capability {
    override val name: String = "observe_web"
    override val description: String = "Observes the current web page and returns a structured semantic tree representing the DOM layout, text, and interactive elements."
    override val riskLevel: RiskLevel = RiskLevel.DATA_READ
    
    override val inputSchema: CapabilitySchema = CapabilitySchema(
        type = "object",
        properties = emptyMap()
    )

    override fun checkAvailability(context: CapabilityExecutionContext?): CapabilityAvailability {
        return if (provider.isAvailable()) {
            CapabilityAvailability.AVAILABLE
        } else {
            CapabilityAvailability.UNAVAILABLE
        }
    }

    override suspend fun execute(
        arguments: Map<String, Any?>,
        executionContext: CapabilityExecutionContext?
    ): CapabilityResult {
        return try {
            val observation = provider.observe()
            CapabilityResult.Success(mapOf("observation" to observation.toMap()))
        } catch (e: UnsupportedOperationException) {
            CapabilityResult.UserActionRequired("Web Provider unavailable")
        } catch (e: Exception) {
            CapabilityResult.Error("Failed to observe web page: ${e.message}")
        }
    }
}
