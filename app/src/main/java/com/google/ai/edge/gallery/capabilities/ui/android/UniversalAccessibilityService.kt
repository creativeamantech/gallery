package com.google.ai.edge.gallery.capabilities.ui.android

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UniversalAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var bridge: AccessibilityServiceBridge

    override fun onServiceConnected() {
        super.onServiceConnected()
        bridge.attach(this)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        bridge.detach()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        bridge.detach()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Keep Universal Agent pull-driven. Do not stream events to LLM.
    }

    override fun onInterrupt() {
        // No-op
    }
}
