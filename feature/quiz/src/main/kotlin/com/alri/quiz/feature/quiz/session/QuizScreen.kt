package com.alri.quiz.feature.quiz.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alri.quiz.core.model.Option
import com.alri.quiz.core.model.QuestionType
import com.alri.quiz.core.ui.component.EmptyState
import com.alri.quiz.core.ui.component.LoadingState
import com.alri.quiz.core.ui.component.TagChips
import com.alri.quiz.core.ui.theme.LocalQuizExtraColors
import com.alri.quiz.feature.quiz.R

/**
 * 刷题页。
 *
 * 布局分三段：进度条 / 可滚动的题目区 / 底部操作条。
 * 底部操作条固定在 Scaffold 的 bottomBar，键盘或长解析都不会把它顶走。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isCurrentFavorite.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.quiz_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = stringResource(
                                if (isFavorite) R.string.quiz_unfavorite else R.string.quiz_favorite,
                            ),
                            tint = if (isFavorite) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                },
            )
        },
        bottomBar = {
            val current = state.current
            if (current != null) {
                BottomBar(
                    state = state,
                    current = current,
                    onSubmit = viewModel::submit,
                    onPrevious = viewModel::previous,
                    onNext = viewModel::next,
                )
            }
        },
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(innerPadding))

            state.loadError != null -> EmptyState(
                icon = Icons.Filled.Close,
                title = stringResource(R.string.quiz_error_title),
                description = state.loadError,
                modifier = Modifier.padding(innerPadding),
            )

            state.questions.isEmpty() -> EmptyState(
                icon = Icons.Filled.Check,
                title = stringResource(R.string.quiz_empty_title),
                description = stringResource(R.string.quiz_empty_desc),
                modifier = Modifier.padding(innerPadding),
            )

            else -> {
                val current = state.current ?: return@Scaffold
                Column(modifier = Modifier.padding(innerPadding)) {
                    LinearProgressIndicator(
                        progress = { (state.currentIndex + 1f) / state.total },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    Text(
                        text = stringResource(R.string.quiz_progress, state.currentIndex + 1, state.total),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    QuestionBody(
                        state = current,
                        answeredCount = state.answeredCount,
                        onSelect = viewModel::selectOption,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionBody(
    state: QuestionState,
    answeredCount: Int,
    onSelect: (String) -> Unit,
) {
    val question = state.question
    val extras = LocalQuizExtraColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        TagChips(tags = question.tags)

        Text(
            text = question.stem,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 8.dp),
        )

        Text(
            text = when (question.type) {
                QuestionType.SINGLE -> stringResource(R.string.quiz_type_single)
                QuestionType.MULTIPLE -> stringResource(R.string.quiz_type_multiple)
                QuestionType.TRUE_FALSE -> stringResource(R.string.quiz_type_true_false)
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(16.dp))

        question.options.forEach { option ->
            OptionRow(
                option = option,
                state = state,
                onClick = { onSelect(option.key) },
            )
            Spacer(Modifier.height(8.dp))
        }

        if (state.isSubmitted) {
            GradePanel(state = state, correctColor = extras.correct, wrongColor = extras.wrong)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun OptionRow(
    option: Option,
    state: QuestionState,
    onClick: () -> Unit,
) {
    val extras = LocalQuizExtraColors.current
    val correctAnswers = state.question.answers
    val submitted = state.isSubmitted
    val isSelected = option.key in state.selected
    val isCorrectOption = option.key in correctAnswers

    // 判分后的配色规则：正确项永远是绿的；选错的那一项标红；其余保持中性。
    val (container, border, keyColor) = when {
        !submitted && isSelected -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary,
        )
        submitted && isCorrectOption -> Triple(
            extras.correct.copy(alpha = 0.12f),
            extras.correct,
            extras.correct,
        )
        submitted && isSelected && !isCorrectOption -> Triple(
            extras.wrong.copy(alpha = 0.12f),
            extras.wrong,
            extras.wrong,
        )
        else -> Triple(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.outlineVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            // 已提交后整行不可点，同时读屏软件也会知道它是 disabled
            .toggleable(
                value = isSelected,
                enabled = !submitted,
                role = Role.Checkbox,
                onValueChange = { onClick() },
            ),
        shape = MaterialTheme.shapes.medium,
        color = container,
        border = BorderStroke(if (isSelected || (submitted && isCorrectOption)) 2.dp else 1.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.key,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = keyColor,
                )
            }
            Text(
                text = option.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
            if (submitted) {
                when {
                    isCorrectOption -> Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = extras.correct,
                        modifier = Modifier.size(20.dp),
                    )
                    isSelected -> Icon(
                        Icons.Filled.Close,
                        contentDescription = null,
                        tint = extras.wrong,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun GradePanel(
    state: QuestionState,
    correctColor: Color,
    wrongColor: Color,
) {
    val grade = state.grade ?: return
    val accent = if (grade.isCorrect) correctColor else wrongColor
    val question = state.question

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f)),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (grade.isCorrect) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(
                        if (grade.isCorrect) R.string.quiz_correct else R.string.quiz_wrong,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }

            if (!grade.isCorrect) {
                Text(
                    text = stringResource(
                        R.string.quiz_correct_answer,
                        question.answers.sorted().joinToString("、"),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            val explanation = question.explanation
            if (explanation.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.quiz_no_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                Text(
                    text = stringResource(R.string.quiz_explanation, explanation),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun BottomBar(
    state: QuizUiState,
    current: QuestionState,
    onSubmit: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (state.isFinished) {
                Text(
                    text = stringResource(
                        R.string.quiz_summary,
                        state.correctCount,
                        state.answeredCount,
                        if (state.answeredCount == 0) 0 else state.correctCount * 100 / state.answeredCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = state.canGoPrevious,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(stringResource(R.string.quiz_previous), modifier = Modifier.padding(start = 4.dp))
                }

                if (!current.isSubmitted) {
                    Button(
                        onClick = onSubmit,
                        enabled = current.selected.isNotEmpty(),
                        modifier = Modifier.weight(1.4f),
                    ) {
                        Text(stringResource(R.string.quiz_submit))
                    }
                } else {
                    Button(
                        onClick = onNext,
                        enabled = state.canGoNext,
                        modifier = Modifier.weight(1.4f),
                    ) {
                        Text(stringResource(R.string.quiz_next))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(start = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
