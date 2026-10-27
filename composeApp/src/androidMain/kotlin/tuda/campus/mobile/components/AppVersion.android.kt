package tuda.campus.mobile.components

import android.content.pm.PackageInfo
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun getAppVersion(): String? {
    val context = LocalContext.current

    return context.packageManager
        .getPackageInfo(context.packageName, 0)
        .versionName ?: ""
}

@Composable
actual fun getBuildNumber(): String? {
    val context = LocalContext.current
    val packageInfo: PackageInfo =
        context.packageManager.getPackageInfo(context.packageName, 0)

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode.toString()
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toString()
    }
}