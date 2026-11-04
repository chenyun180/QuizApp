package com.alri.quiz.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 题目-标签 关联表。把标签摊平成行，才能用索引做「按标签筛题」，
 * 而不是对 JSON 字符串做 LIKE 全表扫描。
 */
@Entity(
    tableName = "question_tags",
    primaryKeys = ["bank_id", "question_id", "tag"],
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["bank_id", "question_id"],
            childColumns = ["bank_id", "question_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tag"), Index(value = ["bank_id", "question_id"])],
)
data class QuestionTagEntity(
    @ColumnInfo(name = "bank_id")
    val bankId: String,
    @ColumnInfo(name = "question_id")
    val questionId: String,
    val tag: String,
)
