package com.alri.quiz.feature.bankimport

import android.content.ContentResolver
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.alri.quiz.feature.bankimport.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alri.quiz.core.data.importer.ImportError
import com.alri.quiz.core.ui.theme.LocalQuizExtraColors

/**
 * 导入页。
 *
 * 文件选择用 Storage Access Framework（`OpenDocument`）：不需要任何存储权限，
 * 用户在系统文件选择器里挑一个文件，App 只拿到那一个 URI 的临时读取权。
 * 这比 `READ_EXTERNAL_STORAGE` 干净得多——后者在 Android 13+ 已经基本不可用，
 * 而且在商店审核里是减分项。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    onImported: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resolver: ContentResolver = context.contentResolver

    val successText: (String, Int) -> String = { title, count ->
        context.getString(R.string.import_success, title, count)
    }
    val replacedText: (String, Int) -> String = { title, count ->
        context.getString(R.string.import_success_replaced, title, count)
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importUri(resolver, uri, successText, replacedText)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Button(
                onClick = {
                    // mime 同时给 json 和 text/*：很多网盘/文件管理器把 .json 报成 text/plain，
                    // 只传 application/json 会让用户根本选不到文件
                    picker.launch(arrayOf("application/json", "text/*", "application/octet-stream"))
                },
                enabled = !state.isImporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(stringResource(R.string.import_in_progress), modifier = Modifier.padding(start = 8.dp))
                } else {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.import_pick_file), modifier = Modifier.padding(start = 8.dp))
                }
            }

            Text(
                text = stringResource(R.string.import_pick_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            state.successMessage?.let { message ->
                SuccessCard(
                    message = message,
                    onGoBack = onImported,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            if (state.errors.isNotEmpty()) {
                ErrorPanel(
                    errors = state.errors,
                    onDismiss = viewModel::dismissErrors,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            FormatContract(modifier = Modifier.padding(top = 24.dp))
        }
    }
}

@Composable
private fun SuccessCard(message: String, onGoBack: () -> Unit, modifier: Modifier = Modifier) {
    val extras = LocalQuizExtraColors.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = extras.correct.copy(alpha = 0.10f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = extras.correct)
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            TextButton(onClick = onGoBack, modifier = Modifier.padding(top = 4.dp)) {
                Text(stringResource(R.string.import_dismiss))
            }
        }
    }
}

@Composable
private fun ErrorPanel(errors: List<ImportError>, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val extras = LocalQuizExtraColors.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = extras.wrong.copy(alpha = 0.08f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = extras.wrong)
                Text(
                    text = stringResource(R.string.import_failed_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = extras.wrong,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                text = stringResource(R.string.import_error_count, errors.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = stringResource(R.string.import_failed_hint),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            // 错误可能很多，这里限制高度 + 内部滚动，避免整页被撑爆
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(errors.size.coerceAtMost(MAX_ERRORS_SHOWN)) { index ->
                    val error = errors[index]
                    Text(
                        text = "• ${error.format()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Default,
                    )
                }
                if (errors.size > MAX_ERRORS_SHOWN) {
                    item {
                        Text(
                            text = "… 还有 ${errors.size - MAX_ERRORS_SHOWN} 处",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 4.dp)) {
                Text(stringResource(R.string.import_dismiss))
            }
        }
    }
}

@Composable
private fun FormatContract(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.import_contract_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.import_contract_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = SAMPLE_JSON,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private const val MAX_ERRORS_SHOWN = 30

private val SAMPLE_JSON = """
{
  "schemaVersion": 1,
  "bankId": "demo-basic",
  "title": "示例题库",
  "questions": [
    {
      "id": "q1",
      "type": "single",
      "stem": "题干",
      "options": [
        {"key": "A", "text": "选项一"},
        {"key": "B", "text": "选项二"}
      ],
      "answers": ["A"],
      "explanation": "解析",
      "tags": ["基础"],
      "image": null
    }
  ]
}
""".trimIndent()
