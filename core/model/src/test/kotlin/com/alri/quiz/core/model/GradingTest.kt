package com.alri.quiz.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GradingTest {

    private fun single() = Question(
        id = "s1",
        type = QuestionType.SINGLE,
        stem = "Kotlin 里声明只读变量用哪个关键字？",
        options = listOf(Option("A", "var"), Option("B", "val"), Option("C", "let")),
        answers = setOf("B"),
    )

    private fun multiple() = Question(
        id = "m1",
        type = QuestionType.MULTIPLE,
        stem = "下列哪些是 Android 的四大组件？",
        options = listOf(
            Option("A", "Activity"),
            Option("B", "Service"),
            Option("C", "Gradle"),
            Option("D", "BroadcastReceiver"),
        ),
        answers = setOf("A", "B", "D"),
    )

    private fun trueFalse() = Question(
        id = "t1",
        type = QuestionType.TRUE_FALSE,
        stem = "Room 是 Google 官方提供的 SQLite 封装库。",
        options = listOf(Option("T", "正确"), Option("F", "错误")),
        answers = setOf("T"),
    )

    @Test
    fun `single choice - correct answer passes`() {
        assertTrue(Grading.grade(single(), setOf("B")).isCorrect)
    }

    @Test
    fun `single choice - wrong answer fails`() {
        assertFalse(Grading.grade(single(), setOf("A")).isCorrect)
    }

    @Test
    fun `single choice - selecting two options fails`() {
        // 单选却提交两个，必须判错而不是"包含正确答案就算对"
        assertFalse(Grading.grade(single(), setOf("B", "C")).isCorrect)
    }

    @Test
    fun `single choice - empty selection fails`() {
        assertFalse(Grading.grade(single(), emptySet()).isCorrect)
    }

    @Test
    fun `multiple choice - exact set passes regardless of insertion order`() {
        assertTrue(Grading.grade(multiple(), linkedSetOf("D", "A", "B")).isCorrect)
    }

    @Test
    fun `multiple choice - missing one option fails`() {
        assertFalse(Grading.grade(multiple(), setOf("A", "B")).isCorrect)
    }

    @Test
    fun `multiple choice - extra option fails`() {
        assertFalse(Grading.grade(multiple(), setOf("A", "B", "C", "D")).isCorrect)
    }

    @Test
    fun `multiple choice - empty selection fails`() {
        assertFalse(Grading.grade(multiple(), emptySet()).isCorrect)
    }

    @Test
    fun `true false - correct`() {
        assertTrue(Grading.grade(trueFalse(), setOf("T")).isCorrect)
    }

    @Test
    fun `true false - wrong`() {
        assertFalse(Grading.grade(trueFalse(), setOf("F")).isCorrect)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown option key is rejected loudly`() {
        Grading.grade(single(), setOf("Z"))
    }

    @Test
    fun `grade result carries answer details for the explanation panel`() {
        val result = Grading.grade(multiple(), setOf("A"))
        assertEquals("m1", result.questionId)
        assertEquals(setOf("A"), result.selected)
        assertEquals(setOf("A", "B", "D"), result.correctAnswers)
        assertFalse(result.isCorrect)
    }

    @Test
    fun `accuracy computes ratio`() {
        assertEquals(0.75, Grading.accuracy(3, 4), 1e-9)
    }

    @Test
    fun `accuracy of zero total is zero not NaN`() {
        assertEquals(0.0, Grading.accuracy(0, 0), 1e-9)
    }

    @Test
    fun `format accuracy renders percent`() {
        assertEquals("75%", Grading.formatAccuracy(3, 4))
        assertEquals("--", Grading.formatAccuracy(0, 0))
        assertEquals("100%", Grading.formatAccuracy(5, 5))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative counts are rejected`() {
        Grading.accuracy(-1, 4)
    }
}
