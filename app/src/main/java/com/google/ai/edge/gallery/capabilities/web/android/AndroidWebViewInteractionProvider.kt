package com.google.ai.edge.gallery.capabilities.web.android

import android.net.Uri
import com.google.ai.edge.gallery.capabilities.web.WebAction
import com.google.ai.edge.gallery.capabilities.web.WebActionResult
import com.google.ai.edge.gallery.capabilities.web.WebActionResultStatus
import com.google.ai.edge.gallery.capabilities.web.WebInteractionProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import kotlin.coroutines.resume

class AndroidWebViewInteractionProvider : WebInteractionProvider {

    override fun isAvailable(): Boolean {
        return ActiveWebViewRegistry.get() != null
    }

    override suspend fun execute(action: WebAction): WebActionResult = withContext(Dispatchers.Main) {
        val webView = ActiveWebViewRegistry.get()
        if (webView == null) {
            return@withContext WebActionResult(WebActionResultStatus.UNAVAILABLE, "No active WebView")
        }

        if (action.type.name == "NAVIGATE") {
            val url = action.parameters["url"]
            if (url == null || url.startsWith("javascript:", ignoreCase = true) || url.startsWith("file:", ignoreCase = true) || url.startsWith("content:", ignoreCase = true)) {
                return@withContext WebActionResult(WebActionResultStatus.NAVIGATION_BLOCKED, "Invalid or blocked URL scheme")
            }
            try {
                val uri = Uri.parse(url)
                val scheme = uri.scheme?.lowercase()
                if (scheme != "http" && scheme != "https" && scheme != "data") {
                     return@withContext WebActionResult(WebActionResultStatus.NAVIGATION_BLOCKED, "Invalid URL scheme: $scheme")
                }
            } catch (e: Exception) {
                return@withContext WebActionResult(WebActionResultStatus.NAVIGATION_BLOCKED, "Malformed URL")
            }
        }

        val script = WebJavascriptTemplates.actionScript(action.type.name, action.target?.id, action.parameters)

        val resultJsonStr = withTimeoutOrNull(5000L) {
            suspendCancellableCoroutine<String> { continuation ->
                webView.evaluateJavascript(script) { result ->
                    continuation.resume(result)
                }
            }
        }

        if (resultJsonStr == null || resultJsonStr == "null") {
            return@withContext WebActionResult(WebActionResultStatus.FAILED, "Evaluation timeout or null")
        }

        val unquoted = if (resultJsonStr.startsWith("\"") && resultJsonStr.endsWith("\"")) {
            resultJsonStr.substring(1, resultJsonStr.length - 1).replace("\\\"", "\"").replace("\\\\", "\\")
        } else {
            resultJsonStr
        }

        try {
            val jsonObj = JSONObject(unquoted)
            val statusStr = jsonObj.optString("status", "FAILED")
            val message = jsonObj.optString("message", null).takeIf { it != "null" && it.isNotEmpty() }

            val status = try {
                WebActionResultStatus.valueOf(statusStr)
            } catch (e: Exception) {
                WebActionResultStatus.FAILED
            }
            WebActionResult(status, message)
        } catch (e: Exception) {
            WebActionResult(WebActionResultStatus.FAILED, "Parse error: ${e.message}")
        }
    }
}
