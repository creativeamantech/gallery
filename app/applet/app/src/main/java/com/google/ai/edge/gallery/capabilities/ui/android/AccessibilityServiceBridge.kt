package com.google.ai.edge.gallery.capabilities.ui.android

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessibilityServiceBridge @Inject constructor() {
    private val lock = ReentrantLock()
    private var service: AccessibilityService? = null

    fun attach(accessibilityService: AccessibilityService) {
        lock.withLock {
            this.service = accessibilityService
        }
    }

    fun detach() {
        lock.withLock {
            this.service = null
        }
    }

    fun isServiceEnabled(): Boolean {
        lock.withLock {
            return service != null
        }
    }

    fun <T> executeWithRoot(block: (AccessibilityNodeInfo, String) -> T): T? {
        lock.withLock {
            val currentService = service ?: return null
            val root = currentService.rootInActiveWindow ?: return null
            val packageName = root.packageName?.toString() ?: ""
            return try {
                block(root, packageName)
            } finally {
                root.recycle()
            }
        }
    }

    fun performGlobalAction(action: Int): Boolean {
        lock.withLock {
            return service?.performGlobalAction(action) ?: false
        }
    }
}
