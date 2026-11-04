package com.alri.quiz.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 答题记录：每答一次插一行，是「历史」。错题本、正确率统计都从它算。
 *
 * 刻意不做 (bank_id, question_id) 唯一：同一题可能答错好几次，
 * 统计需要知道每一次。要不要只看最近一次，由查询决定。
 */
@Entity(
    tableName = "answer_records",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["bank_id", "question_id"],
            childColumns = ["bank_id", "question_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bank_id", "question_id"]),
        Index("is_correct"),
        Index("answered_at"),
    ],
)
data class AnswerRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "bank_id")
    val bankId: String,
    @ColumnInfo(name = "question_id")
    val questionId: String,
    /** 当次选中的选项 key 集合（多选可能多个）。 */
    val selected: Set<String>,
    @ColumnInfo(name = "is_correct")
    val isCorrect: Boolean,
    @ColumnInfo(name = "answered_at")
    val answeredAt: Long,
)
