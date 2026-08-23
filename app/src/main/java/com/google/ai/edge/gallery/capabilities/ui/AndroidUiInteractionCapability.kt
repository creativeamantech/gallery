package com.google.ai.edge.gallery.capabilities.ui

import com.google.ai.edge.gallery.capabilities.Capability
import com.google.ai.edge.gallery.capabilities.CapabilityAvailability
import com.google.ai.edge.gallery.capabilities.CapabilityExecutionContext
import com.google.ai.edge.gallery.capabilities.CapabilityResult
import com.google.ai.edge.gallery.capabilities.CapabilitySchema
import com.google.ai.edge.gallery.capabilities.PropertySchema
import com.google.ai.edge.gallery.capabilities.RiskLevel

class AndroidUiInteractionCapability(
    private val provider: UiInteractionProvider
) : Capability {
    override val name: String = "interact_ui"
    override val description: String = "Interacts with the current Android UI. Requires a previously observed NodeReference target and an action type (e.g. CLICK, INPUT_TEXT)."
    override val riskLevel: RiskLevel = RiskLevel.SYSTEM_MUTATION

    override val inputSchema: CapabilitySchema = CapabilitySchema(
        type = "object",
        properties = mapOf(
            "actionType" to PropertySchema(type = "string", description = "The type of action to perform: CLICK, LONG_CLICK, INPUT_TEXT, CLEAR_TEXT, SCROLL, BACK, SUBMIT, SELECT, TOGGLE"),
            "target" to PropertySchema(type = "object", description = "The NodeReference identifying the target UI element. Not required for global actions like BACK."),
            "parameters" to PropertySchema(type = "object", description = "Additional parameters. E.g. 'text' for INPUT_TEXT action.")
        ),
        required = listOf("actionType")
    )

    override fun checkAvailability(context: CapabilityExecutionContext?): CapabilityAvailability {
        return if (provider.isAvailable()) {
            CapabilityAvailability.AVAILABLE
        } else {
            CapabilityAvailability.USER_ACTION_REQUIRED
        }
    }

    override suspend fun execute(
        arguments: Map<String, Any?>,
        executionContext: CapabilityExecutionContext?
    ): CapabilityResult {
        val actionTypeStr = arguments["actionType"] as? String
            ?: return CapabilityResult.Error("Missing required argument: actionType")
        
        val actionType = try {
            UiActionType.valueOf(actionTypeStr)
        } catch (e: IllegalArgumentException) {
            return CapabilityResult.Error("Invalid actionType: $actionTypeStr")
        }

        val targetMap = arguments["target"] as? Map<String, Any?>
        val target = targetMap?.let { NodeReference.fromMap(it) }

        val parameters = (arguments["parameters"] as? Map<String, Any?>)?.mapValues { it.value.toString() } ?: emptyMap()

        val action = UiAction(type = actionType, target = target, parameters = parameters)

        return try {
            val result = provider.execute(action)
            CapabilityResult.Success(result.toMap())
        } catch (e: UnsupportedOperationException) {
            CapabilityResult.UserActionRequired("Enable Accessibility Service")
        } catch (e: Exception) {
            CapabilityResult.Error("Failed to interact with UI: ${e.message}")
        }
    }
}
