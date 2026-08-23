package com.google.ai.edge.gallery.capabilities.android

import android.content.Context
import com.google.ai.edge.gallery.capabilities.Capability
import com.google.ai.edge.gallery.capabilities.CapabilityRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AndroidCapabilityModule {

    @Provides
    @Singleton
    fun provideAndroidCapabilityHost(@ApplicationContext context: Context): AndroidCapabilityHost {
        return DefaultAndroidCapabilityHost(context)
    }

    @Provides
    @IntoSet
    fun provideAppDiscoveryCapability(host: AndroidCapabilityHost): Capability {
        return AndroidAppDiscoveryCapability(host)
    }

    @Provides
    @IntoSet
    fun provideIntentCapability(host: AndroidCapabilityHost): Capability {
        return AndroidIntentCapability(host)
    }
}
