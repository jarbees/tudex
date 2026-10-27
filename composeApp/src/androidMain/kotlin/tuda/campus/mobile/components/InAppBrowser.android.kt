package tuda.campus.mobile.components

import android.content.Context
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import java.security.MessageDigest
import java.security.SecureRandom

object AuthSessionStore {
    var codeVerifier: String? = null
    var state: String? = null
    var nonce: String? = null
}

class AndroidBrowserLauncher(private val context: Context) : BrowserLauncher {
    // Taken from https://medium.com/@muralivitt/how-to-add-an-in-app-browser-to-your-compose-multiplatform-app-android-ios-7579ddc4d678#selection-889.0-973.5
    override fun openUrl(url: String) {
        val parsedUri = runCatching { url.toUri() }.getOrNull() ?: return
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(context, parsedUri)
    }

    override fun tucanLogin() {
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)

        val state = randomUrlSafeString(32)
        val nonce = randomUrlSafeString(32)

        AuthSessionStore.codeVerifier = codeVerifier
        AuthSessionStore.state = state
        AuthSessionStore.nonce = nonce

        val uri = "https://dsf.tucan.tu-darmstadt.de/IdentityServer/connect/authorize".toUri()
            .buildUpon()
            .appendQueryParameter("nonce", nonce)
            .appendQueryParameter("max_age", "0")
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("scope", "openid DSF profile offline_access")
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("redirect_uri", "de.datenlotsen.campusnet.tuda://oauth2redirect")
            .appendQueryParameter("client_id", "MobileApp")
            .appendQueryParameter("state", state)
            .build()

        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(context, uri)
    }
}

@Composable
actual fun browserLauncher(): BrowserLauncher {
    val context = LocalContext.current
    return remember(context) { AndroidBrowserLauncher(context) }
}



private fun generateCodeVerifier(): String {
    val bytes = ByteArray(32)
    SecureRandom().nextBytes(bytes)
    return base64Url(bytes)
}

private fun generateCodeChallenge(verifier: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(verifier.toByteArray(Charsets.UTF_8))
    return base64Url(digest)
}

private fun randomUrlSafeString(length: Int): String {
    val bytes = ByteArray(length)
    SecureRandom().nextBytes(bytes)
    return base64Url(bytes)
}

private fun base64Url(bytes: ByteArray): String {
    return Base64.encodeToString(
        bytes,
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
    )
}