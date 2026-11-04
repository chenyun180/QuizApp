package com.alri.quiz.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.alri.quiz.core.database.entity.AnswerRecordEntity
import kotlinx.coroutines.flow.Flow

/** 答题记录的写入与统计。 */
@Dao
interface AnswerRecordDao {

    @Insert
    suspend fun insert(record: AnswerRecordEntity): Long

    @Query("SELECT * FROM answer_records WHERE bank_id = :bankId AND question_id = :questionId ORDER BY answered_at DESC")
    suspend fun historyOf(bankId: String, questionId: String): List<AnswerRecordEntity>

    @Query("SELECT * FROM answer_records ORDER BY answered_at DESC")
    fun observeRecent(): Flow<List<AnswerRecordEntity>>

    @Query("SELECT COUNT(*) FROM answer_records")
    fun observeTotalAnswered(): Flow<Int>

    @Query("SELECT COUNT(*) FROM answer_records WHERE is_correct = 1")
    fun observeTotalCorrect(): Flow<Int>

    @Query("SELECT COUNT(*) FROM answer_records WHERE bank_id = :bankId")
    suspend fun countAnsweredInBank(bankId: String): Int

    @Query("SELECT COUNT(*) FROM answer_records WHERE bank_id = :bankId AND is_correct = 1")
    suspend fun countCorrectInBank(bankId: String): Int

    /** 做过多少道**不重复**的题，用于"已覆盖 x/y 题"这种进度。 */
    @Query("SELECT COUNT(DISTINCT bank_id || '/' || question_id) FROM answer_records")
    fun observeDistinctAnswered(): Flow<Int>

    @Query("DELETE FROM answer_records")
    suspend fun deleteAll()
}
