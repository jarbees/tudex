package tuda.campus.mobile.components

import androidx.compose.runtime.Composable
import platform.Foundation.NSURL
import platform.SafariServices.SFSafariViewController
import platform.UIKit.UIApplication

class IosBrowserLauncher : BrowserLauncher {

    // Taken from https://medium.com/@muralivitt/how-to-add-an-in-app-browser-to-your-compose-multiplatform-app-android-ios-7579ddc4d678#selection-889.0-973.5
    override fun openUrl(url: String) {
        val nsUrl = NSURL(string = url)

        // Create Safari View Controller
        val safariViewController = SFSafariViewController(uRL = nsUrl)

        // Find the top-most view controller
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        var topController = rootViewController

        // Navigate to the topmost controller (in case of modals, etc.)
        while (topController?.presentedViewController != null) {
            topController = topController.presentedViewController
        }

        // Present Safari View Controller
        topController?.presentViewController(
            safariViewController,
            animated = true,
            completion = null
        )
    }

    override fun tucanLogin() {
        TODO("Not yet implemented")
    }
}

@Composable
actual fun browserLauncher(): BrowserLauncher = IosBrowserLauncher()