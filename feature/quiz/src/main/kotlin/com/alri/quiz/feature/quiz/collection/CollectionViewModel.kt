package com.alri.quiz.feature.quiz.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alri.quiz.core.data.repository.QuizRepository
import com.alri.quiz.core.data.repository.SourcedQuestion
import com.alri.quiz.core.ui.navigation.QuizMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionUiState(
    val isLoading: Boolean = true,
    val items: List<SourcedQuestion> = emptyList(),
)

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val repository: QuizRepository,
    savedStateHandle: androidx.lifecycle.SavedStateHandle,
) : ViewModel() {

    private val mode: QuizMode = when (
        runCatching { QuizMode.valueOf(savedStateHandle.get<String>("mode").orEmpty()) }.getOrNull()
    ) {
        QuizMode.FAVORITE -> QuizMode.FAVORITE
        else -> QuizMode.WRONG
    }

    private val _uiState = MutableStateFlow(CollectionUiState())
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // 一次性读取：错题本/收藏在打开这一屏期间不需要实时刷新，
            // 反而应该在用户离开时把结果固定下来，避免刷题中途列表突然变空。
            val items = when (mode) {
                QuizMode.FAVORITE -> repository.loadFavoriteQuestions()
                else -> repository.loadWrongQuestions()
            }
            _uiState.value = CollectionUiState(isLoading = false, items = items)
        }
    }
}
