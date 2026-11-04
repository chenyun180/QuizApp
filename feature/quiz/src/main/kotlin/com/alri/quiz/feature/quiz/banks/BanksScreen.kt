package com.alri.quiz.feature.quiz.banks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alri.quiz.core.model.BankSummary
import com.alri.quiz.core.ui.component.EmptyState
import com.alri.quiz.core.ui.component.LoadingState
import com.alri.quiz.core.ui.component.SectionHeader
import com.alri.quiz.feature.quiz.R

/**
 * 首页：题库列表 + 错题本 / 收藏 的入口 + 导入按钮。
 *
 * 注意这一屏没有业务逻辑：所有数据来自 [BanksUiState]，所有动作都是回调。
 * 新手最容易犯的错是把 repository 直接塞进 Composable，那样没法预览、没法测。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BanksScreen(
    onOpenBank: (String) -> Unit,
    onOpenRandom: (String) -> Unit,
    onOpenWrongBook: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BanksViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<BankSummary?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.banks_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.List, contentDescription = stringResource(R.string.banks_settings))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenImport,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.banks_import)) },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(innerPadding))

            state.error != null -> EmptyState(
                icon = Icons.Filled.Warning,
                title = stringResource(R.string.banks_error_title),
                description = state.error,
                actionLabel = stringResource(R.string.action_retry),
                onAction = { /* Flow 会自动重试；这里保持按钮可用不误导 */ },
                modifier = Modifier.padding(innerPadding),
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                item { PracticeShortcuts(state, onOpenWrongBook, onOpenFavorites) }

                if (state.isEmpty) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.List,
                            title = stringResource(R.string.banks_empty_title),
                            description = stringResource(R.string.banks_empty_desc),
                            actionLabel = stringResource(R.string.banks_import),
                            onAction = onOpenImport,
                            modifier = Modifier.padding(top = 48.dp),
                        )
                    }
                } else {
                    item { SectionHeader(stringResource(R.string.banks_section_banks)) }
                    items(state.banks, key = { it.id }) { bank ->
                        BankRow(
                            bank = bank,
                            onClick = { onOpenBank(bank.id) },
                            onRandom = { onOpenRandom(bank.id) },
                            onDelete = { pendingDelete = bank },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { bank ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.banks_delete_title)) },
            text = { Text(stringResource(R.string.banks_delete_message, bank.title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBank(bank.id)
                    pendingDelete = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun PracticeShortcuts(
    state: BanksUiState,
    onOpenWrongBook: () -> Unit,
    onOpenFavorites: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssistChip(
            onClick = onOpenWrongBook,
            label = { Text(stringResource(R.string.banks_wrong_book, state.wrongCount)) },
            leadingIcon = { Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(18.dp)) },
        )
        AssistChip(
            onClick = onOpenFavorites,
            label = { Text(stringResource(R.string.banks_favorites)) },
            leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(18.dp)) },
        )
    }
    if (state.answeredTotal > 0) {
        Text(
            text = stringResource(
                R.string.banks_stats,
                state.answeredTotal,
                state.correctTotal,
                if (state.answeredTotal == 0) 0 else state.correctTotal * 100 / state.answeredTotal,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun BankRow(
    bank: BankSummary,
    onClick: () -> Unit,
    onRandom: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = bank.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.banks_question_count, bank.questionCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onRandom) {
                    Text(stringResource(R.string.banks_random))
                }
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.action_delete))
                }
            }
        }
    }
}
