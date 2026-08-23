package com.google.ai.edge.gallery.capabilities.ui

import com.google.ai.edge.gallery.capabilities.Capability
import com.google.ai.edge.gallery.capabilities.ui.android.AccessibilityServiceBridge
import com.google.ai.edge.gallery.capabilities.ui.android.AndroidUiInteractionProvider
import com.google.ai.edge.gallery.capabilities.ui.android.AndroidUiObservationProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UiCapabilityModule {

    @Provides
    @Singleton
    fun provideUiObservationProvider(bridge: AccessibilityServiceBridge): UiObservationProvider {
        return AndroidUiObservationProvider(bridge)
    }

    @Provides
    @Singleton
    fun provideUiInteractionProvider(bridge: AccessibilityServiceBridge): UiInteractionProvider {
        return AndroidUiInteractionProvider(bridge)
    }

    @Provides
    @IntoSet
    fun provideUiObservationCapability(provider: UiObservationProvider): Capability {
        return AndroidUiObservationCapability(provider)
    }

    @Provides
    @IntoSet
    fun provideUiInteractionCapability(provider: UiInteractionProvider): Capability {
        return AndroidUiInteractionCapability(provider)
    }
}
