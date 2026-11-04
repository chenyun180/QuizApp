package com.alri.quiz.core.data.repository

import com.alri.quiz.core.model.Question

/**
 * 带来源的题目。
 *
 * 为什么需要它：错题本和收藏是**跨题库**的，而答题记录必须写回题目所属的题库。
 * 只传 [Question] 会丢掉 bankId，跨库场景下就记错账了。
 */
data class SourcedQuestion(
    val bankId: String,
    val question: Question,
)
