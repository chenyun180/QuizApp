package com.alri.quiz.core.data.repository

import com.alri.quiz.core.data.importer.ImportResult
import com.alri.quiz.core.model.BankSummary
import com.alri.quiz.core.model.GradeResult
import com.alri.quiz.core.model.Question
import com.alri.quiz.core.model.QuizBank
import kotlinx.coroutines.flow.Flow

/**
 * 刷题 App 的唯一数据入口。UI 层只依赖这个接口，不直接碰 Room 或 JSON。
 *
 * 所有方法都是 suspend / Flow：数据库操作没有一个是可以在主线程同步做完的。
 */
interface QuizRepository {

    /** 所有题库概览，按导入时间倒序。 */
    fun observeBanks(): Flow<List<BankSummary>>

    /** 某个题库里的标签，用于筛选。 */
    fun observeTags(bankId: String): Flow<List<String>>

    /** 某题库的题目（按原始顺序）。 */
    fun observeQuestions(bankId: String): Flow<List<Question>>

    /** 一次性取整库题目（开一个练习会话时调用）。 */
    suspend fun loadQuestions(bankId: String): List<Question>

    /** 一次性按标签取题。 */
    suspend fun loadQuestionsByTag(bankId: String, tag: String): List<Question>

    /** 跨题库的错题，用于错题本练习。 */
    suspend fun loadWrongQuestions(): List<SourcedQuestion>

    /** 跨题库的收藏题。 */
    suspend fun loadFavoriteQuestions(): List<SourcedQuestion>

    /** 随机抽 n 题，用于「随机练习」。 */
    suspend fun randomQuestions(bankId: String, limit: Int): List<Question>

    /** 按标签取题。 */
    fun observeQuestionsByTag(bankId: String, tag: String): Flow<List<Question>>

    /** 错题本：最近一次仍答错的题。 */
    fun observeWrongQuestions(): Flow<List<Question>>

    fun observeWrongCount(): Flow<Int>

    /** 收藏的题。 */
    fun observeFavorites(): Flow<List<Question>>

    fun observeFavoriteIds(): Flow<List<String>>

    fun observeIsFavorite(bankId: String, questionId: String): Flow<Boolean>

    /** 切换收藏，返回切换后的状态。 */
    suspend fun toggleFavorite(bankId: String, questionId: String): Boolean

    /**
     * 判分并写入答题记录。判分逻辑本身在 core:model 的 [com.alri.quiz.core.model.Grading]，
     * 这里只负责落库，保证「判分」和「存记录」不会出现两套规则。
     */
    suspend fun submitAnswer(bankId: String, question: Question, selected: Set<String>): GradeResult

    /** 导入：解析 + 校验 +（成功后）在事务里整库覆盖写入。 */
    suspend fun importBank(rawJson: String): ImportResult

    /** 统计：总答题次数 / 总正确次数。 */
    fun observeOverallStats(): Flow<Pair<Int, Int>>

    /** 删除一个题库（连带题目、记录、收藏，靠外键 CASCADE）。 */
    suspend fun deleteBank(bankId: String)

    /** 清空全部数据（设置页的「清除数据」）。 */
    suspend fun clearAll()
}
