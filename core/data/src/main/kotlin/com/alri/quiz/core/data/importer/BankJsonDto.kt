package com.alri.quiz.core.data.importer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 题库 JSON 的**传输对象**（DTO）。刻意和业务模型 [com.alri.quiz.core.model.QuizBank] 分开：
 *
 * - JSON 的字段是可选的、类型是宽松的（比如 "single" 字符串），解析时任何问题都要能报出具体位置
 * - 业务模型是强类型的，构造出来就一定是合法的
 *
 * 直接把 @Serializable 标在业务模型上会诱使大家"能解析就算合法"，那是 bug 的温床。
 */
@Serializable
internal data class BankJsonDto(
    val schemaVersion: Int = SCHEMA_VERSION,
    val bankId: String? = null,
    val title: String? = null,
    val version: Int = 1,
    val questions: List<QuestionJsonDto> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION: Int = 1
    }
}

@Serializable
internal data class QuestionJsonDto(
    val id: String? = null,
    val type: String? = null,
    val stem: String? = null,
    val options: List<OptionJsonDto> = emptyList(),
    val answers: List<String> = emptyList(),
    val explanation: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("image") val image: String? = null,
)

@Serializable
internal data class OptionJsonDto(
    val key: String? = null,
    val text: String? = null,
)
