package com.alri.quiz.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 题目表。
 *
 * 主键是复合主键 (bank_id, question_id)：题目 id 只在题库内唯一，
 * 不同题库里出现同名的 "q1" 是合法的。
 *
 * [options]、[answers]、[tags] 用 JSON / 集合形式存，由
 * [com.alri.quiz.core.database.db.QuizTypeConverters] 负责和数据库里的 TEXT 互转——
 * 不用建三张子表，是因为它们只作为整体读取，从不单独查询。
 * 唯一需要按值查询的「标签」单独立了 [QuestionTagEntity] 表，才有索引可用。
 */
@Entity(
    tableName = "questions",
    primaryKeys = ["bank_id", "question_id"],
    foreignKeys = [
        ForeignKey(
            entity = BankEntity::class,
            parentColumns = ["bank_id"],
            childColumns = ["bank_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bank_id")],
)
data class QuestionEntity(
    @ColumnInfo(name = "bank_id")
    val bankId: String,
    @ColumnInfo(name = "question_id")
    val questionId: String,
    /** 存 QuestionType 的 name：SINGLE / MULTIPLE / TRUE_FALSE。 */
    val type: String,
    val stem: String,
    val options: List<com.alri.quiz.core.model.Option>,
    val answers: Set<String>,
    val explanation: String?,
    val tags: List<String>,
    @ColumnInfo(name = "image_ref")
    val imageRef: String?,
    /** 在题库 JSON 里的顺序，保证刷题顺序稳定（不依赖插入顺序）。 */
    @ColumnInfo(name = "order_index")
    val orderIndex: Int,
)
