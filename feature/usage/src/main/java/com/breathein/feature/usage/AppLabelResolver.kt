package com.breathein.feature.usage

import android.content.Context
import android.content.pm.PackageManager
import java.util.concurrent.ConcurrentHashMap

/** Human-readable app names, cached. Falls back to the package name for apps we can't see. */
internal class AppLabelResolver(context: Context) {

    private val packageManager = context.packageManager
    private val cache = ConcurrentHashMap<String, String>()

    fun labelFor(packageName: String): String = cache.getOrPut(packageName) {
        try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}
