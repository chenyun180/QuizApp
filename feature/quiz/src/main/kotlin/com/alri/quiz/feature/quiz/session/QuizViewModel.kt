package com.alri.quiz.feature.quiz.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alri.quiz.core.data.repository.QuizRepository
import com.alri.quiz.core.data.repository.SourcedQuestion
import com.alri.quiz.core.model.GradeResult
import com.alri.quiz.core.model.Question
import com.alri.quiz.core.ui.navigation.QuizMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 刷题页的 ViewModel。
 *
 * 状态机很简单但必须严格：
 * - 未提交：可以改选项
 * - 已提交：选项锁死（判分后禁止重复作答是产品要求），显示对错和解析
 * - 翻页不重置已答状态
 *
 * 进度（题目、每题的选中项和对错、当前下标）落在 [SavedStateHandle]，
 * 进程被系统回收后重建 ViewModel 时能原样恢复。
 */
@HiltViewModel
class QuizViewModel @Inject constructor(
    private val repository: QuizRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val store = QuizSessionStore(savedStateHandle)

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    /**
     * 当前这道题是否已收藏。跟着当前题切换而切换，
     * 所以用 flatMapLatest 而不是一次性查询——翻页时自动取消上一条订阅。
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isCurrentFavorite: StateFlow<Boolean> = _uiState
        .map { it.current }
        .distinctUntilChanged()
        .flatMapLatest { current ->
            if (current == null) {
                flowOf(false)
            } else {
                repository.observeIsFavorite(current.bankId, current.question.id)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private var loaded = false

    fun start(mode: String?, bankId: String?, tag: String?) {
        if (loaded) return
        loaded = true

        // 先尝试恢复存档：这是进程被杀后重建的唯一路径
        val restored = store.restore()
        if (restored != null) {
            _uiState.value = QuizUiState(
                title = restored.title,
                isLoading = false,
                questions = restored.states,
                currentIndex = restored.index,
            )
            return
        }

        val quizMode = runCatching { QuizMode.valueOf(mode.orEmpty()) }.getOrDefault(QuizMode.BANK)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            val loadedQuestions = try {
                loadFor(quizMode, bankId.orEmpty(), tag.orEmpty())
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, loadError = e.message ?: "加载题目失败")
                }
                return@launch
            }

            if (loadedQuestions.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadError = null,
                        title = defaultTitle(quizMode),
                        questions = emptyList(),
                    )
                }
                return@launch
            }

            val states = loadedQuestions.map { QuestionState(bankId = it.bankId, question = it.question) }
            val title = titleFor(quizMode, bankId.orEmpty(), tag.orEmpty())
            _uiState.value = QuizUiState(
                title = title,
                isLoading = false,
                questions = states,
                currentIndex = 0,
            )
            store.saveSession(title, states, 0)
        }
    }

    private suspend fun loadFor(mode: QuizMode, bankId: String, tag: String): List<SourcedQuestion> =
        when (mode) {
            QuizMode.BANK -> repository.loadQuestions(bankId).map { SourcedQuestion(bankId, it) }
            QuizMode.TAG -> repository.loadQuestionsByTag(bankId, tag).map { SourcedQuestion(bankId, it) }
            QuizMode.RANDOM -> repository.randomQuestions(bankId, RANDOM_LIMIT).map { SourcedQuestion(bankId, it) }
            QuizMode.WRONG -> repository.loadWrongQuestions()
            QuizMode.FAVORITE -> repository.loadFavoriteQuestions()
        }

    /** 点选项。单选/判断点新选项会替换旧的；多选是切换。已提交的题不再接受输入。 */
    fun selectOption(key: String) {
        _uiState.update { state ->
            val current = state.current ?: return@update state
            if (current.isSubmitted) return@update state

            val newSelection = if (current.allowsMultiple) {
                if (key in current.selected) current.selected - key else current.selected + key
            } else {
                setOf(key)
            }
            state.replaceCurrent(current.copy(selected = newSelection))
        }
        persist()
    }

    /** 提交作答。判分后落库并锁死本题。 */
    fun submit() {
        val state = _uiState.value
        val current = state.current ?: return
        if (current.isSubmitted || current.selected.isEmpty()) return

        viewModelScope.launch {
            val grade = repository.submitAnswer(current.bankId, current.question, current.selected)
            applyGrade(current.question.id, grade)
        }
    }

    private fun applyGrade(questionId: String, grade: GradeResult) {
        _uiState.update { state ->
            val index = state.questions.indexOfFirst { it.question.id == questionId }
            if (index < 0) return@update state
            val target = state.questions[index]
            state.copy(questions = state.questions.toMutableList().also {
                it[index] = target.copy(grade = grade)
            })
        }
        persist()
    }

    fun next() {
        _uiState.update { state ->
            if (!state.canGoNext) state else state.copy(currentIndex = state.currentIndex + 1)
        }
        persist()
    }

    fun previous() {
        _uiState.update { state ->
            if (!state.canGoPrevious) state else state.copy(currentIndex = state.currentIndex - 1)
        }
        persist()
    }

    fun toggleFavorite() {
        val state = _uiState.value
        val current = state.current ?: return
        viewModelScope.launch { repository.toggleFavorite(current.bankId, current.question.id) }
    }

    private fun persist() {
        val state = _uiState.value
        if (state.questions.isEmpty()) return
        store.saveSession(
            title = state.title,
            states = state.questions,
            index = state.currentIndex,
        )
    }

    private fun titleFor(mode: QuizMode, bankId: String, tag: String): String = when (mode) {
        QuizMode.TAG -> "标签：$tag"
        QuizMode.RANDOM -> "随机练习"
        QuizMode.WRONG -> "错题练习"
        QuizMode.FAVORITE -> "收藏练习"
        QuizMode.BANK -> bankId
    }

    private fun defaultTitle(mode: QuizMode): String = when (mode) {
        QuizMode.TAG -> "标签练习"
        QuizMode.RANDOM -> "随机练习"
        QuizMode.WRONG -> "错题练习"
        QuizMode.FAVORITE -> "收藏练习"
        QuizMode.BANK -> "练习"
    }

    private fun QuizUiState.replaceCurrent(newCurrent: QuestionState): QuizUiState {
        val list = questions.toMutableList()
        val index = list.indexOfFirst { it.question.id == newCurrent.question.id }
        if (index < 0) return this
        list[index] = newCurrent
        return copy(questions = list)
    }

    private companion object {
        const val RANDOM_LIMIT = 20
    }
}
