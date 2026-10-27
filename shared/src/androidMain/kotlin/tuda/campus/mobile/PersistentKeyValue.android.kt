package tuda.campus.mobile

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.liftric.kvault.KVault
import kotlin.IllegalStateException


object VaultManager {
    private var _vault: KVault? = null

    // Initialize the Vault with this method before using it
    fun initialize(context: Context) {
        if (_vault == null) {
            _vault = KVault(context)
        }
    }

    val vault: KVault
        get() = _vault ?: throw IllegalStateException("Vault is not initialized. Call initialize() first.")
}

actual val vault = VaultManager.vault

// On Android these are not required
actual fun isProtectedDataAvailable(): Boolean { return true }
actual suspend fun awaitProtectedDataAvailable() { }

object DataStoreManager {
    private var _dataStore: DataStore<Preferences>? = null

    // Initialize the DataStore with this method before using it
    fun initialize(context: Context) {
        if (_dataStore == null) {
            _dataStore = createPreferencesDataStore(context)
        }
    }

    private fun createPreferencesDataStore(appContext: Context): DataStore<Preferences> {
        val path = appContext.filesDir.resolve(dataStoreFileName).absolutePath
        return getPreferencesDataStore(path)
    }

    val dataStore: DataStore<Preferences>
        get() = _dataStore ?: throw IllegalStateException("DataStore is not initialized. Call initialize() first.")
}

actual val dataStore = DataStoreManager.dataStore
