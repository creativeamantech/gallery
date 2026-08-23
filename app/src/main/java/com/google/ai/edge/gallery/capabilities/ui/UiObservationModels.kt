package com.google.ai.edge.gallery.capabilities.ui

data class NodeReference(
    val accessibilityId: String? = null,
    val resourceId: String? = null,
    val hierarchyPath: String? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "accessibilityId" to accessibilityId,
        "resourceId" to resourceId,
        "hierarchyPath" to hierarchyPath
    ).filterValues { it != null }

    companion object {
        fun fromMap(map: Map<String, Any?>): NodeReference {
            return NodeReference(
                accessibilityId = map["accessibilityId"] as? String,
                resourceId = map["resourceId"] as? String,
                hierarchyPath = map["hierarchyPath"] as? String
            )
        }
    }
}

enum class UiRole {
    BUTTON, TEXT, TEXT_FIELD, CHECKBOX, RADIO_BUTTON, SWITCH,
    IMAGE, LIST, LIST_ITEM, MENU, TAB, LINK, SCROLL_CONTAINER,
    WEB_CONTENT, UNKNOWN
}

data class UiNode(
    val id: String,
    val text: String? = null,
    val contentDescription: String? = null,
    val role: UiRole = UiRole.UNKNOWN,
    val resourceId: String? = null,
    val packageName: String? = null,
    val bounds: String? = null,
    val isVisible: Boolean = true,
    val isEnabled: Boolean = true,
    val isClickable: Boolean = false,
    val isScrollable: Boolean = false,
    val isChecked: Boolean = false,
    val isPassword: Boolean = false,
    val children: List<UiNode> = emptyList()
) {
    fun toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "id" to id,
            "role" to role.name,
            "isVisible" to isVisible,
            "isEnabled" to isEnabled,
            "isClickable" to isClickable,
            "isScrollable" to isScrollable,
            "isChecked" to isChecked,
            "isPassword" to isPassword
        )
        val displayText = if (isPassword) "***" else text
        if (displayText != null) map["text"] = displayText
        if (contentDescription != null) map["contentDescription"] = contentDescription
        if (resourceId != null) map["resourceId"] = resourceId
        if (packageName != null) map["packageName"] = packageName
        if (bounds != null) map["bounds"] = bounds
        if (children.isNotEmpty()) {
            map["children"] = children.map { it.toMap() }
        }
        return map
    }
}

data class UiObservation(
    val root: UiNode,
    val timestamp: Long,
    val packageName: String? = null
) {
    fun toMap(): Map<String, Any?> = mutableMapOf<String, Any?>(
        "timestamp" to timestamp,
        "root" to root.toMap()
    ).apply {
        if (packageName != null) put("packageName", packageName)
    }
}
