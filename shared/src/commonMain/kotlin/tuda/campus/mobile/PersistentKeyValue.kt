package tuda.campus.mobile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.liftric.kvault.KVault
import com.russhwolf.settings.Settings
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.compressImage
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.write
import korlibs.io.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import kotlin.concurrent.atomics.ExperimentalAtomicApi

val settings: Settings = Settings()
// in Android, this needs to be initialized first, see PersistentKeyValue.android.kt
expect val vault: KVault

/**
 * On iOS check if encrypted values can be retrieved
 */
expect fun isProtectedDataAvailable(): Boolean

/**
 * On iOS delay execution until encrypted values can be retrieved
 */
expect suspend fun awaitProtectedDataAvailable()

internal const val dataStoreFileName = "dataStore.preferences_pb"

fun getPreferencesDataStore(path: String) = PreferenceDataStoreFactory.createWithPath {
    path.toPath()
}

// in Android, this needs to be initialized first, see PersistentKeyValue.android.kt
expect val dataStore: DataStore<Preferences>

/**
 * Creates a MutableStateFlow that automatically persists app restarts.
 *
 * It works by serializing the specified type and storing it as json in DataStore.
 */
@OptIn(ExperimentalAtomicApi::class)
inline fun <reified T: @Serializable Any?> DataStore<Preferences>.persistentStateFlow(
    keyName: String,
    defaultValue: T,
    crossinline onInitialLoad: (T) -> Unit = {},
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
): MutableStateFlow<T> {
    val key = stringPreferencesKey(keyName)
    val _state = MutableStateFlow(defaultValue)

    // Initial load, might be smart to use runBlocking here and set _state afterwards (?)
    runBlocking {
        data.first()[key]?.let { json ->
            try {
                _state.value = Json.decodeFromString<T>(json)
                onInitialLoad(_state.value)
            } catch (e: Exception) { }
        }
    }

    // Write on _state.value change, this is probably (unnecessarily) also called after first read
    scope.launch {
        _state.collect { value ->
            edit { prefs ->
                prefs[key] = Json.encodeToString(value)
            }
        }
    }

    return _state
}

/**
 * Creates a MutableStateFlow<PlatformImage> that automatically persists app restarts.
 *
 * The Image is saved as an internal file on Disk and path is stored in DataStore.
 */
fun DataStore<Preferences>.persistentImageStateFlow(
    keyName: String,
    defaultValue: PlatformImage? = null,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
): MutableStateFlow<PlatformImage?> {
    val key = stringPreferencesKey(keyName)
    val _state = MutableStateFlow(defaultValue)

    // Initial load, might be smart to use runBlocking here and set _state afterwards (?)
    scope.launch {
        data.first()[key]?.let { pathJson ->
            try {
                val path = Json.decodeFromString<String>(pathJson)
                val file = PlatformFile(path)
                if (file.exists()) {
                    _state.value = PlatformImage(file.readBytes())
                }
            } catch (e: Exception) { }
        }
    }

    // Write on _state.value change, this is probably (unnecessarily) also called after first read
    scope.launch {
        _state.collect { image ->
            image?.let { img ->
                val compressed = FileKit.compressImage(bytes = img.bytes, imageFormat = ImageFormat.PNG, quality = 85)
                val file = FileKit.cacheDir / "${UUID.randomUUID()}.jpg"
                file.write(compressed)

                edit { prefs ->
                    prefs[key] = Json.encodeToString(file.path)
                }
            } ?: edit { prefs ->
                prefs.remove(key)
            }
        }
    }

    return _state
}



// Goal: be able to transition
//       private val _varName = MutableStateFlow("")
//       val varName = _varName.asStateFlow()
//   to
//       private val _varName = settings.persistentStateFlow(key: "keyName", defaultValue: "")
//       val varName = _varName.asStateFlow()
//   for making a StateFlow persistent
//   or alternatively if it needs to be encrypted
//       private val _varName = vault.persistentEncryptedStateFlow(key: "keyName", defaultValue: "")
//       val varName = _varName.asStateFlow()


private inline fun <reified T> Settings.persistentStateFlow(key: String, defaultValue: T, scope: CoroutineScope): MutableStateFlow<T> {
    val settings = this@persistentStateFlow

    val flow = MutableStateFlow(
        when (T::class) {
        String::class -> settings.getString(key, defaultValue as String) as T
        Int::class -> settings.getInt(key, defaultValue as Int) as T
        Boolean::class -> settings.getBoolean(key, defaultValue as Boolean) as T
        Float::class -> settings.getFloat(key, defaultValue as Float) as T
        Long::class -> settings.getLong(key, defaultValue as Long) as T
        Double::class -> settings.getDouble(key, defaultValue as Double) as T
        else -> throw IllegalArgumentException("Unsupported type ${T::class}")
    })

    scope.launch { flow.collect {
        when (T::class) {
            String::class -> putString(key, it as String)
            Int::class -> putInt(key, it as Int)
            Boolean::class -> putBoolean(key, it as Boolean)
            Float::class -> putFloat(key, it as Float)
            Long::class -> putLong(key, it as Long)
            Double::class -> putDouble(key, it as Double)
            else -> throw IllegalArgumentException("Unsupported type ${T::class}")
        }
    } }
    return flow
}

// Allowed are String, Int, Long, Float, Double, Boolean and all Enum types

inline fun <reified E : Enum<E>> Settings.persistentStateFlow(
    key: String,
    defaultValue: E,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
): MutableStateFlow<E> {
    val initial = getString(key, defaultValue.name)
        .let { stored ->
            enumValues<E>().firstOrNull { it.name == stored } ?: defaultValue
        }

    val flow = MutableStateFlow(initial)

    scope.launch {
        flow.collect { value ->
            putString(key, value.name)
        }
    }

    return flow
}

fun Settings.persistentStateFlow(
    key: String, defaultValue: String,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) = persistentStateFlow<String>(key, defaultValue, scope)

fun Settings.persistentStateFlow(
    key: String, defaultValue: Int,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) = persistentStateFlow<Int>(key, defaultValue, scope)

fun Settings.persistentStateFlow(
    key: String, defaultValue: Long,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) = persistentStateFlow<Long>(key, defaultValue, scope)

fun Settings.persistentStateFlow(
    key: String, defaultValue: Float,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) = persistentStateFlow<Float>(key, defaultValue, scope)

fun Settings.persistentStateFlow(
    key: String, defaultValue: Double,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) = persistentStateFlow<Double>(key, defaultValue, scope)

fun Settings.persistentStateFlow(
    key: String, defaultValue: Boolean,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) = persistentStateFlow<Boolean>(key, defaultValue, scope)


// Currently only implemented for String, but could also be generalized
// This implementation means that on iOS reads will fail if the device is locked
interface EncryptedValue<T> {
    suspend fun await(): T
    suspend fun set(value: T)
    suspend fun reset()
}

fun KVault.persistentEncryptedString(
    key: String,
    defaultValue: String
): EncryptedValue<String> =
    object : EncryptedValue<String> {
        override suspend fun await(): String {
            awaitProtectedDataAvailable()
            return string(key) ?: defaultValue
        }

        override suspend fun set(value: String) {
            awaitProtectedDataAvailable()
            set(key, value)
        }

        override suspend fun reset() {
            awaitProtectedDataAvailable()
            set(key, defaultValue)
        }
    }