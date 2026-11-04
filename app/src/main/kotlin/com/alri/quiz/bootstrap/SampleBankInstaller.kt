package com.alri.quiz.bootstrap

import android.content.Context
import com.alri.quiz.core.data.importer.ImportResult
import com.alri.quiz.core.data.repository.QuizRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 首次启动时导入内置示例题库。
 *
 * 为什么要有这个：一个装好就是白屏、必须自己去电脑上写 JSON 再传手机的 App，
 * 用户第一分钟就走了。内置一份 14 题的示例，打开就能刷，同时它本身就是
 * 「题库 JSON 长什么样」的活样例。
 *
 * 只做一次：用 SharedPreferences 记一个标记。用户删掉示例题库后不会再被强塞回来
 * （标记在删除时不回滚，这是刻意设计）。
 */
@Singleton
class SampleBankInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: QuizRepository,
) {

    fun installIfNeeded() {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SEEDED, false)) return

        // 用独立的 scope 而不是 viewModelScope：这个活儿和任何界面都无关，
        // 界面退出也不能把它打断
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                // 已经有题库（比如用户自己先导入了）就不再塞示例
                if (repository.observeBanks().first().isNotEmpty()) {
                    prefs.edit().putBoolean(KEY_SEEDED, true).apply()
                    return@runCatching
                }
                val json = context.assets.open(SAMPLE_PATH).bufferedReader().use { it.readText() }
                when (val result = repository.importBank(json)) {
                    is ImportResult.Success -> prefs.edit().putBoolean(KEY_SEEDED, true).apply()
                    is ImportResult.Failure -> {
                        // 示例题库坏掉属于打包事故，静默失败，不影响用户自己导入
                        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
                    }
                }
            }
        }
    }

    private companion object {
        const val PREFS = "quiz_app_prefs"
        const val KEY_SEEDED = "sample_bank_seeded"
        const val SAMPLE_PATH = "samples/sample_bank.json"
    }
}
