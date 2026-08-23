package com.google.ai.edge.gallery.capabilities.android

import com.google.ai.edge.gallery.capabilities.Capability
import com.google.ai.edge.gallery.capabilities.CapabilityAvailability
import com.google.ai.edge.gallery.capabilities.CapabilityExecutionContext
import com.google.ai.edge.gallery.capabilities.CapabilityResult
import com.google.ai.edge.gallery.capabilities.CapabilitySchema
import com.google.ai.edge.gallery.capabilities.PropertySchema
import com.google.ai.edge.gallery.capabilities.RiskLevel
import com.google.ai.edge.gallery.intents.IntentAction
import android.Manifest

class AndroidIntentCapability(
    private val host: AndroidCapabilityHost
) : Capability {

    override val name: String = "runIntent"
    override val description: String = "Run an Android intent. It is used to interact with the app to perform certain actions."
    
    // We treat intents as MUTATION risk by default.
    override val riskLevel: RiskLevel = RiskLevel.SYSTEM_MUTATION

    override val inputSchema: CapabilitySchema = CapabilitySchema(
        type = "object",
        properties = mapOf(
            "intent" to PropertySchema(type = "string", description = "The intent to run."),
            "parameters" to PropertySchema(type = "string", description = "A JSON string containing the parameter values required for the intent.")
        ),
        required = listOf("intent", "parameters")
    )

    override fun checkAvailability(context: CapabilityExecutionContext?): CapabilityAvailability {
        // If we want to intercept specific known intents that need permissions, we can do it here.
        // We do not evaluate the arguments dynamically here since checkAvailability is stateless.
        // We will do dynamic permission checking in execute() instead.
        return CapabilityAvailability.AVAILABLE
    }

    override suspend fun execute(
        arguments: Map<String, Any?>,
        executionContext: CapabilityExecutionContext?
    ): CapabilityResult {
        val intentString = arguments["intent"] as? String
            ?: return CapabilityResult.Error("Missing required argument: intent")
        val parametersString = arguments["parameters"] as? String
            ?: return CapabilityResult.Error("Missing required argument: parameters")

        val action = IntentAction.from(intentString)
            ?: return CapabilityResult.Error("Intent not found: '$intentString'")

        // Explicit pre-flight permission checks for known sensitive intents before execution
        if (action == IntentAction.READ_CALENDAR_EVENTS) {
            if (!host.checkPermission(Manifest.permission.READ_CALENDAR)) {
                return CapabilityResult.PermissionRequired(listOf(Manifest.permission.READ_CALENDAR))
            }
        }

        return try {
            val res = host.executeIntentAction(intentString, parametersString)
            CapabilityResult.Success(
                mapOf(
                    "action" to intentString,
                    "parameters" to parametersString,
                    "result" to res
                )
            )
        } catch (e: Exception) {
            CapabilityResult.Error("Exception executing intent '$intentString': ${e.message}")
        }
    }
}
