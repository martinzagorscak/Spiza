package com.mzagorscak.spiza.data.appUpdate

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat

class AndroidAppVersionProvider(
    private val context: Context,
) : AppVersionProvider {
    // versionCode is already major * 10_000 + minor * 100 + patch (androidApp/build.gradle.kts).
    override fun currentVersion(): Long = PackageInfoCompat.getLongVersionCode(packageInfo())

    private fun packageInfo(): PackageInfo {
        val packageManager = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(context.packageName, 0)
        }
    }
}
