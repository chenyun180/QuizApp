package com.alri.quiz.core.model

/** 一个题库（一份可以导入的题集）。 */
public data class QuizBank(
    val id: String,
    val title: String,
    val version: Int = 1,
    val questions: List<Question> = emptyList(),
)

/** 题库列表页用的轻量投影，不带题目内容，避免把整库题目读进内存。 */
public data class BankSummary(
    val id: String,
    val title: String,
    val questionCount: Int,
)
