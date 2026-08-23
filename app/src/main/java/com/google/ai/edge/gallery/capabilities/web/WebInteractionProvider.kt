package com.google.ai.edge.gallery.capabilities.web

interface WebInteractionProvider {
    fun isAvailable(): Boolean
    suspend fun execute(action: WebAction): WebActionResult
}
