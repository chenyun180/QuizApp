package com.alri.quiz.feature.quiz.session

import androidx.lifecycle.SavedStateHandle
import com.alri.quiz.core.model.GradeResult
import com.alri.quiz.core.model.Option
import com.alri.quiz.core.model.Question
import com.alri.quiz.core.model.QuestionType

/** 一道题在本次练习里的状态。 */
data class QuestionState(
    /** 题目所属题库。跨题库的错题/收藏练习里，每道题的库可能不同。 */
    val bankId: String,
    val question: Question,
    /** 当前选中的选项。判分后保持不动，用于回看。 */
    val selected: Set<String> = emptySet(),
    /** 判分结果；null 表示还没提交。 */
    val grade: GradeResult? = null,
) {
    val isSubmitted: Boolean get() = grade != null

    /** 只有多选题允许在提交前勾多个。 */
    val allowsMultiple: Boolean get() = question.type == QuestionType.MULTIPLE
}

/** 刷题页的完整 UI 状态。Compose 直接渲染它，不用在 Composable 里算任何东西。 */
data class QuizUiState(
    val title: String = "",
    val isLoading: Boolean = true,
    val questions: List<QuestionState> = emptyList(),
    val currentIndex: Int = 0,
    /** 题目加载失败（题库为空、题库已删除等）。 */
    val loadError: String? = null,
) {
    val current: QuestionState? get() = questions.getOrNull(currentIndex)
    val total: Int get() = questions.size
    val answeredCount: Int get() = questions.count { it.isSubmitted }
    val correctCount: Int get() = questions.count { it.grade?.isCorrect == true }
    val canGoPrevious: Boolean get() = currentIndex > 0
    val canGoNext: Boolean get() = currentIndex < questions.lastIndex
    val isLast: Boolean get() = questions.isNotEmpty() && currentIndex == questions.lastIndex
    val isFinished: Boolean get() = isLast && (current?.isSubmitted == true)
}

/**
 * 会话状态的存取，落在 [SavedStateHandle]（底层是 Bundle）。
 *
 * 为什么不用导航参数传题目：题目可能有几千道，导航参数有大小限制，
 * 一传就 `TransactionTooLargeException`。
 *
 * 为什么用自定义文本编码而不是 JSON：这里的数据结构极简（题目 + 选中项），
 * 手写编码没有反射/插件依赖，恢复失败还能优雅降级；代价是不支持题干里出现
 * 两个控制字符（U+001F / U+001E），实际不可能。
 */
class QuizSessionStore(private val handle: SavedStateHandle) {

    fun saveSession(title: String, states: List<QuestionState>, index: Int) {
        handle[KEY_TITLE] = title
        handle[KEY_QUESTIONS] = states.map { it.encodeState() }.toTypedArray()
        handle[KEY_ANSWERS] = states.mapIndexedNotNull { i, state ->
            state.grade?.let { grade -> encodeAnswer(i, grade) }
        }.toTypedArray()
        handle[KEY_INDEX] = index
    }

    /** @return null 表示没有存档，或存档已损坏 */
    fun restore(): RestoredSession? {
        val title: String = handle.get<String>(KEY_TITLE) ?: return null
        val lines: Array<String> = handle.get<Array<String>>(KEY_QUESTIONS) ?: return null
        if (lines.isEmpty()) return null

        val decoded = try {
            lines.map { it.decodeState() }
        } catch (e: RuntimeException) {
            return null
        }

        val answers: Map<Int, Pair<Set<String>, Boolean>> = handle.get<Array<String>>(KEY_ANSWERS)
            ?.mapNotNull { decodeAnswer(it) }
            ?.toMap()
            .orEmpty()

        val states = decoded.mapIndexed { i, (bankId, question) ->
            val saved = answers[i] ?: return@mapIndexed QuestionState(bankId, question)
            QuestionState(
                bankId = bankId,
                question = question,
                selected = saved.first,
                grade = GradeResult(
                    questionId = question.id,
                    selected = saved.first,
                    correctAnswers = question.answers,
                    isCorrect = saved.second,
                ),
            )
        }

        val index = (handle.get<Int>(KEY_INDEX) ?: 0).coerceIn(0, states.lastIndex)
        return RestoredSession(title = title, states = states, index = index)
    }

    private fun encodeAnswer(index: Int, grade: GradeResult): String =
        "$index\u001D${if (grade.isCorrect) 1 else 0}\u001D${grade.selected.joinToString(",")}"

    private fun decodeAnswer(raw: String): Pair<Int, Pair<Set<String>, Boolean>>? {
        val parts = raw.split('\u001D')
        if (parts.size != 3) return null
        val index = parts[0].toIntOrNull() ?: return null
        val selected = if (parts[2].isEmpty()) emptySet() else parts[2].split(",").toSet()
        return index to (selected to (parts[1] == "1"))
    }

    /** 把 (bankId, 题目) 编码成一行。bankId 放在最前面，解码时无需额外映射。 */
    private fun QuestionState.encodeState(): String =
        listOf(bankId, question.encodeFields()).joinToString(FIELD_SEP)

    private fun String.decodeState(): Pair<String, Question> {
        val f = split(FIELD_SEP)
        require(f.size >= 9) { "存档字段数不对：${f.size}" }
        return f[0] to decodeFields(f.subList(1, f.size))
    }

    private fun Question.encodeFields(): String = listOf(
        id,
        type.name,
        stem,
        options.joinToString(OPTION_SEP) { "${it.key}=${it.text}" },
        answers.joinToString(","),
        explanation.orEmpty(),
        tags.joinToString(","),
        imageRef.orEmpty(),
    ).joinToString(FIELD_SEP)

    private fun decodeFields(f: List<String>): Question {
        require(f.size >= 8) { "存档字段数不对：${f.size}" }
        return Question(
            id = f[0],
            type = QuestionType.valueOf(f[1]),
            stem = f[2],
            options = if (f[3].isEmpty()) {
                emptyList()
            } else {
                f[3].split(OPTION_SEP).map { pair ->
                    val sep = pair.indexOf('=')
                    Option(pair.substring(0, sep), pair.substring(sep + 1))
                }
            },
            answers = if (f[4].isEmpty()) emptySet() else f[4].split(",").toSet(),
            explanation = f[5].ifEmpty { null },
            tags = if (f[6].isEmpty()) emptyList() else f[6].split(","),
            imageRef = f[7].ifEmpty { null },
        )
    }

    data class RestoredSession(
        val title: String,
        val states: List<QuestionState>,
        val index: Int,
    )

    private companion object {
        const val KEY_TITLE = "quiz.title"
        const val KEY_QUESTIONS = "quiz.questions"
        const val KEY_ANSWERS = "quiz.answers"
        const val KEY_INDEX = "quiz.index"
        const val FIELD_SEP = "\u001F"
        const val OPTION_SEP = "\u001E"
    }
}
