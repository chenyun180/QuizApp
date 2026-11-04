package com.alri.quiz.feature.quiz.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alri.quiz.core.data.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val bankCount: Int = 0,
    val answeredTotal: Int = 0,
    val correctTotal: Int = 0,
    val isClearing: Boolean = false,
    val cleared: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: QuizRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeBanks()
                .catch { /* 设置页读不到统计不影响使用，保持默认值 */ }
                .collect { banks ->
                    _uiState.value = _uiState.value.copy(bankCount = banks.size)
                }
        }
        viewModelScope.launch {
            repository.observeOverallStats().collect { (answered, correct) ->
                _uiState.value = _uiState.value.copy(answeredTotal = answered, correctTotal = correct)
            }
        }
    }

    fun clearAllData() {
        if (_uiState.value.isClearing) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isClearing = true, cleared = false)
            repository.clearAll()
            _uiState.value = _uiState.value.copy(isClearing = false, cleared = true)
        }
    }

    fun consumeClearedFlag() {
        _uiState.value = _uiState.value.copy(cleared = false)
    }
}
