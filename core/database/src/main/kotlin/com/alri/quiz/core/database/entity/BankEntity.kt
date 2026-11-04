package com.alri.quiz.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 题库表。
 *
 * [bankId] 是题库 JSON 里声明的业务主键（不是自增 id），
 * 这样同一份题库重复导入时是「覆盖更新」而不是又插一份，天然幂等。
 */
@Entity(tableName = "banks")
data class BankEntity(
    @PrimaryKey
    @ColumnInfo(name = "bank_id")
    val bankId: String,
    val title: String,
    val version: Int,
    @ColumnInfo(name = "question_count")
    val questionCount: Int,
    /** 导入时间（epoch millis），题库列表按它倒序。 */
    @ColumnInfo(name = "imported_at")
    val importedAt: Long,
)
