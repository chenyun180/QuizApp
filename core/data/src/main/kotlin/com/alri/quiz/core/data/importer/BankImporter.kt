package com.alri.quiz.core.data.importer

import android.content.ContentResolver
import android.net.Uri
import com.alri.quiz.core.data.repository.QuizRepository
import java.io.IOException
import javax.inject.Inject

/**
 * 把「用户从文件选择器选中的 URI」变成一次导入。
 *
 * 只依赖 [ContentResolver] 而不是 Activity/Fragment：SAF 返回的 URI 权限是临时的，
 * 所以要在这个调用栈里立刻读完，不能在后面异步再读，否则会 SecurityException。
 */
class BankImporter @Inject constructor(
    private val repository: QuizRepository,
) {

    /** 直接导入一段 JSON 文本（预览/测试用）。 */
    suspend fun importFromJson(rawJson: String): ImportResult = repository.importBank(rawJson)

    /**
     * 从 content:// URI 读文件并导入。
     *
     * 文件大小上限 8 MiB：题库 JSON 再大也不现实，限制一下可以避免用户误选大文件把内存打爆。
     */
    suspend fun importFromUri(resolver: ContentResolver, uri: Uri): ImportResult {
        val text = try {
            readAll(resolver, uri)?.toString(Charsets.UTF_8)
        } catch (e: IOException) {
            return ImportResult.Failure(
                listOf(ImportError(null, null, "无法读取文件：${e.message ?: "IO 错误"}")),
            )
        } catch (e: SecurityException) {
            return ImportResult.Failure(
                listOf(ImportError(null, null, "没有读取该文件的权限，请重新选择一次")),
            )
        }

        if (text == null) {
            return ImportResult.Failure(
                listOf(ImportError(null, null, "无法打开这个文件（可能是网盘/虚拟文件，请先下载到本机）")),
            )
        }
        return repository.importBank(text)
    }

    private fun readAll(resolver: ContentResolver, uri: Uri): ByteArray? {
        val stream = resolver.openInputStream(uri) ?: return null
        stream.use { input ->
            val buffer = java.io.ByteArrayOutputStream()
            val chunk = ByteArray(16 * 1024)
            var total = 0L
            while (true) {
                val read = input.read(chunk)
                if (read < 0) break
                total += read
                if (total > MAX_BYTES) {
                    throw IOException("文件超过 ${MAX_BYTES / 1024 / 1024} MiB 上限")
                }
                buffer.write(chunk, 0, read)
            }
            return buffer.toByteArray()
        }
    }

    private companion object {
        const val MAX_BYTES = 8L * 1024 * 1024
    }
}
