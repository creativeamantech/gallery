package com.google.ai.edge.gallery.capabilities.ui.android

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.google.ai.edge.gallery.capabilities.ui.UiAction
import com.google.ai.edge.gallery.capabilities.ui.UiActionResult
import com.google.ai.edge.gallery.capabilities.ui.UiActionResultStatus
import com.google.ai.edge.gallery.capabilities.ui.UiActionType
import com.google.ai.edge.gallery.capabilities.ui.UiInteractionProvider

class AndroidUiInteractionProvider(
    private val bridge: AccessibilityServiceBridge
) : UiInteractionProvider {
    override fun isAvailable(): Boolean = bridge.isServiceEnabled()

    override suspend fun execute(action: UiAction): UiActionResult {
        if (!isAvailable()) {
            throw UnsupportedOperationException("AccessibilityService not enabled")
        }

        if (action.type == UiActionType.BACK) {
            val success = bridge.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            return if (success) {
                UiActionResult(UiActionResultStatus.SUCCESS)
            } else {
                UiActionResult(UiActionResultStatus.ACTION_UNSUPPORTED, "Back action unsupported")
            }
        }

        val target = action.target ?: return UiActionResult(UiActionResultStatus.FAILED, "Target required for non-global actions")

        val result = bridge.executeWithRoot { root, _ ->
            val node = NodeReferenceResolver.resolve(target, root)
            if (node == null) {
                return@executeWithRoot UiActionResult(UiActionResultStatus.TARGET_NOT_FOUND)
            }
            
            try {
                when (action.type) {
                    UiActionType.CLICK -> {
                        if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            UiActionResult(UiActionResultStatus.SUCCESS)
                        } else {
                            UiActionResult(UiActionResultStatus.ACTION_UNSUPPORTED, "Node not clickable")
                        }
                    }
                    UiActionType.LONG_CLICK -> {
                        if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)) {
                            UiActionResult(UiActionResultStatus.SUCCESS)
                        } else {
                            UiActionResult(UiActionResultStatus.ACTION_UNSUPPORTED, "Long click unsupported")
                        }
                    }
                    UiActionType.INPUT_TEXT -> {
                        if (!node.isEditable) {
                            UiActionResult(UiActionResultStatus.ACTION_UNSUPPORTED, "Node not editable")
                        } else {
                            val text = action.parameters["text"] ?: ""
                            val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
                            if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
                                UiActionResult(UiActionResultStatus.SUCCESS)
                            } else {
                                UiActionResult(UiActionResultStatus.ACTION_UNSUPPORTED, "Set text unsupported")
                            }
                        }
                    }
                    UiActionType.CLEAR_TEXT -> {
                        if (!node.isEditable) {
                            UiActionResult(UiActionResultStatus.ACTION_UNSUPPORTED, "Node not editable")
                        } else {
                            val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "") }
                            if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
                                UiActionResult(UiActionResultStatus.SUCCESS)
                            } else {
                                UiActionResult(UiActionResultStatus.ACTION_UNSUPPORTED, "Clear text unsupported")
                            }
                        }
                    }
                    UiActionType.SCROLL -> {
                        val dir = action.parameters["direction"]
                        val actionCode = if (dir == "backward") AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD else AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                        if (node.isScrollable && node.performAction(actionCode)) {
                            UiActionResult(UiActionResultStatus.SUCCESS)
                        } else {
                            UiActionResult(UiActionResultStatus.ACTION_UNSUPPORTED, "Scroll unsupported")
                        }
                    }
                    else -> UiActionResult(UiActionResultStatus.ACTION_UNSUPPORTED, "Action ${action.type} not mapped")
                }
            } finally {
                node.recycle()
            }
        }

        return result ?: UiActionResult(UiActionResultStatus.FAILED, "Could not access UI root")
    }
}
