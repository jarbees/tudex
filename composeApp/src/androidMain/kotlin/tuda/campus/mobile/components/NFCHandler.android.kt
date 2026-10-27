package tuda.campus.mobile.components

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import tuda.campus.mobile.managers.MensaManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.collections.fold

@Composable
actual fun rememberNFCHandler(): NFCHandler {
    val context = LocalContext.current
    val activity = context as Activity

    return remember {
        NFCHandler(activity)
    }
}

actual class NFCHandler(private val activity: Activity) {

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    private val _isEnabled = MutableStateFlow(false)
    actual val isEnabled = _isEnabled.asStateFlow()

    actual fun startScan() {
        nfcAdapter?.enableReaderMode(
            activity,
            readerCallback,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
        _isEnabled.value = true
    }

    actual fun stopScan() {
        nfcAdapter?.disableReaderMode(activity)
        _isEnabled.value = false
    }

    private val readerCallback = NfcAdapter.ReaderCallback { tag ->
        val isoDep = IsoDep.get(tag) ?: return@ReaderCallback

        CoroutineScope(Dispatchers.IO).launch {
            try {
                isoDep.connect()

                var uidBytes = tag.id
                if (uidBytes.size == 7) uidBytes = uidBytes.copyOf(8)

                val uid = uidBytes
                    .reversed()
                    .fold(0UL) { acc, byte ->
                        (acc shl 8) or byte.toUByte().toULong()
                    }.toString()

                sendApdu(isoDep, byteArrayOf(
                    0x90.toByte(), 0x5A.toByte(), 0x00, 0x00, 0x03,
                    0x5F.toByte(), 0x84.toByte(), 0x15.toByte(), 0x00
                ))

                val balanceResp = sendApdu(isoDep, byteArrayOf(
                    0x90.toByte(), 0x6C.toByte(), 0x00, 0x00, 0x01, 0x01, 0x00
                ))

                val balance = balanceResp.dropLast(2)
                    .reversed()
                    .fold(0) { acc, b -> (acc shl 8) or b.toUByte().toInt() }
                    .toDouble() / 1000.0

                val txResp = sendApdu(isoDep, byteArrayOf(
                    0x90.toByte(), 0xF5.toByte(), 0x00, 0x00, 0x01, 0x01, 0x00
                ))

                val lastTx = ((txResp[13].toUByte().toInt() shl 8) or
                        txResp[12].toUByte().toInt()) / 1000.0

                MensaManager.updateCard(
                    cardID = uid,
                    cardBalance = "%.2f".format(balance),
                    cardLastBalance = "%.2f".format(lastTx)
                )

                stopScan()

            } catch (e: Exception) {
                println("NFC Error: ${e.message}")
            } finally {
                try { isoDep.close() } catch (_: Exception) {}
            }
        }
    }

    private fun sendApdu(isoDep: IsoDep, apdu: ByteArray): ByteArray {
        return isoDep.transceive(apdu)
    }
}