package com.google.ai.edge.gallery.capabilities.ui.android

import android.content.Context
import com.google.ai.edge.gallery.capabilities.ui.UiObservation
import com.google.ai.edge.gallery.capabilities.ui.UiObservationProvider

class AndroidUiObservationProvider(private val context: Context) : UiObservationProvider {
    override fun isAvailable(): Boolean {
        // No AccessibilityService exists yet, so this is always unavailable.
        return false
    }

    override suspend fun observe(): UiObservation {
        throw UnsupportedOperationException("AndroidUiObservationProvider is not implemented yet")
    }
}
