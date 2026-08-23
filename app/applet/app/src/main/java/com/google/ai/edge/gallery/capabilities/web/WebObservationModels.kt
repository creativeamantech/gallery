package com.google.ai.edge.gallery.capabilities.web

data class WebObservation(
    val url: String?,
    val title: String?,
    val rootElement: WebElement,
    val timestamp: Long
) {
    fun toMap(): Map<String, Any?> = mutableMapOf<String, Any?>(
        "timestamp" to timestamp,
        "rootElement" to rootElement.toMap()
    ).apply {
        if (url != null) put("url", url)
        if (title != null) put("title", title)
    }
}

data class WebElement(
    val id: String, // structural path or stable ID
    val tag: String,
    val text: String? = null,
    val accessibleName: String? = null,
    val isClickable: Boolean = false,
    val isEditable: Boolean = false,
    val isPassword: Boolean = false,
    val isChecked: Boolean? = null,
    val children: List<WebElement> = emptyList()
) {
    fun toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "id" to id,
            "tag" to tag,
            "isClickable" to isClickable,
            "isEditable" to isEditable,
            "isPassword" to isPassword
        )
        if (isChecked != null) map["isChecked"] = isChecked
        
        val displayText = if (isPassword) "***" else text
        if (displayText != null) map["text"] = displayText
        
        if (accessibleName != null) map["accessibleName"] = accessibleName
        if (children.isNotEmpty()) {
            map["children"] = children.map { it.toMap() }
        }
        return map
    }
}
