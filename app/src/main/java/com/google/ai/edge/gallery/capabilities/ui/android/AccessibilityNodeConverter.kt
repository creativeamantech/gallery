package com.google.ai.edge.gallery.capabilities.ui.android

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.google.ai.edge.gallery.capabilities.ui.UiNode
import com.google.ai.edge.gallery.capabilities.ui.UiRole

object AccessibilityNodeConverter {
    fun toUiNode(node: AccessibilityNodeInfo, pathId: String = "0"): UiNode {
        val classNameStr = node.className?.toString()
        val role = mapRole(classNameStr)
        val isPassword = node.isPassword
        
        val children = mutableListOf<UiNode>()
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                children.add(toUiNode(child, "$pathId/$i"))
                child.recycle()
            }
        }
        
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val boundsStr = "[${bounds.left},${bounds.top}][${bounds.right},${bounds.bottom}]"
        
        return UiNode(
            id = pathId,
            text = if (isPassword) "***" else node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            role = role,
            resourceId = node.viewIdResourceName,
            packageName = node.packageName?.toString(),
            bounds = boundsStr,
            isVisible = node.isVisibleToUser,
            isEnabled = node.isEnabled,
            isClickable = node.isClickable,
            isScrollable = node.isScrollable,
            isChecked = node.isChecked,
            isPassword = isPassword,
            children = children
        )
    }

    private fun mapRole(className: String?): UiRole {
        if (className == null) return UiRole.UNKNOWN
        return when {
            className.contains("Button") && className.contains("Radio") -> UiRole.RADIO_BUTTON
            className.contains("Button") -> UiRole.BUTTON
            className.contains("EditText") -> UiRole.TEXT_FIELD
            className.contains("CheckBox") -> UiRole.CHECKBOX
            className.contains("Switch") -> UiRole.SWITCH
            className.contains("Image") -> UiRole.IMAGE
            className.contains("RecyclerView") || className.contains("ListView") || className.contains("ScrollView") -> UiRole.SCROLL_CONTAINER
            className.contains("WebView") -> UiRole.WEB_CONTENT
            className.contains("TextView") -> UiRole.TEXT
            className.contains("MenuItem") -> UiRole.MENU
            className.contains("Tab") -> UiRole.TAB
            else -> UiRole.UNKNOWN
        }
    }
}
