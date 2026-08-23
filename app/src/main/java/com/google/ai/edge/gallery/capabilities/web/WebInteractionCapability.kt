package com.google.ai.edge.gallery.capabilities.web

import com.google.ai.edge.gallery.capabilities.Capability
import com.google.ai.edge.gallery.capabilities.CapabilityAvailability
import com.google.ai.edge.gallery.capabilities.CapabilityExecutionContext
import com.google.ai.edge.gallery.capabilities.CapabilityResult
import com.google.ai.edge.gallery.capabilities.CapabilitySchema
import com.google.ai.edge.gallery.capabilities.PropertySchema
import com.google.ai.edge.gallery.capabilities.RiskLevel

class WebInteractionCapability(
    private val provider: WebInteractionProvider
) : Capability {
    override val name: String = "interact_web"
    override val description: String = "Interacts with the current web page. Requires a previously observed WebElementReference target and an action type (e.g. CLICK, INPUT_TEXT, NAVIGATE)."
    override val riskLevel: RiskLevel = RiskLevel.SYSTEM_MUTATION

    override val inputSchema: CapabilitySchema = CapabilitySchema(
        type = "object",
        properties = mapOf(
            "actionType" to PropertySchema(type = "string", description = "The type of action to perform: CLICK, INPUT_TEXT, CLEAR_TEXT, SELECT, SCROLL, NAVIGATE, GO_BACK"),
            "target" to PropertySchema(type = "object", description = "The WebElementReference identifying the target element. Not required for global actions like NAVIGATE or GO_BACK."),
            "parameters" to PropertySchema(type = "object", description = "Additional parameters. E.g. 'text' for INPUT_TEXT, 'url' for NAVIGATE action.")
        ),
        required = listOf("actionType")
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
        val actionTypeStr = arguments["actionType"] as? String
            ?: return CapabilityResult.Error("Missing required argument: actionType")
        
        val actionType = try {
            WebActionType.valueOf(actionTypeStr)
        } catch (e: IllegalArgumentException) {
            return CapabilityResult.Error("Invalid actionType: $actionTypeStr")
        }

        val targetMap = arguments["target"] as? Map<String, Any?>
        val target = targetMap?.let { WebElementReference.fromMap(it) }

        val parameters = (arguments["parameters"] as? Map<String, Any?>)?.mapValues { it.value.toString() } ?: emptyMap()

        val action = WebAction(type = actionType, target = target, parameters = parameters)

        return try {
            val result = provider.execute(action)
            CapabilityResult.Success(result.toMap())
        } catch (e: UnsupportedOperationException) {
            CapabilityResult.UserActionRequired("Web Provider unavailable")
        } catch (e: Exception) {
            CapabilityResult.Error("Failed to interact with web: ${e.message}")
        }
    }
}
