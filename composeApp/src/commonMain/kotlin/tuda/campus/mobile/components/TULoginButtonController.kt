package tuda.campus.mobile.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tuda.campus.mobile.managers.IdentityManager
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources._2faFailAndWaitMsg
import tudex.composeapp.generated.resources.cancel
import tudex.composeapp.generated.resources.enterLoginDetails
import tudex.composeapp.generated.resources.loginFailed
import tudex.composeapp.generated.resources.resetTotpSecret
import tudex.composeapp.generated.resources.retry
import tudex.composeapp.generated.resources.unknownErrorMsg
import tudex.composeapp.generated.resources.usernameOrPasswordRejectedMsg
import tudex.composeapp.generated.resources.webRequestFailedWithError

private enum class LoginType {
    BASIC,
    MFA
}

@Composable
fun TULoginController(
    startLoginFlow: () -> Unit,
    startLoginFlowAtMfa: () -> Unit,
    usesSSO: Boolean = true,
    content: @Composable (loginAction: () -> Unit, isLoading: Boolean) -> Unit
) {
    val tuUsername by IdentityManager.tuUsername.collectAsStateWithLifecycle()

    var showAlert by rememberSaveable { mutableStateOf(false) }
    var alertText by rememberSaveable { mutableStateOf("") }
    var modalType by rememberSaveable { mutableStateOf(LoginType.BASIC) }

    var isLoading by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val launcher = browserLauncher()

    val _2faFailAndWaitMsg = stringResource(Res.string._2faFailAndWaitMsg)
    val usernameOrPasswordRejectedMsg = stringResource(Res.string.usernameOrPasswordRejectedMsg)
    val unknownErrorMsg = stringResource(Res.string.unknownErrorMsg)
    val webRequestFailedWithError = stringResource(Res.string.webRequestFailedWithError)

    fun tuLogin() {
        scope.launch {
            isLoading = true

            val status = IdentityManager.tuRefreshLogin()
            if (status != 200) {
                if (status == -4) {
                    alertText = _2faFailAndWaitMsg
                    modalType = LoginType.BASIC
                } else if (status == -2) {
                    alertText = usernameOrPasswordRejectedMsg
                    modalType = LoginType.MFA
                } else if (status == -1) {
                    alertText = unknownErrorMsg
                    modalType = LoginType.BASIC
                } else {
                    alertText = webRequestFailedWithError + status.toString()
                    modalType = LoginType.BASIC
                }

                showAlert = true
            }

            isLoading = false
        }
    }

    fun loginAction() {
        if (usesSSO) {
            launcher.tucanLogin()
        } else if (tuUsername == "") {      // username is either valid or ""
            //isSheetPresented = true
            startLoginFlow()
        }
        else {
            tuLogin()
        }
    }


    content({ loginAction() }, isLoading)

    if (showAlert) {
        AlertDialog(
            title = { Text(stringResource(Res.string.loginFailed)) },
            text = { Text(alertText) },
            onDismissRequest = { showAlert = false },
            confirmButton = {
                Column {
                    TextButton(
                        onClick = { tuLogin() }
                    ) {
                        Text(stringResource(Res.string.retry))
                    }

                    if (modalType == LoginType.BASIC) {
                        TextButton(
                            onClick = {
                                showAlert = false
                                startLoginFlow()
                            }
                        ) {
                            Text(stringResource(Res.string.resetTotpSecret))
                        }
                    } else if (modalType == LoginType.MFA) {
                        TextButton(
                            onClick = {
                                showAlert = false
                                startLoginFlowAtMfa()
                            }
                        ) {
                            Text(stringResource(Res.string.enterLoginDetails))
                        }
                    }

                    TextButton(
                        onClick = { showAlert = false }
                    ) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            }
        )
    }
}
