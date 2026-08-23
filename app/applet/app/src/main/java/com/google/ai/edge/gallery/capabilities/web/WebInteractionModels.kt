package com.google.ai.edge.gallery.capabilities.web

enum class WebActionType {
    CLICK, INPUT_TEXT, CLEAR_TEXT, SELECT, SCROLL, NAVIGATE, GO_BACK
}

data class WebElementReference(
    val id: String? = null,
    val domId: String? = null,
    val accessibleName: String? = null,
    val structuralPath: String? = null,
    val providerReference: String? = null
) {
    fun toMap(): Map<String, Any?> = mutableMapOf<String, Any?>().apply {
        id?.let { put("id", it) }
        domId?.let { put("domId", it) }
        accessibleName?.let { put("accessibleName", it) }
        structuralPath?.let { put("structuralPath", it) }
        providerReference?.let { put("providerReference", it) }
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): WebElementReference {
            return WebElementReference(
                id = map["id"] as? String,
                domId = map["domId"] as? String,
                accessibleName = map["accessibleName"] as? String,
                structuralPath = map["structuralPath"] as? String,
                providerReference = map["providerReference"] as? String
            )
        }
    }
}

data class WebAction(
    val type: WebActionType,
    val target: WebElementReference? = null,
    val parameters: Map<String, String> = emptyMap()
)

enum class WebActionResultStatus {
    SUCCESS,
    TARGET_NOT_FOUND,
    ACTION_UNSUPPORTED,
    INVALID_ARGUMENT,
    NAVIGATION_BLOCKED,
    FAILED,
    USER_ACTION_REQUIRED,
    UNAVAILABLE
}

data class WebActionResult(
    val status: WebActionResultStatus,
    val message: String? = null
) {
    fun toMap(): Map<String, Any?> = mutableMapOf<String, Any?>(
        "status" to status.name
    ).apply {
        if (message != null) put("message", message)
    }
}
