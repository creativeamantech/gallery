package com.google.ai.edge.gallery.capabilities.ui

import android.content.Context
import com.google.ai.edge.gallery.capabilities.Capability
import com.google.ai.edge.gallery.capabilities.ui.android.AndroidUiInteractionProvider
import com.google.ai.edge.gallery.capabilities.ui.android.AndroidUiObservationProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UiCapabilityModule {

    @Provides
    @Singleton
    fun provideUiObservationProvider(@ApplicationContext context: Context): UiObservationProvider {
        return AndroidUiObservationProvider(context)
    }

    @Provides
    @Singleton
    fun provideUiInteractionProvider(@ApplicationContext context: Context): UiInteractionProvider {
        return AndroidUiInteractionProvider(context)
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
