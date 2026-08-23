package com.google.ai.edge.gallery.capabilities.web

class FakeWebObservationProvider : WebObservationProvider {
    var available = true
    var currentUrl = "https://example.com"
    var inputValue = ""
    var submitted = false

    override fun isAvailable(): Boolean = available

    override suspend fun observe(): WebObservation {
        if (!available) throw UnsupportedOperationException("Fake web provider unavailable")

        val root = WebElement(
            id = "root",
            tag = "HTML",
            children = listOf(
                WebElement(
                    id = "input_name",
                    tag = "INPUT",
                    isEditable = true,
                    text = inputValue,
                    accessibleName = "Name"
                ),
                WebElement(
                    id = "input_pwd",
                    tag = "INPUT",
                    isEditable = true,
                    isPassword = true,
                    text = "secret_password",
                    accessibleName = "Password"
                ),
                WebElement(
                    id = "btn_submit",
                    tag = "BUTTON",
                    isClickable = true,
                    text = "Submit"
                ),
                WebElement(
                    id = "status_text",
                    tag = "DIV",
                    text = if (submitted) "Submitted: $inputValue" else "Not submitted"
                )
            )
        )
        return WebObservation(
            url = currentUrl,
            title = "Fake Page",
            rootElement = root,
            timestamp = System.currentTimeMillis()
        )
    }
}

class FakeWebInteractionProvider(
    private val observationProvider: FakeWebObservationProvider
) : WebInteractionProvider {
    var available = true

    override fun isAvailable(): Boolean = available

    override suspend fun execute(action: WebAction): WebActionResult {
        if (!available) throw UnsupportedOperationException("Fake web provider unavailable")

        when (action.type) {
            WebActionType.NAVIGATE -> {
                val url = action.parameters["url"]
                if (url == null || url.startsWith("javascript:")) {
                    return WebActionResult(WebActionResultStatus.NAVIGATION_BLOCKED, "Invalid URL")
                }
                observationProvider.currentUrl = url
                return WebActionResult(WebActionResultStatus.SUCCESS)
            }
            WebActionType.INPUT_TEXT -> {
                val targetId = action.target?.id
                if (targetId == "input_name") {
                    observationProvider.inputValue = action.parameters["text"] ?: ""
                    return WebActionResult(WebActionResultStatus.SUCCESS)
                }
                return WebActionResult(WebActionResultStatus.TARGET_NOT_FOUND)
            }
            WebActionType.CLICK -> {
                val targetId = action.target?.id
                if (targetId == "btn_submit") {
                    observationProvider.submitted = true
                    return WebActionResult(WebActionResultStatus.SUCCESS)
                }
                return WebActionResult(WebActionResultStatus.TARGET_NOT_FOUND)
            }
            else -> return WebActionResult(WebActionResultStatus.ACTION_UNSUPPORTED)
        }
    }
}
