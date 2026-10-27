package tuda.campus.mobile.managers

import tuda.campus.mobile.PlatformImage
import tuda.campus.mobile.dataStore
import tuda.campus.mobile.persistentImageStateFlow
import tuda.campus.mobile.persistentStateFlow
import tuda.campus.mobile.settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


enum class UIType {
    SWIFT_UI,
    COMPOSE
}

enum class TimeViewType {
    WEEK, LIST
}

object SettingsManager {
    private val _userImage: MutableStateFlow<PlatformImage?> = dataStore.persistentImageStateFlow("UserImage", defaultValue = null)
    val userImage = _userImage.asStateFlow()

    private val _timeViewType = settings.persistentStateFlow("TimeViewType", TimeViewType.WEEK)
    val timeViewType = _timeViewType.asStateFlow()

    private val _isMfaLoginAvailable = settings.persistentStateFlow("MfaLoginAvailable", false)
    val isMfaLoginAvailable = _isMfaLoginAvailable.asStateFlow()

    private val _isDemoLoginAvailable = settings.persistentStateFlow("DemoLoginAvailable", false)
    val isDemoLoginAvailable = _isDemoLoginAvailable.asStateFlow()

    private val _uiType = settings.persistentStateFlow("UIType", UIType.SWIFT_UI)
    val uiType = _uiType.asStateFlow()


    /**
     * Sets [userImage] to a new Image, which is the image used for the StudentID Card
     */
    fun setUserImage(imageData: ByteArray) {
        _userImage.value = PlatformImage(imageData)
    }

    /**
     * A debugging function which toggles [uiType] between [Compose][UIType.COMPOSE] and [SwiftUI][UIType.SWIFT_UI]
     */
    fun switchUiType() {
        kotlinx.coroutines.MainScope().launch {
            if (uiType.value == UIType.COMPOSE) {
                _uiType.update { UIType.SWIFT_UI }
            } else {
                _uiType.update { UIType.COMPOSE }
            }
        }
    }

    fun updateTimeViewType(newType: TimeViewType) {
        _timeViewType.value = newType
    }

    fun updateMfaLoginAvailability(isAvailable: Boolean) {
        _isMfaLoginAvailable.value = isAvailable
    }

    fun updateDemoLoginAvailability(isAvailable: Boolean) {
        _isDemoLoginAvailable.value = isAvailable
    }
}