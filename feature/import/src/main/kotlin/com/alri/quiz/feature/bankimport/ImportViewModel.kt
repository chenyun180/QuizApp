package com.alri.quiz.feature.bankimport

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alri.quiz.core.data.importer.BankImporter
import com.alri.quiz.core.data.importer.ImportError
import com.alri.quiz.core.data.importer.ImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 导入页状态。 */
data class ImportUiState(
    val isImporting: Boolean = false,
    /** 导入失败时的错误清单；非空即展示错误面板。 */
    val errors: List<ImportError> = emptyList(),
    /** 成功提示，展示一次后清空。 */
    val successMessage: String? = null,
)

/** 一次性事件（导航返回、Snackbar）。用 Channel 而不是 StateFlow，避免旋转屏幕后重复触发。 */
sealed interface ImportEvent {
    data class Succeeded(val message: String, val reopenedExisting: Boolean = false) : ImportEvent
}

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val importer: BankImporter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private val _events = Channel<ImportEvent>(Channel.BUFFERED)
    val events: Flow<ImportEvent> = _events.receiveAsFlow()

    /**
     * 处理从文件选择器拿到的 URI。
     *
     * 注意必须在拿到 URI 后立刻读——SAF 授权的生命周期绑定在这条调用链上。
     * 这也是为什么这里接收 [ContentResolver] 而不是把它存起来稍后用。
     */
    fun importUri(
        resolver: ContentResolver,
        uri: Uri,
        successText: (title: String, count: Int) -> String,
        replacedText: (title: String, count: Int) -> String,
    ) {
        if (_uiState.value.isImporting) return
        viewModelScope.launch {
            _uiState.value = ImportUiState(isImporting = true)
            val result = importer.importFromUri(resolver, uri)
            _uiState.value = when (result) {
                is ImportResult.Failure -> ImportUiState(errors = result.errors)
                is ImportResult.Success -> {
                    val message = if (result.replaced) {
                        replacedText(result.bankTitle, result.questionCount)
                    } else {
                        successText(result.bankTitle, result.questionCount)
                    }
                    _events.send(ImportEvent.Succeeded(message))
                    ImportUiState(successMessage = message)
                }
            }
        }
    }

    fun dismissErrors() {
        _uiState.value = _uiState.value.copy(errors = emptyList())
    }
}
