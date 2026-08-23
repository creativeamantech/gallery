package com.google.ai.edge.gallery.capabilities.web.android

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ai.edge.gallery.capabilities.web.WebAction
import com.google.ai.edge.gallery.capabilities.web.WebActionResultStatus
import com.google.ai.edge.gallery.capabilities.web.WebActionType
import com.google.ai.edge.gallery.capabilities.web.WebElementReference
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidWebViewProvidersTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<UniversalAgentWebViewTestActivity>()

    @Test
    fun testWebViewObservationAndInteraction() = runBlocking {
        delay(3000) // Wait for WebView to load

        val obsProvider = AndroidWebViewObservationProvider()
        val intProvider = AndroidWebViewInteractionProvider()

        assertTrue(obsProvider.isAvailable())

        val observation = obsProvider.observe()
        
        fun findElement(id: String, current: com.google.ai.edge.gallery.capabilities.web.WebElement): com.google.ai.edge.gallery.capabilities.web.WebElement? {
            if (current.id == id || current.id.endsWith(">$id")) return current
            for (child in current.children) {
                val found = findElement(id, child)
                if (found != null) return found
            }
            return null
        }

        val nameInput = findElement("name_input", observation.rootElement)
        assertNotNull("name_input should be found", nameInput)
        
        val pwdInput = findElement("password_input", observation.rootElement)
        assertNotNull("password_input should be found", pwdInput)
        assertTrue("password should be redacted", pwdInput?.isPassword == true && pwdInput.text == "***")

        val inputRes = intProvider.execute(
            WebAction(
                type = WebActionType.INPUT_TEXT,
                target = WebElementReference(id = nameInput!!.id),
                parameters = mapOf("text" to "Alice")
            )
        )
        assertEquals(WebActionResultStatus.SUCCESS, inputRes.status)

        val submitBtn = findElement("submit_button", observation.rootElement)
        val clickRes = intProvider.execute(
            WebAction(
                type = WebActionType.CLICK,
                target = WebElementReference(id = submitBtn!!.id)
            )
        )
        assertEquals(WebActionResultStatus.SUCCESS, clickRes.status)

        delay(1500)
        
        val obs2 = obsProvider.observe()
        val statusText = findElement("status_text", obs2.rootElement)
        assertEquals("Submitted", statusText?.text)
    }
}
