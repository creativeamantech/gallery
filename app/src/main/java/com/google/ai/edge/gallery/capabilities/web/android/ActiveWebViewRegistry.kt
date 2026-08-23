package com.google.ai.edge.gallery.capabilities.web.android

import android.webkit.WebView
import java.lang.ref.WeakReference

object ActiveWebViewRegistry {
    private var activeWebViewRef: WeakReference<WebView>? = null

    @Synchronized
    fun attach(webView: WebView) {
        activeWebViewRef = WeakReference(webView)
    }

    @Synchronized
    fun detach(webView: WebView) {
        val current = activeWebViewRef?.get()
        if (current === webView) {
            activeWebViewRef?.clear()
            activeWebViewRef = null
        }
    }

    @Synchronized
    fun get(): WebView? {
        return activeWebViewRef?.get()
    }
}
