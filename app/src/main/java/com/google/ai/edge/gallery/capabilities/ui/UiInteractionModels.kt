package com.google.ai.edge.gallery.capabilities.ui

enum class UiActionType {
    CLICK, LONG_CLICK, INPUT_TEXT, CLEAR_TEXT, SCROLL, BACK, SUBMIT, SELECT, TOGGLE
}

data class UiAction(
    val type: UiActionType,
    val target: NodeReference? = null,
    val parameters: Map<String, String> = emptyMap()
)

enum class UiActionResultStatus {
    SUCCESS, TARGET_NOT_FOUND, ACTION_UNSUPPORTED, FAILED
}

data class UiActionResult(
    val status: UiActionResultStatus,
    val message: String? = null
) {
    fun toMap(): Map<String, Any?> = mutableMapOf<String, Any?>(
        "status" to status.name
    ).apply {
        if (message != null) put("message", message)
    }
}
