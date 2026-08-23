package com.google.ai.edge.gallery.capabilities.ui.android

import android.content.Context
import com.google.ai.edge.gallery.capabilities.ui.UiAction
import com.google.ai.edge.gallery.capabilities.ui.UiActionResult
import com.google.ai.edge.gallery.capabilities.ui.UiInteractionProvider

class AndroidUiInteractionProvider(private val context: Context) : UiInteractionProvider {
    override fun isAvailable(): Boolean {
        // No AccessibilityService exists yet, so this is always unavailable.
        return false
    }

    override suspend fun execute(action: UiAction): UiActionResult {
        throw UnsupportedOperationException("AndroidUiInteractionProvider is not implemented yet")
    }
}
