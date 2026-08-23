package com.google.ai.edge.gallery.capabilities.web

interface WebObservationProvider {
    fun isAvailable(): Boolean
    suspend fun observe(): WebObservation
}
