package tuda.campus.mobile.components

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow


@Composable
actual fun rememberNFCHandler(): NFCHandler {
    TODO("iOS NFC not implemented")
}

actual class NFCHandler {
    actual fun startScan() {
        TODO("iOS NFC not implemented")
    }

    actual fun stopScan() {
        TODO("iOS NFC not implemented")
    }

    actual val isEnabled: StateFlow<Boolean>
        get() = TODO("iOS NFC not implemented")
}