package com.alri.quiz.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

/** 收藏表。做成独立的表而不是给题目加一列，因为「收藏」是用户的数据，题库更新时不该被覆盖。 */
@Entity(
    tableName = "favorites",
    primaryKeys = ["bank_id", "question_id"],
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["bank_id", "question_id"],
            childColumns = ["bank_id", "question_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class FavoriteEntity(
    @ColumnInfo(name = "bank_id")
    val bankId: String,
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "added_at")
    val addedAt: Long,
)
