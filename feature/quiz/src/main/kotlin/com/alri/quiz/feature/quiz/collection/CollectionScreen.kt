package com.alri.quiz.feature.quiz.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alri.quiz.core.model.QuestionType
import com.alri.quiz.core.ui.component.EmptyState
import com.alri.quiz.core.ui.component.LoadingState
import com.alri.quiz.core.ui.component.TagChips
import com.alri.quiz.feature.quiz.R

/** 错题本 / 收藏 共用一屏，只是数据源和文案不同。 */
enum class CollectionKind {
    WRONG,
    FAVORITE,
}

/**
 * 错题本（或收藏）列表。
 *
 * 列表本身只显示题干预览：错题本的作用是「快速定位到不熟的题」，
 * 直接把解析全部铺开反而看不清。点进练习再从第 1 题开始刷。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    kind: CollectionKind,
    onBack: () -> Unit,
    onStartPractice: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            when (kind) {
                                CollectionKind.WRONG -> R.string.wrong_book_title
                                CollectionKind.FAVORITE -> R.string.favorites_title
                            },
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.quiz_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(innerPadding))

            state.items.isEmpty() -> EmptyState(
                icon = when (kind) {
                    CollectionKind.WRONG -> Icons.Filled.Warning
                    CollectionKind.FAVORITE -> Icons.Outlined.Star
                },
                title = stringResource(
                    when (kind) {
                        CollectionKind.WRONG -> R.string.wrong_book_empty_title
                        CollectionKind.FAVORITE -> R.string.favorites_empty_title
                    },
                ),
                description = stringResource(
                    when (kind) {
                        CollectionKind.WRONG -> R.string.wrong_book_empty_desc
                        CollectionKind.FAVORITE -> R.string.favorites_empty_desc
                    },
                ),
                modifier = Modifier.padding(innerPadding),
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Button(
                        onClick = onStartPractice,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.collection_start_practice, state.items.size))
                    }
                }
                itemsIndexed(state.items, key = { _, item -> "${item.bankId}/${item.question.id}" }) { index, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "#${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = typeLabel(item.question.type),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                            Text(
                                text = item.question.stem,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            TagChips(tags = item.question.tags, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun typeLabel(type: QuestionType): String = stringResource(
    when (type) {
        QuestionType.SINGLE -> R.string.quiz_type_single
        QuestionType.MULTIPLE -> R.string.quiz_type_multiple
        QuestionType.TRUE_FALSE -> R.string.quiz_type_true_false
    },
)
