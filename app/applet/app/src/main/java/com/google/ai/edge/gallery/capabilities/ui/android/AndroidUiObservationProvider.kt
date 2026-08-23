package com.google.ai.edge.gallery.capabilities.ui.android

import com.google.ai.edge.gallery.capabilities.ui.UiObservation
import com.google.ai.edge.gallery.capabilities.ui.UiObservationProvider

class AndroidUiObservationProvider(
    private val bridge: AccessibilityServiceBridge
) : UiObservationProvider {
    override fun isAvailable(): Boolean = bridge.isServiceEnabled()

    override suspend fun observe(): UiObservation {
        if (!isAvailable()) {
            throw UnsupportedOperationException("AccessibilityService not enabled")
        }
        
        return bridge.executeWithRoot { root, packageName ->
            val uiNode = AccessibilityNodeConverter.toUiNode(root)
            UiObservation(
                root = uiNode,
                timestamp = System.currentTimeMillis(),
                packageName = packageName
            )
        } ?: throw IllegalStateException("Could not retrieve UI root")
    }
}
