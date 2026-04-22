package com.example.studyplannerai.viewmodel.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyplannerai.data.repository.SettingsRepository
import com.example.studyplannerai.data.repository.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val settings: UserSettings = UserSettings(),
    val message: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application.applicationContext)
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeSettings().collect { settings ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        settings = settings
                    )
                }
            }
        }
    }

    fun updateWeeklyView(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateWeeklyView(enabled)
            _uiState.update { it.copy(message = "Weekly view preference updated.") }
        }
    }

    fun updateAutoReschedule(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAutoReschedule(enabled)
            _uiState.update { it.copy(message = "Reschedule preference updated.") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
