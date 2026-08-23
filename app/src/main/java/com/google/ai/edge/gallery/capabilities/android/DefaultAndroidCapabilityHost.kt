package com.google.ai.edge.gallery.capabilities.android

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.ai.edge.gallery.intents.IntentHandler

class DefaultAndroidCapabilityHost(
    private val context: Context
) : AndroidCapabilityHost {

    override fun getInstalledApplications(): List<AppInfo> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        
        return resolveInfoList.mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val label = resolveInfo.loadLabel(packageManager).toString()
            if (packageName != null) {
                AppInfo(
                    packageName = packageName,
                    label = label,
                    isLaunchable = true
                )
            } else {
                null
            }
        }.distinctBy { it.packageName }
    }

    override fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun executeIntentAction(action: String, parameters: String): String {
        return IntentHandler.handleAction(context, action, parameters) { permissionToRequest ->
            // In the headless/capability layer, if we get here and the permission is missing,
            // we should have caught it earlier in checkAvailability().
            // However, to satisfy IntentHandler's lambda, we return false here unless
            // the user actually went through a UI flow (which the capability layer doesn't do natively).
            // This prevents unexpected side effects.
            Log.e("DefaultAndroidCapabilityHost", "IntentHandler requested permission $permissionToRequest at runtime. Should be handled by CapabilityResult.PermissionRequired.")
            false
        }
    }
}
