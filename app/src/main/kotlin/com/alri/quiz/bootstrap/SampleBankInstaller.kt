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
 * 用户第一分钟就走了。内置一份 30 题的示例，打开就能刷，同时它本身就是
 * 「题库 JSON 长什么样」的活样例。
 *
 * 该不该写、写什么，全部交给 [SampleSeedPolicy] 决定（纯函数，有单测覆盖）。
 * 这里只负责三件事：读标记、执行动作、把标记推进到一个新的版本号。
 * 标记一旦推进，用户之后删掉示例题库就不会再被反复塞回来。
 */
@Singleton
class SampleBankInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: QuizRepository,
) {

    fun installIfNeeded() {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // 老版本只写过一个布尔标记，这里读不到版本号时按 0 处理，正好走一次升级检查
        val recorded = prefs.getInt(KEY_SEED_VERSION, 0)
        if (recorded >= SampleSeedPolicy.CURRENT_SEED_VERSION) return

        // 用独立的 scope 而不是 viewModelScope：这个活儿和任何界面都无关，
        // 界面退出也不能把它打断
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                val banks = repository.observeBanks().first()
                when (SampleSeedPolicy.decide(banks, recorded)) {
                    SampleSeedPolicy.Action.NOTHING -> Unit

                    SampleSeedPolicy.Action.IMPORT_SAMPLE -> importSample()

                    SampleSeedPolicy.Action.REPLACE_LEGACY_SAMPLE -> {
                        // 只删 id + 标题 + 题量三者都匹配的那一个，用户自建题库碰不到
                        banks.filter { SampleSeedPolicy.run { it.isLegacySample() } }
                            .forEach { repository.deleteBank(it.id) }
                        importSample()
                    }
                }
                // 无论走哪个分支都算「这一版已处理过」，避免每次启动重复检查
                prefs.edit().putInt(KEY_SEED_VERSION, SampleSeedPolicy.CURRENT_SEED_VERSION).apply()
            }
        }
    }

    private suspend fun importSample() {
        val json = context.assets.open(SAMPLE_PATH).bufferedReader().use { it.readText() }
        when (repository.importBank(json)) {
            // 示例题库坏掉属于打包事故，运行时静默失败不影响用户自己导入；
            // 这种事故由 core:data 的 AndroidBasicsBankTest 在测试阶段先兜住。
            is ImportResult.Success, is ImportResult.Failure -> Unit
        }
    }

    private companion object {
        const val PREFS = "quiz_app_prefs"
        const val KEY_SEED_VERSION = "sample_bank_seed_version"
        const val SAMPLE_PATH = "samples/sample_bank.json"
    }
}
