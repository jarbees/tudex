package tuda.campus.mobile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.liftric.kvault.KVault
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIApplication
import platform.darwin.NSObjectProtocol
import kotlin.coroutines.resume

// the full constructor is
// KVault(serviceName: String? = null, accessGroup: String? = null, accessibility: KVault.Accessible = Accessible.WhenUnlocked)
// TODO: Possibly use these? Seems to work fine without...
actual val vault: KVault = KVault()


actual fun isProtectedDataAvailable(): Boolean {
    return UIApplication.sharedApplication.isProtectedDataAvailable()
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun awaitProtectedDataAvailable() {
    if (isProtectedDataAvailable()) {
        return
    }

    suspendCancellableCoroutine { continuation ->
        var observerToken: NSObjectProtocol? = null

        observerToken = NSNotificationCenter.defaultCenter.addObserverForName(
            name = "UIApplicationProtectedDataDidBecomeAvailable",
            `object` = null,
            queue = null,
            usingBlock = { _ ->
                if (observerToken != null) {
                    NSNotificationCenter.defaultCenter.removeObserver(observerToken!!)
                }
                continuation.resume(Unit)
            }
        )

        continuation.invokeOnCancellation {
            NSNotificationCenter.defaultCenter.removeObserver(observerToken)
        }
    }
}



actual val dataStore: DataStore<Preferences> = createPreferencesDataStore()


@OptIn(ExperimentalForeignApi::class)
private fun getPreferencesDataStorePath(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )

    return requireNotNull(documentDirectory).path + "/$dataStoreFileName"
}

private fun createPreferencesDataStore(): DataStore<Preferences> {
    val path = getPreferencesDataStorePath()
    return getPreferencesDataStore(path)
}