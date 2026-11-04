package com.alri.quiz.core.data.importer

/** 一条可以定位到「哪一题、哪个字段」的导入错误。 */
data class ImportError(
    /** 题目在 JSON 数组里的下标，从 0 开始；题库级错误为 null。 */
    val questionIndex: Int?,
    /** 出错字段，如 "options"、"answers"。 */
    val field: String?,
    val message: String,
) {
    fun format(): String = buildString {
        if (questionIndex != null) append("[第 ${questionIndex + 1} 题")
        if (field != null) {
            if (questionIndex != null) append(" · ") else append("[")
            append(field)
        }
        if (questionIndex != null || field != null) append("] ")
        append(message)
    }
}

/** 导入结果。要么全部成功，要么带着完整错误清单失败——不做"部分导入"。 */
sealed interface ImportResult {
    /**
     * @param bankTitle 题库标题，用于提示「导入了哪个库」
     * @param questionCount 本次导入的题目数
     * @param replaced true 表示覆盖了一个同 id 的旧题库
     */
    data class Success(
        val bankTitle: String,
        val questionCount: Int,
        val replaced: Boolean,
    ) : ImportResult

    data class Failure(val errors: List<ImportError>) : ImportResult
}
