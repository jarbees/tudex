package tuda.campus.mobile.components

import androidx.compose.runtime.Composable

interface BrowserLauncher {
    fun openUrl(url: String)

    fun tucanLogin()
}

@Composable
expect fun browserLauncher(): BrowserLauncher