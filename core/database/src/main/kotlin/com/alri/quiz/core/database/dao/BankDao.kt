package com.alri.quiz.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.alri.quiz.core.database.entity.BankEntity
import com.alri.quiz.core.database.entity.QuestionEntity
import com.alri.quiz.core.database.entity.QuestionTagEntity
import kotlinx.coroutines.flow.Flow

/** 题库的读写。导入时整库覆盖，保证同一份 JSON 反复导入不会产生重复数据。 */
@Dao
interface BankDao {

    @Query("SELECT * FROM banks ORDER BY imported_at DESC")
    fun observeBanks(): Flow<List<BankEntity>>

    @Query("SELECT * FROM banks ORDER BY imported_at DESC")
    suspend fun getBanks(): List<BankEntity>

    @Query("SELECT * FROM banks WHERE bank_id = :bankId")
    suspend fun getBank(bankId: String): BankEntity?

    @Query("SELECT * FROM banks WHERE bank_id = :bankId")
    fun observeBank(bankId: String): Flow<BankEntity?>

    @Query("SELECT COUNT(*) FROM banks")
    suspend fun countBanks(): Int

    @Upsert
    suspend fun upsertBank(bank: BankEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<QuestionTagEntity>)

    @Query("DELETE FROM questions WHERE bank_id = :bankId")
    suspend fun deleteQuestionsOfBank(bankId: String)

    @Query("DELETE FROM banks WHERE bank_id = :bankId")
    suspend fun deleteBank(bankId: String)

    @Query("DELETE FROM banks")
    suspend fun deleteAllBanks()

    /**
     * 用新内容整体替换一个题库的题目。
     * 必须先删旧题再插新题：重新导入时题目可能被删掉了，
     * 不删就会残留。整个过程在一个事务里，中途失败会整体回滚。
     */
    @Transaction
    suspend fun replaceBankContent(bank: BankEntity, questions: List<QuestionEntity>, tags: List<QuestionTagEntity>) {
        deleteQuestionsOfBank(bank.bankId)
        upsertBank(bank)
        if (questions.isNotEmpty()) insertQuestions(questions)
        if (tags.isNotEmpty()) insertTags(tags)
    }
}
