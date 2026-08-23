package com.google.ai.edge.gallery.capabilities.web.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.ai.edge.gallery.ui.common.GalleryWebView

class UniversalAgentWebViewTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val html = """
                        <!DOCTYPE html>
                        <html>
                        <body>
                            <h1>Test Page</h1>
                            <input type="text" id="name_input" value="" />
                            <input type="password" id="password_input" value="secret" />
                            <button id="submit_button" onclick="document.getElementById('status_text').innerText = 'Submitted';">Submit</button>
                            <div id="status_text">Not submitted</div>
                        </body>
                        </html>
                    """.trimIndent()
                    
                    GalleryWebView(
                        modifier = Modifier.fillMaxSize(),
                        initialUrl = "data:text/html;charset=utf-8," + android.net.Uri.encode(html),
                        allowRequestPermission = true
                    )
                }
            }
        }
    }
}
