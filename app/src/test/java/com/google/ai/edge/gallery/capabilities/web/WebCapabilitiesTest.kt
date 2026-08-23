package com.google.ai.edge.gallery.capabilities.web

import com.google.ai.edge.gallery.capabilities.CapabilityAvailability
import com.google.ai.edge.gallery.capabilities.CapabilityResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebCapabilitiesTest {

    @Test
    fun testObservationAndRedaction() = runBlocking {
        val fakeObsProvider = FakeWebObservationProvider()
        val obsCapability = WebObservationCapability(fakeObsProvider)

        assertEquals(CapabilityAvailability.AVAILABLE, obsCapability.checkAvailability(null))

        val result = obsCapability.execute(emptyMap(), null) as CapabilityResult.Success
        val obsMap = result.data["observation"] as Map<*, *>
        val root = obsMap["rootElement"] as Map<*, *>
        val children = root["children"] as List<Map<*, *>>

        // Check password redaction
        val pwdElement = children.find { it["id"] == "input_pwd" }
        assertEquals("***", pwdElement?.get("text"))
        assertEquals(true, pwdElement?.get("isPassword"))
        
        // Check normal text
        val submitBtn = children.find { it["id"] == "btn_submit" }
        assertEquals("Submit", submitBtn?.get("text"))
    }

    @Test
    fun testInteractionAndNavigation() = runBlocking {
        val fakeObsProvider = FakeWebObservationProvider()
        val fakeIntProvider = FakeWebInteractionProvider(fakeObsProvider)
        val intCapability = WebInteractionCapability(fakeIntProvider)

        // Test navigation
        var result = intCapability.execute(
            mapOf(
                "actionType" to "NAVIGATE",
                "parameters" to mapOf("url" to "https://google.com")
            ), null
        ) as CapabilityResult.Success
        assertEquals("SUCCESS", result.data["status"])
        assertEquals("https://google.com", fakeObsProvider.currentUrl)

        // Test navigation blocked (javascript:)
        result = intCapability.execute(
            mapOf(
                "actionType" to "NAVIGATE",
                "parameters" to mapOf("url" to "javascript:alert(1)")
            ), null
        ) as CapabilityResult.Success
        assertEquals("NAVIGATION_BLOCKED", result.data["status"])

        // Test input text
        val targetRef = WebElementReference(id = "input_name").toMap()
        result = intCapability.execute(
            mapOf(
                "actionType" to "INPUT_TEXT",
                "target" to targetRef,
                "parameters" to mapOf("text" to "Alice")
            ), null
        ) as CapabilityResult.Success
        assertEquals("SUCCESS", result.data["status"])
        assertEquals("Alice", fakeObsProvider.inputValue)

        // Test click
        val clickRef = WebElementReference(id = "btn_submit").toMap()
        result = intCapability.execute(
            mapOf(
                "actionType" to "CLICK",
                "target" to clickRef
            ), null
        ) as CapabilityResult.Success
        assertEquals("SUCCESS", result.data["status"])
        assertTrue(fakeObsProvider.submitted)
        
        // Test observe after act
        val obs = fakeObsProvider.observe()
        val statusText = obs.rootElement.children.find { it.id == "status_text" }?.text
        assertEquals("Submitted: Alice", statusText)
    }

    @Test
    fun testUnavailableProvider() = runBlocking {
        val fakeObsProvider = FakeWebObservationProvider()
        fakeObsProvider.available = false
        val obsCapability = WebObservationCapability(fakeObsProvider)

        assertEquals(CapabilityAvailability.UNAVAILABLE, obsCapability.checkAvailability(null))
        val result = obsCapability.execute(emptyMap(), null)
        assertTrue(result is CapabilityResult.UserActionRequired)
    }
}
