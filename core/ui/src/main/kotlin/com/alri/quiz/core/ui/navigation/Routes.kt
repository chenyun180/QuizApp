package com.alri.quiz.core.ui.navigation

import android.net.Uri

/**
 * 全部页面路由。集中在一处，任何模块改路由名都只需要动这里。
 *
 * 为什么用字符串而不是 Navigation 2.8 的类型安全路由：类型安全路由要求参数是
 * `@Serializable` 数据类并由序列化插件生成，而本工程已经用 kotlinx.serialization
 * 处理题库 JSON，两者混用会让阅读路由代码的人分不清哪套是给谁用的。
 * 字符串路由在这里参数很少，收益大于成本。
 */
object Routes {

    const val BANKS = "banks"
    const val SETTINGS = "settings"
    const val IMPORT = "import"
    /** 错题本 / 收藏的列表页，两者共用一个路由，靠 mode 区分。 */
    const val QUIZ_LIST = "quiz_list?mode={mode}"

    fun quizList(mode: QuizMode): String = "quiz_list?mode=${mode.name}"

    /** 刷题页。mode/bankId/tag 三个可选参数，用空串表示"没有"。 */
    const val QUIZ = "quiz?mode={mode}&bankId={bankId}&tag={tag}"

    fun quiz(mode: QuizMode, bankId: String? = null, tag: String? = null): String {
        val bank = bankId?.let { Uri.encode(it) }.orEmpty()
        val tagPart = tag?.let { Uri.encode(it) }.orEmpty()
        return "quiz?mode=${mode.name}&bankId=$bank&tag=$tagPart"
    }
}

/** 练习模式，决定题目从哪来。 */
enum class QuizMode {
    /** 某个题库的全部题目 */
    BANK,

    /** 某个题库里某个标签下的题目 */
    TAG,

    /** 某个题库随机抽 20 题 */
    RANDOM,

    /** 跨题库的错题 */
    WRONG,

    /** 跨题库的收藏 */
    FAVORITE,
}
