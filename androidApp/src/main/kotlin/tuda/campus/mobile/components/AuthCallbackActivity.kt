package tuda.campus.mobile.components

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import tuda.campus.mobile.MainActivity
import tuda.campus.mobile.managers.IdentityManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AuthCallbackActivity : ComponentActivity() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent?.data ?: return

        val code = uri.getQueryParameter("code")
        val returnedState = uri.getQueryParameter("state")

        val codeVerifier = AuthSessionStore.codeVerifier ?: run { finish(); return }
        val expectedState = AuthSessionStore.state ?: run { finish(); return }
        val nonce = AuthSessionStore.nonce ?: run { finish(); return }

        if (code == null || returnedState == null) {
            finish()
            return
        }

        if (returnedState != expectedState) { // Possible CSRF attack
            finish()
            return
        }

        GlobalScope.launch {
            IdentityManager.tucanLogin(code, codeVerifier, nonce)

            runOnUiThread {
                val intent = Intent(this@AuthCallbackActivity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
                finish()
            }
        }

        finish()
    }
}