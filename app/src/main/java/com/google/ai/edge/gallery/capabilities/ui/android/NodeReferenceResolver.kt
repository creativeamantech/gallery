package com.google.ai.edge.gallery.capabilities.ui.android

import android.view.accessibility.AccessibilityNodeInfo
import com.google.ai.edge.gallery.capabilities.ui.NodeReference

object NodeReferenceResolver {
    fun resolve(target: NodeReference, root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (target.resourceId != null) {
            val matches = root.findAccessibilityNodeInfosByViewId(target.resourceId)
            if (matches.isNotEmpty()) {
                val result = matches[0]
                for (i in 1 until matches.size) {
                    matches[i].recycle()
                }
                return result
            }
        }
        
        if (target.hierarchyPath != null) {
            return resolveByPath(target.hierarchyPath, root)
        }
        
        return null
    }
    
    private fun resolveByPath(path: String, root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val parts = path.split("/")
        if (parts.isEmpty() || parts[0] != "0") return null
        
        var current = AccessibilityNodeInfo.obtain(root)
        for (i in 1 until parts.size) {
            val index = parts[i].toIntOrNull() ?: run {
                current.recycle()
                return null
            }
            if (index < 0 || index >= current.childCount) {
                current.recycle()
                return null
            }
            val next = current.getChild(index)
            current.recycle()
            if (next == null) return null
            current = next
        }
        return current
    }
}
