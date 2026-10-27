package tuda.campus.mobile.screens.settings

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import tuda.campus.mobile.managers.IdentityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TULoginViewModel(val dismissSheet: () -> Unit) : ViewModel() {

    private val _showAlert = MutableStateFlow(false)
    val showAlert = _showAlert.asStateFlow()

    private val _alertText = MutableStateFlow("An unknown error occurred.")
    val alertText = _alertText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    val usernameTextFieldState = TextFieldState()
    val passwordTextFieldState = TextFieldState()
    val totpTextFieldState = TextFieldState()

    fun updateShowAlert(newShowAlert: Boolean) { _showAlert.value = newShowAlert }

    suspend fun basicLogin(nextPage: () -> Unit): Int {
        _isLoading.value = true

        /*
        val status: Int = IdentityManager.tuBasicLogin(
            usernameTextFieldState.text.toString(),
            passwordTextFieldState.text.toString()
        )

        if (status != 200) {
            if (status == -2) {
                _alertText.value = "Username or Password is incorrect, please try again"
            } else if (status == -1) {
                _alertText.value = "An unknown error has occurred.\nThis could be due to your internet connection"
            } else {
                _alertText.value = "The web request failed with Error code $status"
            }

            _showAlert.value = true
        } else {
            if (IdentityManager.tuTotpSecretExists.value) {
                val mfsStatus = IdentityManager.tuLogin()

                if (mfsStatus == 200) {
                    dismissSheet()
                } else {
                    nextPage()
                }
            } else {
                nextPage()
            }
        }
         */

        nextPage()

        _isLoading.value = false
        //return status
        return 200
    }

    suspend fun mfaLogin() {
        _isLoading.value = true
        //val status: Int = IdentityManager.tuTotpLogin(totpTextFieldState.text.toString())
        val status: Int = IdentityManager.tuLogin(
            username = usernameTextFieldState.text.toString(),
            password = passwordTextFieldState.text.toString(),
            totpSecret = totpTextFieldState.text.toString()
        )

        if (status == 200 || status == 302) {
            dismissSheet()
        } else {
            if (status == -4) {
                _alertText.value = "Your secret couldn't be validated\nTry entering it again"
            } else if (status == -3) {
                _alertText.value = "No token containing \"TUPlan\" exists. Please view the setup guide"
            } else if (status == -2) {
                _alertText.value = "The SSO rejected your username or password.\nSince TUCaN accepted it, either try again, completely log out of the app or restart..."
            } else if (status == -1) {
                _alertText.value = "An unknown error has occurred.\nThis could be due to your internet connection"
            } else {
                _alertText.value = "The web request failed with Error code $status"
            }

            _showAlert.value = true
        }

        _isLoading.value = false
    }
}