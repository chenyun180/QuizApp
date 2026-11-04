package com.alri.quiz.core.database.db

import androidx.room.TypeConverter
import com.alri.quiz.core.model.Option
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Room 的列类型转换器。
 *
 * - 选项列表用 JSON 存：结构是嵌套对象，塞进一个 TEXT 列最省事，且只整体读取
 * - 答案集合、标签数组用 \u001F（ASCII Unit Separator）拼接：内容简单、数据库里可读，
 *   不用逗号是因为选项 key 和标签文字里都可能出现逗号
 */
class QuizTypeConverters {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun optionsToString(options: List<Option>): String =
        json.encodeToString(OptionListDto.serializer(), OptionListDto(options.map { OptionDto(it.key, it.text) }))

    @TypeConverter
    fun stringToOptions(raw: String): List<Option> {
        if (raw.isBlank()) return emptyList()
        return json.decodeFromString(OptionListDto.serializer(), raw).items.map { Option(it.key, it.text) }
    }

    @TypeConverter
    fun stringSetToString(value: Set<String>): String = value.joinToString(SEPARATOR)

    @TypeConverter
    fun stringToStringSet(raw: String): Set<String> =
        if (raw.isEmpty()) emptySet() else LinkedHashSet(raw.split(SEPARATOR))

    @TypeConverter
    fun stringListToString(value: List<String>): String = value.joinToString(SEPARATOR)

    @TypeConverter
    fun stringToStringList(raw: String): List<String> =
        if (raw.isEmpty()) emptyList() else raw.split(SEPARATOR)

    /** 纯数据库内部用的序列化外壳，不让 Room 的存储格式泄漏到业务模型里。 */
    @Serializable
    private data class OptionListDto(val items: List<OptionDto>)

    @Serializable
    private data class OptionDto(val key: String, val text: String)

    private companion object {
        const val SEPARATOR = "\u001F"
    }
}
