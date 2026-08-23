package com.google.ai.edge.gallery.capabilities.web.android

import android.util.Log
import com.google.ai.edge.gallery.capabilities.web.WebElement
import com.google.ai.edge.gallery.capabilities.web.WebObservation
import com.google.ai.edge.gallery.capabilities.web.WebObservationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import kotlin.coroutines.resume

class AndroidWebViewObservationProvider : WebObservationProvider {
    private val TAG = "AndroidWebViewObs"

    override fun isAvailable(): Boolean {
        return ActiveWebViewRegistry.get() != null
    }

    override suspend fun observe(): WebObservation = withContext(Dispatchers.Main) {
        val webView = ActiveWebViewRegistry.get() ?: throw UnsupportedOperationException("No Active WebView")

        val resultJsonStr = withTimeoutOrNull(5000L) {
            suspendCancellableCoroutine<String> { continuation ->
                webView.evaluateJavascript(WebJavascriptTemplates.EXTRACTION_SCRIPT) { result ->
                    continuation.resume(result)
                }
            }
        }

        if (resultJsonStr == null || resultJsonStr == "null") {
            throw IllegalStateException("WebView evaluation timeout or null")
        }

        val unquoted = if (resultJsonStr.startsWith("\"") && resultJsonStr.endsWith("\"")) {
            resultJsonStr.substring(1, resultJsonStr.length - 1).replace("\\\"", "\"").replace("\\\\", "\\")
        } else {
            resultJsonStr
        }

        parseObservation(unquoted)
    }

    private fun parseObservation(jsonString: String): WebObservation {
        try {
            val rootObj = JSONObject(jsonString)
            if (rootObj.has("error")) {
                throw IllegalStateException("JS Error: ${rootObj.getString("error")}")
            }

            val url = rootObj.optString("url", null).takeIf { it != "null" && it.isNotEmpty() }
            val title = rootObj.optString("title", null).takeIf { it != "null" && it.isNotEmpty() }
            val timestamp = rootObj.optLong("timestamp", System.currentTimeMillis())

            val rootElementJson = rootObj.getJSONObject("rootElement")
            val rootElement = parseElement(rootElementJson)

            return WebObservation(url, title, rootElement, timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse observation", e)
            throw e
        }
    }

    private fun parseElement(jsonObj: JSONObject): WebElement {
        val id = jsonObj.optString("id", "")
        val tag = jsonObj.optString("tag", "UNKNOWN")
        var text = jsonObj.optString("text", null).takeIf { it != "null" && it.isNotEmpty() }
        val accessibleName = jsonObj.optString("accessibleName", null).takeIf { it != "null" && it.isNotEmpty() }
        val isClickable = jsonObj.optBoolean("isClickable", false)
        val isEditable = jsonObj.optBoolean("isEditable", false)
        val isPassword = jsonObj.optBoolean("isPassword", false)
        val isChecked = if (jsonObj.has("isChecked") && !jsonObj.isNull("isChecked")) jsonObj.getBoolean("isChecked") else null

        if (isPassword) {
            text = "***"
        }

        val childrenList = mutableListOf<WebElement>()
        if (jsonObj.has("children")) {
            val childrenArray = jsonObj.getJSONArray("children")
            for (i in 0 until childrenArray.length()) {
                childrenList.add(parseElement(childrenArray.getJSONObject(i)))
            }
        }

        return WebElement(id, tag, text, accessibleName, isClickable, isEditable, isPassword, isChecked, childrenList)
    }
}
