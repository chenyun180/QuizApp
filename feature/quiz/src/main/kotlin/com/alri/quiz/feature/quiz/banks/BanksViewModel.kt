package com.alri.quiz.feature.quiz.banks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alri.quiz.core.data.repository.QuizRepository
import com.alri.quiz.core.model.BankSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 首页（题库列表）的状态。 */
data class BanksUiState(
    val isLoading: Boolean = true,
    val banks: List<BankSummary> = emptyList(),
    val wrongCount: Int = 0,
    val answeredTotal: Int = 0,
    val correctTotal: Int = 0,
    val error: String? = null,
) {
    val isEmpty: Boolean get() = !isLoading && banks.isEmpty()
}

@HiltViewModel
class BanksViewModel @Inject constructor(
    private val repository: QuizRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BanksUiState())
    val uiState: StateFlow<BanksUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeBanks(),
                repository.observeWrongCount(),
                repository.observeOverallStats(),
            ) { banks, wrong, stats ->
                BanksUiState(
                    isLoading = false,
                    banks = banks,
                    wrongCount = wrong,
                    answeredTotal = stats.first,
                    correctTotal = stats.second,
                )
            }
                .catch { e ->
                    _uiState.value = BanksUiState(
                        isLoading = false,
                        error = e.message ?: "读取题库失败",
                    )
                }
                .collect { _uiState.value = it }
        }
    }

    fun deleteBank(bankId: String) {
        viewModelScope.launch { repository.deleteBank(bankId) }
    }
}
