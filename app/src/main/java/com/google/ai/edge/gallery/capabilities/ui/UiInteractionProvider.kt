package com.google.ai.edge.gallery.capabilities.ui

interface UiInteractionProvider {
    fun isAvailable(): Boolean
    suspend fun execute(action: UiAction): UiActionResult
}
