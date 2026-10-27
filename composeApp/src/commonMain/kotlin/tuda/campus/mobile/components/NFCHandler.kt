package tuda.campus.mobile.components

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

@Composable
expect fun rememberNFCHandler(): NFCHandler

expect class NFCHandler {
    fun startScan()
    fun stopScan()
    val isEnabled: StateFlow<Boolean>
}