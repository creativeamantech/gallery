package com.google.ai.edge.gallery.capabilities.web

import com.google.ai.edge.gallery.capabilities.Capability
import com.google.ai.edge.gallery.capabilities.web.android.AndroidWebViewInteractionProvider
import com.google.ai.edge.gallery.capabilities.web.android.AndroidWebViewObservationProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WebCapabilityModule {

    @Provides
    @Singleton
    fun provideWebObservationProvider(): WebObservationProvider {
        return AndroidWebViewObservationProvider()
    }

    @Provides
    @Singleton
    fun provideWebInteractionProvider(): WebInteractionProvider {
        return AndroidWebViewInteractionProvider()
    }

    @Provides
    @IntoSet
    fun provideWebObservationCapability(provider: WebObservationProvider): Capability {
        return WebObservationCapability(provider)
    }

    @Provides
    @IntoSet
    fun provideWebInteractionCapability(provider: WebInteractionProvider): Capability {
        return WebInteractionCapability(provider)
    }
}
