package com.alri.quiz.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.alri.quiz.core.database.entity.FavoriteEntity
import com.alri.quiz.core.database.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

/** 题目的查询：按库、按标签、随机抽题、错题、收藏。 */
@Dao
interface QuestionDao {

    @Query("SELECT * FROM questions WHERE bank_id = :bankId ORDER BY order_index ASC")
    fun observeQuestions(bankId: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE bank_id = :bankId ORDER BY order_index ASC")
    suspend fun getQuestions(bankId: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE bank_id = :bankId AND question_id = :questionId")
    suspend fun getQuestion(bankId: String, questionId: String): QuestionEntity?

    @Query("SELECT COUNT(*) FROM questions WHERE bank_id = :bankId")
    suspend fun countQuestions(bankId: String): Int

    /** 随机抽题。ORDER BY RANDOM() 在 SQLite 上对小表足够用。 */
    @Query("SELECT * FROM questions WHERE bank_id = :bankId ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomQuestions(bankId: String, limit: Int): List<QuestionEntity>

    /** 按标签筛题，走 question_tags 的 tag 索引。 */
    @Query(
        """
        SELECT q.* FROM questions q
        INNER JOIN question_tags t
            ON t.bank_id = q.bank_id AND t.question_id = q.question_id
        WHERE q.bank_id = :bankId AND t.tag = :tag
        ORDER BY q.order_index ASC
        """,
    )
    fun observeByTag(bankId: String, tag: String): Flow<List<QuestionEntity>>

    @Query(
        """
        SELECT DISTINCT tag FROM question_tags
        WHERE bank_id = :bankId
        ORDER BY tag ASC
        """,
    )
    fun observeTags(bankId: String): Flow<List<String>>

    /**
     * 错题：**最近一次作答是错的**的题。
     *
     * 只看最新一次记录，是因为用户重做答对了，就该从错题本消失。
     * 写法用「相关子查询定位最新一条记录」而不是窗口函数 ROW_NUMBER()：
     * Room 2.6 的 SQL 解析器（基于 SQLite 语法子集）不接受 OVER (...)。
     * 两种写法结果一致，这个在 SQLite 上还能命中 (bank_id, question_id) 索引。
     */
    @Query(
        """
        SELECT q.* FROM questions q
        INNER JOIN answer_records r ON r.id = (
            SELECT r2.id FROM answer_records r2
            WHERE r2.bank_id = q.bank_id AND r2.question_id = q.question_id
            ORDER BY r2.answered_at DESC, r2.id DESC
            LIMIT 1
        )
        WHERE r.is_correct = 0
        ORDER BY q.bank_id ASC, q.order_index ASC
        """,
    )
    fun observeWrongQuestions(): Flow<List<QuestionEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM questions q
        INNER JOIN answer_records r ON r.id = (
            SELECT r2.id FROM answer_records r2
            WHERE r2.bank_id = q.bank_id AND r2.question_id = q.question_id
            ORDER BY r2.answered_at DESC, r2.id DESC
            LIMIT 1
        )
        WHERE r.is_correct = 0
        """,
    )
    fun observeWrongCount(): Flow<Int>

    /** 收藏夹里的题，最近收藏的排前面。 */
    @Query(
        """
        SELECT q.* FROM questions q
        INNER JOIN favorites f
            ON f.bank_id = q.bank_id AND f.question_id = q.question_id
        ORDER BY f.added_at DESC
        """,
    )
    fun observeFavorites(): Flow<List<QuestionEntity>>

    @Upsert
    suspend fun upsert(question: QuestionEntity)
}
