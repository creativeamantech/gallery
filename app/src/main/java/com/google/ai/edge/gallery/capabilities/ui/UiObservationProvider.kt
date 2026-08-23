package com.google.ai.edge.gallery.capabilities.ui

interface UiObservationProvider {
    fun isAvailable(): Boolean
    suspend fun observe(): UiObservation
}
