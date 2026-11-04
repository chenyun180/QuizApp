package com.alri.quiz.core.model

/**
 * 题型。
 *
 * - [SINGLE] 单选：答案是唯一一个选项
 * - [MULTIPLE] 多选：答案是一个集合，必须完全相等才算对
 * - [TRUE_FALSE] 判断：本质上也是单选的二选一，单独分出来是为了 UI 和统计
 */
public enum class QuestionType {
    SINGLE,
    MULTIPLE,
    TRUE_FALSE,
}

/** 一个选项。[key] 是题库里的稳定标识（A/B/C…），[text] 是展示给用户的文字。 */
public data class Option(
    val key: String,
    val text: String,
)

/**
 * 一道题。
 *
 * 注意 [answers] 用 [Set] 而不是 [List]：多选判分是集合相等，
 * 用 List 会误导人以为顺序有意义。
 */
public data class Question(
    val id: String,
    val type: QuestionType,
    val stem: String,
    val options: List<Option>,
    val answers: Set<String>,
    val explanation: String? = null,
    val tags: List<String> = emptyList(),
    val imageRef: String? = null,
) {
    /** 所有合法选项 key。 */
    public val optionKeys: Set<String> get() = options.mapTo(LinkedHashSet()) { it.key }

    /** 按 key 找选项文字，找不到返回 null。 */
    public fun optionText(key: String): String? = options.firstOrNull { it.key == key }?.text
}
