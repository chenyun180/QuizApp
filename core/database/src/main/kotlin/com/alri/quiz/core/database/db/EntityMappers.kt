package com.alri.quiz.core.database.db

import com.alri.quiz.core.database.entity.BankEntity
import com.alri.quiz.core.database.entity.QuestionEntity
import com.alri.quiz.core.database.entity.QuestionTagEntity
import com.alri.quiz.core.model.BankSummary
import com.alri.quiz.core.model.Option
import com.alri.quiz.core.model.Question
import com.alri.quiz.core.model.QuestionType
import com.alri.quiz.core.model.QuizBank

/**
 * 实体 ↔ 业务模型的转换。
 *
 * 这一层存在的意义：业务层（feature / data）只认识 [Question]，
 * 完全不 import 任何 Room 类。以后换数据库、加缓存都不用改 UI 代码。
 */

fun BankEntity.toSummary(): BankSummary = BankSummary(
    id = bankId,
    title = title,
    questionCount = questionCount,
)

fun QuestionEntity.toDomain(): Question = Question(
    id = questionId,
    type = QuestionType.valueOf(type),
    stem = stem,
    options = options.map { Option(it.key, it.text) },
    answers = answers,
    explanation = explanation,
    tags = tags,
    imageRef = imageRef,
)

fun Question.toEntity(bankId: String, orderIndex: Int): QuestionEntity = QuestionEntity(
    bankId = bankId,
    questionId = id,
    type = type.name,
    stem = stem,
    options = options,
    answers = answers,
    explanation = explanation,
    tags = tags,
    imageRef = imageRef,
    orderIndex = orderIndex,
)

fun Question.toTagEntities(bankId: String): List<QuestionTagEntity> =
    tags.distinct().map { QuestionTagEntity(bankId = bankId, questionId = id, tag = it) }

/** 整库模型 → 三张表的行，供导入事务一次性写入。 */
fun QuizBank.toRows(importedAt: Long): Triple<BankEntity, List<QuestionEntity>, List<QuestionTagEntity>> {
    val questionEntities = questions.mapIndexed { index, question -> question.toEntity(id, index) }
    val tagEntities = questions.flatMap { it.toTagEntities(id) }
    val bankEntity = BankEntity(
        bankId = id,
        title = title,
        version = version,
        questionCount = questions.size,
        importedAt = importedAt,
    )
    return Triple(bankEntity, questionEntities, tagEntities)
}
