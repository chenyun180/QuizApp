package com.alri.quiz.core.data.importer

import com.alri.quiz.core.model.Option
import com.alri.quiz.core.model.Question
import com.alri.quiz.core.model.QuestionType
import com.alri.quiz.core.model.QuizBank
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * 题库 JSON → [QuizBank] 的解析器。
 *
 * 设计原则：**要么全部合法，要么一份完整错误清单**。绝不静默丢题、绝不"跳过坏数据继续导入"——
 * 学习场景里，少一道题用户根本发现不了，但考试会做错。
 *
 * 纯 JVM 实现（不碰 Android），所以可以在 `src/test` 里直接跑。
 */
class BankJsonParser {

    private val json = Json {
        // 严格模式：拼错的字段名（比如 "stemm"）直接报错，而不是被默默忽略
        ignoreUnknownKeys = false
        isLenient = false
        allowTrailingComma = false
        prettyPrint = false
    }

    /** 解析结果。 */
    sealed interface Result {
        data class Ok(val bank: QuizBank) : Result
        data class Invalid(val errors: List<ImportError>) : Result
    }

    fun parse(rawJson: String): Result {
        if (rawJson.isBlank()) {
            return Result.Invalid(listOf(ImportError(null, null, "文件内容为空")))
        }

        val dto = try {
            json.decodeFromString(BankJsonDto.serializer(), rawJson)
        } catch (e: SerializationException) {
            return Result.Invalid(listOf(ImportError(null, null, "JSON 格式错误：${e.readableMessage()}")))
        } catch (e: IllegalArgumentException) {
            return Result.Invalid(listOf(ImportError(null, null, "JSON 格式错误：${e.message ?: "无法解析"}")))
        }

        val errors = mutableListOf<ImportError>()

        // ---- 题库级校验 ----
        if (dto.schemaVersion != BankJsonDto.SCHEMA_VERSION) {
            errors += ImportError(
                null,
                "schemaVersion",
                "不支持的 schemaVersion=${dto.schemaVersion}，当前只支持 ${BankJsonDto.SCHEMA_VERSION}",
            )
        }

        val bankId = dto.bankId?.trim().orEmpty()
        when {
            bankId.isEmpty() -> errors += ImportError(null, "bankId", "bankId 不能为空")
            !BANK_ID_PATTERN.matches(bankId) ->
                errors += ImportError(null, "bankId", "bankId 只能包含字母、数字、点、下划线、短横线，实际是「$bankId」")
        }

        val title = dto.title?.trim().orEmpty()
        if (title.isEmpty()) errors += ImportError(null, "title", "title 不能为空")

        if (dto.questions.isEmpty()) errors += ImportError(null, "questions", "questions 不能为空数组")

        // ---- 题目级校验 ----
        val questions = mutableListOf<Question>()
        val seenIds = mutableMapOf<String, Int>()

        dto.questions.forEachIndexed { index, item ->
            val questionErrors = mutableListOf<ImportError>()

            val id = item.id?.trim().orEmpty()
            if (id.isEmpty()) {
                questionErrors += ImportError(index, "id", "id 不能为空")
            } else {
                val previous = seenIds.put(id, index)
                if (previous != null) {
                    questionErrors += ImportError(index, "id", "题目 id「$id」与第 ${previous + 1} 题重复")
                }
            }

            val type = parseType(item.type)
            if (type == null) {
                questionErrors += ImportError(
                    index,
                    "type",
                    "type 必须是 single / multiple / true_false 之一，实际是「${item.type ?: "缺失"}」",
                )
            }

            val stem = item.stem?.trim().orEmpty()
            if (stem.isEmpty()) questionErrors += ImportError(index, "stem", "stem 不能为空")

            // 选项：判断题允许省略 options，自动补 正确/错误 两个选项
            val options = resolveOptions(index, type, item.options, questionErrors)
            val optionKeys = options.mapTo(LinkedHashSet()) { it.key }

            // 答案：判断题用默认 T/F 选项时，允许写 true/false、正确/错误 等别名
            val rawAnswers = item.answers.map { it.trim() }.filter { it.isNotEmpty() }
            val answers = if (type == QuestionType.TRUE_FALSE && item.options.isEmpty()) {
                rawAnswers.map { TRUE_FALSE_ALIASES[it.lowercase()] ?: it }
            } else {
                rawAnswers
            }
            when {
                answers.isEmpty() ->
                    questionErrors += ImportError(index, "answers", "answers 不能为空，至少要有 1 个正确选项")
                answers.size != answers.toSet().size ->
                    questionErrors += ImportError(index, "answers", "answers 里有重复项：$answers")
                else -> {
                    val unknown = answers.filterNot { it in optionKeys }
                    if (unknown.isNotEmpty()) {
                        questionErrors += ImportError(
                            index,
                            "answers",
                            "answers 引用了不存在的选项 $unknown（本题选项：$optionKeys）",
                        )
                    }
                    if (type == QuestionType.SINGLE || type == QuestionType.TRUE_FALSE) {
                        if (answers.size > 1) {
                            questionErrors += ImportError(
                                index,
                                "answers",
                                "题型 $type 只能有 1 个答案，实际给了 ${answers.size} 个：$answers（多选请把 type 改成 multiple）",
                            )
                        }
                    }
                    if (type == QuestionType.MULTIPLE && answers.size == 1) {
                        questionErrors += ImportError(
                            index,
                            "answers",
                            "题型 multiple 只有 1 个答案，请改成 type=single（或补全答案）",
                        )
                    }
                }
            }

            val tags = item.tags.map { it.trim() }
            if (tags.any { it.isEmpty() }) {
                questionErrors += ImportError(index, "tags", "tags 里不能有空字符串")
            }

            val image = item.image?.trim()?.takeIf { it.isNotEmpty() }

            if (questionErrors.isEmpty()) {
                questions += Question(
                    id = id,
                    type = requireNotNull(type),
                    stem = stem,
                    options = options,
                    answers = answers.toSet(),
                    explanation = item.explanation?.trim()?.takeIf { it.isNotEmpty() },
                    tags = tags.distinct(),
                    imageRef = image,
                )
            }
            errors += questionErrors
        }

        if (errors.isNotEmpty()) return Result.Invalid(errors)

        return Result.Ok(
            QuizBank(
                id = bankId,
                title = title,
                version = dto.version,
                questions = questions,
            ),
        )
    }

    private fun parseType(raw: String?): QuestionType? = when (raw?.trim()?.lowercase()) {
        "single" -> QuestionType.SINGLE
        "multiple" -> QuestionType.MULTIPLE
        "true_false" -> QuestionType.TRUE_FALSE
        else -> null
    }

    private fun resolveOptions(
        index: Int,
        type: QuestionType?,
        rawOptions: List<OptionJsonDto>,
        errors: MutableList<ImportError>,
    ): List<Option> {
        if (rawOptions.isEmpty()) {
            if (type == QuestionType.TRUE_FALSE) {
                // 判断题不写选项是常见写法，这里给出默认的 T/F 两项
                return TRUE_FALSE_OPTIONS
            }
            errors += ImportError(index, "options", "options 不能为空，至少要有 2 个选项")
            return emptyList()
        }

        if (rawOptions.size < 2) {
            errors += ImportError(index, "options", "options 至少有 2 个选项，实际只有 ${rawOptions.size} 个")
        }

        val result = mutableListOf<Option>()
        val seenKeys = mutableSetOf<String>()
        rawOptions.forEachIndexed { optionIndex, option ->
            val key = option.key?.trim().orEmpty()
            val text = option.text?.trim().orEmpty()
            if (key.isEmpty()) {
                errors += ImportError(index, "options[$optionIndex].key", "选项 key 不能为空")
            } else if (!seenKeys.add(key)) {
                errors += ImportError(index, "options[$optionIndex].key", "选项 key「$key」重复")
            }
            if (text.isEmpty()) {
                errors += ImportError(index, "options[$optionIndex].text", "选项文字不能为空")
            }
            if (key.isNotEmpty() && text.isNotEmpty()) {
                result += Option(key = key, text = text)
            }
        }
        return result
    }

    private fun SerializationException.readableMessage(): String {
        val text = message.orEmpty()
        // kotlinx 的异常信息已经带 JSON 路径（如 $.questions[2].stem），原样透出最有用
        return text.substringAfter("Exception:", text).trim().ifEmpty { "无法解析 JSON" }
    }

    private companion object {
        val BANK_ID_PATTERN = Regex("^[A-Za-z0-9._-]+$")
        val TRUE_FALSE_OPTIONS = listOf(Option("T", "正确"), Option("F", "错误"))
        val TRUE_FALSE_ALIASES = mapOf(
            "t" to "T", "true" to "T", "1" to "T", "正确" to "T", "对" to "T", "是" to "T",
            "f" to "F", "false" to "F", "0" to "F", "错误" to "F", "错" to "F", "否" to "F",
        )
    }
}
