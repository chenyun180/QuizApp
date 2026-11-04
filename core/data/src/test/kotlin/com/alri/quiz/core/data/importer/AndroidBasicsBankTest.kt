package com.alri.quiz.core.data.importer

import com.alri.quiz.core.model.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 内置题库 `android-basics-30` 的回归测试。
 *
 * 为什么要有这个测试：这份题库是打进 APK 的默认内容，只要有一处不符合契约
 * （题号重复、answers 指向不存在的选项、多选只给了一个答案……），
 * App 首次启动的自动导入就会静默失败，用户看到的是一个空题库。
 * 用工程自己的 [BankJsonParser] 跑一遍，等于把「导入失败」这件事挡在 CI 里。
 *
 * JSON 内容复制在 `src/test/resources/banks/android-basics-30.json`，
 * 和 `app/src/main/assets/samples/sample_bank.json` 保持字节一致，
 * 这样单测不依赖任何绝对路径，换机器照样能跑。
 */
class AndroidBasicsBankTest {

    private val parser = BankJsonParser()

    private fun loadBankJson(): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(BANK_RESOURCE)) {
            "测试资源缺失：$BANK_RESOURCE"
        }.bufferedReader().use { it.readText() }

    private fun parsedBank(): com.alri.quiz.core.model.QuizBank {
        val result = parser.parse(loadBankJson())
        if (result is BankJsonParser.Result.Invalid) {
            throw AssertionError(
                "题库校验未通过，共 ${result.errors.size} 处错误：\n" +
                    result.errors.joinToString("\n") { it.format() },
            )
        }
        return (result as BankJsonParser.Result.Ok).bank
    }

    @Test
    fun `bank parses successfully with 30 questions`() {
        assertEquals(30, parsedBank().questions.size)
    }

    @Test
    fun `bank metadata is the expected one`() {
        val bank = parsedBank()
        assertEquals("android-basics-30", bank.id)
        assertEquals("Android 基础 · 30 题", bank.title)
        assertEquals(1, bank.version)
    }

    @Test
    fun `question type distribution is 16 single 8 multiple 6 true-false`() {
        val byType = parsedBank().questions.groupingBy { it.type }.eachCount()
        assertEquals(16, byType[QuestionType.SINGLE])
        assertEquals(8, byType[QuestionType.MULTIPLE])
        assertEquals(6, byType[QuestionType.TRUE_FALSE])
    }

    @Test
    fun `ids are q1 through q30 without duplicates`() {
        val ids = parsedBank().questions.map { it.id }
        assertEquals((1..30).map { "q$it" }, ids)
    }

    @Test
    fun `every question has an explanation of at least 60 chinese characters`() {
        parsedBank().questions.forEach { question ->
            val explanation = question.explanation
            assertTrue("${question.id} 缺少解析", explanation != null)
            val chineseChars = explanation!!.count { it in '\u4e00'..'\u9fff' }
            assertTrue(
                "${question.id} 解析只有 $chineseChars 个中文字符，要求至少 60",
                chineseChars >= 60,
            )
        }
    }

    @Test
    fun `every question has one or two tags from the fixed vocabulary`() {
        val allowed = setOf(
            "Kotlin 基础", "Activity", "生命周期", "Compose", "Gradle",
            "Room", "协程", "权限", "四大组件", "Intent", "资源", "打包发布",
        )
        parsedBank().questions.forEach { question ->
            assertTrue(
                "${question.id} 标签数量应为 1~2 个，实际 ${question.tags}",
                question.tags.size in 1..2,
            )
            question.tags.forEach { tag ->
                assertTrue("${question.id} 使用了约定外的标签「$tag」", tag in allowed)
            }
        }
    }

    @Test
    fun `choice questions have at least four options and answers point at real options`() {
        parsedBank().questions.forEach { question ->
            if (question.type == QuestionType.TRUE_FALSE) {
                assertEquals("${question.id} 判断题应是默认 T/F 两项", listOf("T", "F"), question.options.map { it.key })
                return@forEach
            }
            assertTrue(
                "${question.id} 选项数应不少于 4，实际 ${question.options.size}",
                question.options.size >= 4,
            )
            assertTrue("${question.id} 答案为空", question.answers.isNotEmpty())
            question.answers.forEach { answer ->
                assertTrue("${question.id} 答案「$answer」不在选项里", answer in question.optionKeys)
            }
            if (question.type == QuestionType.MULTIPLE) {
                assertTrue("${question.id} 多选题答案应至少 2 个", question.answers.size >= 2)
            }
        }
    }

    @Test
    fun `question stems are non-blank and reasonably long`() {
        parsedBank().questions.forEach { question ->
            assertTrue("${question.id} 题干过短", question.stem.trim().length >= 10)
        }
    }

    private companion object {
        const val BANK_RESOURCE = "banks/android-basics-30.json"
    }
}
