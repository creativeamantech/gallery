package com.google.ai.edge.gallery.capabilities.ui

import com.google.ai.edge.gallery.capabilities.CapabilityAvailability
import com.google.ai.edge.gallery.capabilities.CapabilityExecutionContext
import com.google.ai.edge.gallery.capabilities.CapabilityResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UiCapabilityLayerTest {

    private val fakeContext = object : CapabilityExecutionContext {
        override val taskId: String = "test-task"
        override val isHeadless: Boolean = true
    }

    private class FakeUiObservationProvider(var available: Boolean = true) : UiObservationProvider {
        var currentObservation: UiObservation? = null
        
        override fun isAvailable(): Boolean = available

        override suspend fun observe(): UiObservation {
            if (!available) throw UnsupportedOperationException("Service disabled")
            return currentObservation ?: UiObservation(
                root = UiNode("root"),
                timestamp = System.currentTimeMillis()
            )
        }
    }

    private class FakeUiInteractionProvider(var available: Boolean = true) : UiInteractionProvider {
        var lastAction: UiAction? = null
        var resultToReturn: UiActionResult = UiActionResult(UiActionResultStatus.SUCCESS)

        override fun isAvailable(): Boolean = available

        override suspend fun execute(action: UiAction): UiActionResult {
            if (!available) throw UnsupportedOperationException("Service disabled")
            lastAction = action
            return resultToReturn
        }
    }

    @Test
    fun `test UiNode serialization and password redaction`() {
        val child = UiNode(id = "child_1", text = "secret", isPassword = true)
        val root = UiNode(id = "root_1", text = "public text", children = listOf(child))
        
        val map = root.toMap()
        assertEquals("root_1", map["id"])
        assertEquals("public text", map["text"])
        
        val childrenMap = map["children"] as List<Map<String, Any?>>
        assertEquals(1, childrenMap.size)
        assertEquals("child_1", childrenMap[0]["id"])
        assertEquals("***", childrenMap[0]["text"]) // Redaction verified
    }

    @Test
    fun `test NodeReference fromMap and toMap`() {
        val map = mapOf("accessibilityId" to "node_5", "resourceId" to "com.test:id/btn")
        val ref = NodeReference.fromMap(map)
        assertEquals("node_5", ref.accessibilityId)
        assertEquals("com.test:id/btn", ref.resourceId)
        
        val outMap = ref.toMap()
        assertEquals("node_5", outMap["accessibilityId"])
        assertEquals("com.test:id/btn", outMap["resourceId"])
    }

    @Test
    fun `test AndroidUiObservationCapability availability and execute`() = runBlocking {
        val provider = FakeUiObservationProvider(available = false)
        val capability = AndroidUiObservationCapability(provider)
        
        assertEquals(CapabilityAvailability.USER_ACTION_REQUIRED, capability.checkAvailability(fakeContext))
        
        // Even if execute is called, it should throw or return UserActionRequired
        val res = capability.execute(emptyMap(), fakeContext)
        assertTrue(res is CapabilityResult.UserActionRequired)
        
        // Now enable
        provider.available = true
        assertEquals(CapabilityAvailability.AVAILABLE, capability.checkAvailability(fakeContext))
        provider.currentObservation = UiObservation(root = UiNode("test_root"), timestamp = 123L)
        val successRes = capability.execute(emptyMap(), fakeContext)
        assertTrue(successRes is CapabilityResult.Success)
        val data = (successRes as CapabilityResult.Success).data
        val obsMap = data["observation"] as Map<String, Any?>
        val rootMap = obsMap["root"] as Map<String, Any?>
        assertEquals("test_root", rootMap["id"])
    }

    @Test
    fun `test AndroidUiInteractionCapability action parsing and execution`() = runBlocking {
        val provider = FakeUiInteractionProvider()
        val capability = AndroidUiInteractionCapability(provider)
        
        val targetMap = mapOf("resourceId" to "com.app:id/submit")
        val args = mapOf(
            "actionType" to "CLICK",
            "target" to targetMap
        )
        
        val result = capability.execute(args, fakeContext)
        assertTrue(result is CapabilityResult.Success)
        val statusStr = (result as CapabilityResult.Success).data["status"]
        assertEquals("SUCCESS", statusStr)
        
        val lastAction = provider.lastAction
        assertNotNull(lastAction)
        assertEquals(UiActionType.CLICK, lastAction!!.type)
        assertEquals("com.app:id/submit", lastAction.target?.resourceId)
    }

    @Test
    fun `test Observe Act Observe scenario`() = runBlocking {
        val obsProvider = FakeUiObservationProvider()
        val actProvider = FakeUiInteractionProvider()
        
        val obsCap = AndroidUiObservationCapability(obsProvider)
        val actCap = AndroidUiInteractionCapability(actProvider)
        
        // 1. Observe initial state
        obsProvider.currentObservation = UiObservation(
            root = UiNode("root", text = "Unclicked", resourceId = "btn"), 
            timestamp = 1L
        )
        var obsResult = obsCap.execute(emptyMap(), fakeContext) as CapabilityResult.Success
        var root = (obsResult.data["observation"] as Map<String, Any?>)["root"] as Map<String, Any?>
        assertEquals("Unclicked", root["text"])
        
        // 2. Act
        val actionArgs = mapOf(
            "actionType" to "CLICK",
            "target" to mapOf("resourceId" to "btn")
        )
        val actResult = actCap.execute(actionArgs, fakeContext) as CapabilityResult.Success
        assertEquals("SUCCESS", actResult.data["status"])
        
        // 3. Provider simulates state change
        obsProvider.currentObservation = UiObservation(
            root = UiNode("root", text = "Clicked", resourceId = "btn"), 
            timestamp = 2L
        )
        
        // 4. Observe again
        obsResult = obsCap.execute(emptyMap(), fakeContext) as CapabilityResult.Success
        root = (obsResult.data["observation"] as Map<String, Any?>)["root"] as Map<String, Any?>
        assertEquals("Clicked", root["text"])
    }

    @Test
    fun `test malformed actionType`() = runBlocking {
        val provider = FakeUiInteractionProvider()
        val capability = AndroidUiInteractionCapability(provider)
        
        val args = mapOf("actionType" to "INVALID_ACTION")
        val result = capability.execute(args, fakeContext)
        assertTrue(result is CapabilityResult.Error)
    }
}
