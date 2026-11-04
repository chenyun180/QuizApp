package com.alri.quiz.core.data.importer

import com.alri.quiz.core.model.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BankJsonParserTest {

    private val parser = BankJsonParser()

    private fun ok(json: String) = (parser.parse(json) as BankJsonParser.Result.Ok).bank

    private fun errors(json: String) =
        (parser.parse(json) as BankJsonParser.Result.Invalid).errors

    private fun wrap(questions: String, bankId: String = "bank-1", title: String = "测试题库") = """
        {
          "schemaVersion": 1,
          "bankId": "$bankId",
          "title": "$title",
          "questions": $questions
        }
    """.trimIndent()

    // ---------------- 成功路径 ----------------

    @Test
    fun `parses a valid bank with all three question types`() {
        val bank = ok(
            wrap(
                """
                [
                  {"id":"q1","type":"single","stem":"题干1","options":[{"key":"A","text":"甲"},{"key":"B","text":"乙"}],"answers":["A"],"explanation":"因为甲","tags":["基础"]},
                  {"id":"q2","type":"multiple","stem":"题干2","options":[{"key":"A","text":"甲"},{"key":"B","text":"乙"},{"key":"C","text":"丙"}],"answers":["A","C"],"tags":["进阶","基础"]},
                  {"id":"q3","type":"true_false","stem":"题干3","answers":["T"]}
                ]
                """,
            ),
        )

        assertEquals("bank-1", bank.id)
        assertEquals("测试题库", bank.title)
        assertEquals(3, bank.questions.size)
        assertEquals(QuestionType.SINGLE, bank.questions[0].type)
        assertEquals(QuestionType.MULTIPLE, bank.questions[1].type)
        assertEquals(QuestionType.TRUE_FALSE, bank.questions[2].type)
        assertEquals(setOf("A", "C"), bank.questions[1].answers)
        assertEquals(listOf("进阶", "基础"), bank.questions[1].tags)
        assertEquals("因为甲", bank.questions[0].explanation)
    }

    @Test
    fun `true_false without options gets default T F options`() {
        val bank = ok(wrap("""[{"id":"q1","type":"true_false","stem":"对还是错","answers":["T"]}]"""))
        assertEquals(listOf("T", "F"), bank.questions[0].options.map { it.key })
        assertEquals(setOf("T"), bank.questions[0].answers)
    }

    @Test
    fun `true_false accepts alias answers like true and 正确`() {
        val bank = ok(
            wrap(
                """
                [
                  {"id":"q1","type":"true_false","stem":"a","answers":["true"]},
                  {"id":"q2","type":"true_false","stem":"b","answers":["错误"]}
                ]
                """,
            ),
        )
        assertEquals(setOf("T"), bank.questions[0].answers)
        assertEquals(setOf("F"), bank.questions[1].answers)
    }

    @Test
    fun `duplicate tags are de-duplicated and blank explanation becomes null`() {
        val bank = ok(
            wrap("""[{"id":"q1","type":"single","stem":"s","options":[{"key":"A","text":"a"},{"key":"B","text":"b"}],"answers":["A"],"tags":["x","x"],"explanation":"   "}]"""),
        )
        assertEquals(listOf("x"), bank.questions[0].tags)
        assertEquals(null, bank.questions[0].explanation)
    }

    // ---------------- 失败路径 ----------------

    @Test
    fun `blank input is rejected`() {
        assertTrue(errors("   ").single().message.contains("为空"))
    }

    @Test
    fun `malformed json is rejected with a message`() {
        val errs = errors("""{"bankId": "x", "questions": [ }""")
        assertTrue(errs.isNotEmpty())
        assertTrue(errs.first().message.contains("JSON 格式错误"))
    }

    @Test
    fun `unknown field is rejected instead of silently ignored`() {
        val errs = errors(
            wrap("""[{"id":"q1","type":"single","stemm":"写了错字","options":[{"key":"A","text":"a"},{"key":"B","text":"b"}],"answers":["A"]}]"""),
        )
        assertTrue(errs.first().message.contains("JSON 格式错误"))
    }

    @Test
    fun `missing bank id is rejected`() {
        val errs = errors("""{"schemaVersion":1,"title":"t","questions":[]}""")
        assertTrue(errs.any { it.field == "bankId" })
    }

    @Test
    fun `unsupported schema version is rejected`() {
        val errs = errors("""{"schemaVersion":99,"bankId":"b","title":"t","questions":[]}""")
        assertTrue(errs.any { it.field == "schemaVersion" })
    }

    @Test
    fun `empty questions array is rejected`() {
        val errs = errors(wrap("[]"))
        assertTrue(errs.any { it.field == "questions" })
    }

    @Test
    fun `answer referencing a missing option is rejected and locates the question`() {
        val errs = errors(
            wrap(
                """
                [
                  {"id":"q1","type":"single","stem":"ok","options":[{"key":"A","text":"a"},{"key":"B","text":"b"}],"answers":["A"]},
                  {"id":"q2","type":"single","stem":"坏","options":[{"key":"A","text":"a"},{"key":"B","text":"b"}],"answers":["C"]}
                ]
                """,
            ),
        )
        val err = errs.single()
        assertEquals(1, err.questionIndex)
        assertEquals("answers", err.field)
        assertTrue(err.message.contains("C"))
        assertTrue(err.format().startsWith("[第 2 题"))
    }

    @Test
    fun `empty answers is rejected`() {
        val errs = errors(
            wrap("""[{"id":"q1","type":"single","stem":"s","options":[{"key":"A","text":"a"},{"key":"B","text":"b"}],"answers":[]}]"""),
        )
        assertTrue(errs.any { it.field == "answers" })
    }

    @Test
    fun `duplicate question ids are rejected`() {
        val errs = errors(
            wrap(
                """
                [
                  {"id":"dup","type":"single","stem":"a","options":[{"key":"A","text":"a"},{"key":"B","text":"b"}],"answers":["A"]},
                  {"id":"dup","type":"single","stem":"b","options":[{"key":"A","text":"a"},{"key":"B","text":"b"}],"answers":["A"]}
                ]
                """,
            ),
        )
        assertTrue(errs.any { it.questionIndex == 1 && it.message.contains("重复") })
    }

    @Test
    fun `unknown type is rejected`() {
        val errs = errors(
            wrap("""[{"id":"q1","type":"essay","stem":"s","options":[{"key":"A","text":"a"},{"key":"B","text":"b"}],"answers":["A"]}]"""),
        )
        assertTrue(errs.any { it.field == "type" })
    }

    @Test
    fun `single choice with two answers is rejected`() {
        val errs = errors(
            wrap("""[{"id":"q1","type":"single","stem":"s","options":[{"key":"A","text":"a"},{"key":"B","text":"b"}],"answers":["A","B"]}]"""),
        )
        assertTrue(errs.any { it.field == "answers" && it.message.contains("只能有 1 个答案") })
    }

    @Test
    fun `multiple choice with one answer is rejected`() {
        val errs = errors(
            wrap("""[{"id":"q1","type":"multiple","stem":"s","options":[{"key":"A","text":"a"},{"key":"B","text":"b"}],"answers":["A"]}]"""),
        )
        assertTrue(errs.any { it.field == "answers" && it.message.contains("multiple") })
    }

    @Test
    fun `duplicate option keys are rejected`() {
        val errs = errors(
            wrap("""[{"id":"q1","type":"single","stem":"s","options":[{"key":"A","text":"a"},{"key":"A","text":"b"}],"answers":["A"]}]"""),
        )
        assertTrue(errs.any { it.field?.contains("key") == true })
    }

    @Test
    fun `option with blank text is rejected`() {
        val errs = errors(
            wrap("""[{"id":"q1","type":"single","stem":"s","options":[{"key":"A","text":"  "},{"key":"B","text":"b"}],"answers":["A"]}]"""),
        )
        assertTrue(errs.any { it.field?.contains("text") == true })
    }

    @Test
    fun `fewer than two options is rejected`() {
        val errs = errors(
            wrap("""[{"id":"q1","type":"single","stem":"s","options":[{"key":"A","text":"a"}],"answers":["A"]}]"""),
        )
        assertTrue(errs.any { it.field == "options" })
    }

    @Test
    fun `all errors are collected at once not just the first`() {
        val errs = errors(
            wrap(
                """
                [
                  {"id":"","type":"nope","stem":"","options":[],"answers":[]},
                  {"id":"q2","type":"single","stem":"s","options":[{"key":"A","text":"a"},{"key":"B","text":"b"}],"answers":["Z"]}
                ]
                """,
            ),
        )
        assertTrue("actual=${errs.size}", errs.size >= 6)
    }

    @Test
    fun `invalid bank id characters are rejected`() {
        val errs = errors(wrap("[]", bankId = "有 空格"))
        assertTrue(errs.any { it.field == "bankId" })
    }
}
