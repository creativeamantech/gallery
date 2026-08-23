package com.google.ai.edge.gallery.capabilities.android

import android.Manifest
import com.google.ai.edge.gallery.capabilities.CapabilityAvailability
import com.google.ai.edge.gallery.capabilities.CapabilityExecutionContext
import com.google.ai.edge.gallery.capabilities.CapabilityResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidCapabilityLayerTest {

    private class FakeAndroidCapabilityHost(
        val installedApps: List<AppInfo> = emptyList(),
        val grantedPermissions: Set<String> = emptySet()
    ) : AndroidCapabilityHost {
        override fun getInstalledApplications(): List<AppInfo> = installedApps

        override fun checkPermission(permission: String): Boolean {
            return grantedPermissions.contains(permission)
        }

        override suspend fun executeIntentAction(action: String, parameters: String): String {
            return "success_fake"
        }
    }

    private val fakeContext = object : CapabilityExecutionContext {
        override val taskId: String = "test-task"
        override val isHeadless: Boolean = true
    }

    @Test
    fun testAppDiscoveryCapability() = runBlocking {
        val host = FakeAndroidCapabilityHost(
            installedApps = listOf(
                AppInfo("com.example.app", "Example App", true)
            )
        )
        val capability = AndroidAppDiscoveryCapability(host)
        
        assertEquals(CapabilityAvailability.AVAILABLE, capability.checkAvailability(fakeContext))
        
        val result = capability.execute(emptyMap(), fakeContext)
        assertTrue(result is CapabilityResult.Success)
        val data = (result as CapabilityResult.Success).data
        val apps = data["applications"] as List<Map<String, Any>>
        assertEquals(1, apps.size)
        assertEquals("com.example.app", apps[0]["package_name"])
    }

    @Test
    fun testIntentCapability_PermissionMissing() = runBlocking {
        // Not granting READ_CALENDAR permission
        val host = FakeAndroidCapabilityHost(grantedPermissions = emptySet())
        val capability = AndroidIntentCapability(host)
        
        // Availability is unconditionally available because arguments are evaluated during execution
        assertEquals(CapabilityAvailability.AVAILABLE, capability.checkAvailability(fakeContext))
        
        val args = mapOf(
            "intent" to "read_calendar_events",
            "parameters" to "{}"
        )
        
        val result = capability.execute(args, fakeContext)
        assertTrue(result is CapabilityResult.PermissionRequired)
        val permissions = (result as CapabilityResult.PermissionRequired).permissions
        assertTrue(permissions.contains(Manifest.permission.READ_CALENDAR))
    }

    @Test
    fun testIntentCapability_PermissionGranted() = runBlocking {
        // Granting READ_CALENDAR permission
        val host = FakeAndroidCapabilityHost(grantedPermissions = setOf(Manifest.permission.READ_CALENDAR))
        val capability = AndroidIntentCapability(host)
        
        val args = mapOf(
            "intent" to "read_calendar_events",
            "parameters" to "{}"
        )
        
        val result = capability.execute(args, fakeContext)
        assertTrue(result is CapabilityResult.Success)
    }
}
