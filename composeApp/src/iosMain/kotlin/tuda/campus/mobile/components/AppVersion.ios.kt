package tuda.campus.mobile.components

import androidx.compose.runtime.Composable
import platform.Foundation.NSBundle


@Composable
actual fun getAppVersion(): String? {
    return NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String
}

@Composable
actual fun getBuildNumber(): String? {
    return NSBundle.mainBundle.infoDictionary?.get("CFBundleVersion") as? String
}