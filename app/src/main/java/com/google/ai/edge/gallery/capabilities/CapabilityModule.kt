package com.google.ai.edge.gallery.capabilities

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CapabilityModule {

    @Provides
    @Singleton
    fun provideCapabilityRegistry(capabilities: Set<@JvmSuppressWildcards Capability>): CapabilityRegistry {
        val registry = DefaultCapabilityRegistry()
        capabilities.forEach { registry.register(it) }
        return registry
    }

    @Provides
    @Singleton
    fun provideCapabilityDispatcher(registry: CapabilityRegistry): CapabilityDispatcher {
        return DefaultCapabilityDispatcher(registry)
    }
}
