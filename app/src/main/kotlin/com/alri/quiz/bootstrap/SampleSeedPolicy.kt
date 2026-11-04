package com.alri.quiz.bootstrap

import com.alri.quiz.core.model.BankSummary

/**
 * 「内置示例题库该不该写进数据库」的判定逻辑。
 *
 * 单独抽成一个纯函数的原因：这段逻辑的每一条分支都对应一类真实用户，
 * 判错的后果是「删掉了用户自己的题库」或者「老用户升级后还看到旧题库」。
 * 它不碰 Context、不碰协程、不碰数据库，所以能用最快的 JVM 单测把每条分支钉住。
 */
internal object SampleSeedPolicy {

    /** 当前内置示例题库的版本。题库内容换一版就把这个数字加一。 */
    const val CURRENT_SEED_VERSION: Int = 2

    const val SAMPLE_BANK_ID: String = "android-basics-30"

    /** v1.0.0 内置的那份旧示例：id、标题、题量三者都匹配才认。 */
    const val LEGACY_SAMPLE_BANK_ID: String = "demo-basic"
    const val LEGACY_SAMPLE_TITLE: String = "示例题库 · Android 基础"
    const val LEGACY_SAMPLE_QUESTION_COUNT: Int = 14

    /** 启动时的动作。 */
    enum class Action {
        /** 什么都不做：已经处理过，或用户已有自己的题库。 */
        NOTHING,

        /** 导入内置示例（库里是空的，或刚删掉旧版示例）。 */
        IMPORT_SAMPLE,

        /** 删掉 v1.0.0 的旧示例，再导入新示例。 */
        REPLACE_LEGACY_SAMPLE,
    }

    fun decide(banks: List<BankSummary>, recordedSeedVersion: Int): Action {
        // 这一版已经处理过：用户之后删掉示例题库也不会被反复塞回来
        if (recordedSeedVersion >= CURRENT_SEED_VERSION) return Action.NOTHING

        // 新示例已经在库里了（比如用户手动导入过同一份），不覆盖
        if (banks.any { it.id == SAMPLE_BANK_ID }) return Action.NOTHING

        val legacy = banks.filter { it.isLegacySample() }
        if (legacy.isNotEmpty()) return Action.REPLACE_LEGACY_SAMPLE

        // 用户有别的题库、或者主动删过示例：尊重现状
        if (banks.isNotEmpty()) return Action.NOTHING

        return Action.IMPORT_SAMPLE
    }

    /**
     * 判定某个题库是不是 v1.0.0 那份内置示例。
     *
     * 三个条件同时成立才算，任何一个用户自建的题库都不会同时满足——
     * 尤其是题量 14：即便有人 id 撞上，标题和题量也对不上。
     */
    fun BankSummary.isLegacySample(): Boolean =
        id == LEGACY_SAMPLE_BANK_ID &&
            title == LEGACY_SAMPLE_TITLE &&
            questionCount == LEGACY_SAMPLE_QUESTION_COUNT
}
