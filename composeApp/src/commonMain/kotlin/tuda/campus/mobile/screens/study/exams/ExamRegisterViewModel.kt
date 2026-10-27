package tuda.campus.mobile.screens.study.exams

import androidx.lifecycle.ViewModel
import tuda.campus.mobile.managers.tucanManager.ExamRegistration
import tuda.campus.mobile.managers.tucanManager.RegistrationOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExamRegisterViewModel(): ViewModel() {

    /*
    private val _showAlert = MutableStateFlow(false)
    val showAlert = _showAlert.asStateFlow()
     */

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _selection = MutableStateFlow<List<ExamRegistration>>(emptyList())
    val selection = _selection.asStateFlow()

    //private val _selectionMode = MutableStateFlow<RegistrationOption?>(null)
    private val _selectionMode = MutableStateFlow<RegistrationOption?>(null)
    val selectionMode = _selectionMode.asStateFlow()

    fun updateIsLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }

    fun updateSelectionMode(isEnabled: Boolean) {
        if (isEnabled) {
            _selectionMode.value = RegistrationOption.UNAVAILABLE
        } else {
            _selectionMode.value = null
            _selection.value = emptyList()
        }
    }

    fun selectExam(registration: ExamRegistration, isSelected: Boolean) {
        if (selectionMode.value == registration.registerOption || selectionMode.value == RegistrationOption.UNAVAILABLE) {
            if (isSelected) {
                val newList = _selection.value.toMutableList()
                newList.remove(registration)
                _selection.value = newList

                if (selection.value.isEmpty()) {
                    _selectionMode.value = RegistrationOption.UNAVAILABLE
                }
            } else {
                val newList = _selection.value.toMutableList()
                newList.add(registration)
                _selection.value = newList

                if (selectionMode.value == RegistrationOption.UNAVAILABLE) {
                    _selectionMode.value = registration.registerOption
                }
            }
        }
    }

    fun deselectAllExams() {
        _selection.value = emptyList()
        _selectionMode.value = RegistrationOption.UNAVAILABLE
    }


    //@MainActor
    fun completeModify() {
        _isLoading.value = false
        _selectionMode.value = null
        _selection.value = emptyList()
    }
}