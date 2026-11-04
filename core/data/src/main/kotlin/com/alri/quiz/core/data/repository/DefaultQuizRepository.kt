package com.alri.quiz.core.data.repository

import com.alri.quiz.core.data.importer.BankJsonParser
import com.alri.quiz.core.data.importer.ImportResult
import com.alri.quiz.core.database.dao.AnswerRecordDao
import com.alri.quiz.core.database.dao.BankDao
import com.alri.quiz.core.database.dao.FavoriteDao
import com.alri.quiz.core.database.dao.QuestionDao
import com.alri.quiz.core.database.db.toDomain
import com.alri.quiz.core.database.db.toRows
import com.alri.quiz.core.database.db.toSummary
import com.alri.quiz.core.database.entity.AnswerRecordEntity
import com.alri.quiz.core.database.entity.FavoriteEntity
import com.alri.quiz.core.model.BankSummary
import com.alri.quiz.core.model.GradeResult
import com.alri.quiz.core.model.Grading
import com.alri.quiz.core.model.Question
import com.alri.quiz.core.model.QuizBank
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultQuizRepository @Inject constructor(
    private val bankDao: BankDao,
    private val questionDao: QuestionDao,
    private val answerRecordDao: AnswerRecordDao,
    private val favoriteDao: FavoriteDao,
    private val parser: BankJsonParser,
) : QuizRepository {

    override fun observeBanks(): Flow<List<BankSummary>> =
        bankDao.observeBanks().map { banks -> banks.map { it.toSummary() } }

    override fun observeTags(bankId: String): Flow<List<String>> = questionDao.observeTags(bankId)

    override fun observeQuestions(bankId: String): Flow<List<Question>> =
        questionDao.observeQuestions(bankId).map { list -> list.map { it.toDomain() } }

    override suspend fun loadQuestions(bankId: String): List<Question> =
        questionDao.getQuestions(bankId).map { it.toDomain() }

    override suspend fun loadQuestionsByTag(bankId: String, tag: String): List<Question> =
        questionDao.observeByTag(bankId, tag).first().map { it.toDomain() }

    override suspend fun loadWrongQuestions(): List<SourcedQuestion> =
        questionDao.observeWrongQuestions().first().map { SourcedQuestion(it.bankId, it.toDomain()) }

    override suspend fun loadFavoriteQuestions(): List<SourcedQuestion> =
        questionDao.observeFavorites().first().map { SourcedQuestion(it.bankId, it.toDomain()) }

    override suspend fun randomQuestions(bankId: String, limit: Int): List<Question> =
        questionDao.getRandomQuestions(bankId, limit).map { it.toDomain() }

    override fun observeQuestionsByTag(bankId: String, tag: String): Flow<List<Question>> =
        questionDao.observeByTag(bankId, tag).map { list -> list.map { it.toDomain() } }

    override fun observeWrongQuestions(): Flow<List<Question>> =
        questionDao.observeWrongQuestions().map { list -> list.map { it.toDomain() } }

    override fun observeWrongCount(): Flow<Int> = questionDao.observeWrongCount()

    override fun observeFavorites(): Flow<List<Question>> =
        questionDao.observeFavorites().map { list -> list.map { it.toDomain() } }

    override fun observeFavoriteIds(): Flow<List<String>> = favoriteDao.observeFavoriteIds()

    override fun observeIsFavorite(bankId: String, questionId: String): Flow<Boolean> =
        favoriteDao.observeIsFavorite(bankId, questionId)

    override suspend fun toggleFavorite(bankId: String, questionId: String): Boolean {
        // 先查再写有两个 DB 往返，但收藏是低频操作，正确性优先
        return if (favoriteDao.isFavorite(bankId, questionId)) {
            favoriteDao.remove(bankId, questionId)
            false
        } else {
            favoriteDao.add(
                FavoriteEntity(bankId = bankId, questionId = questionId, addedAt = System.currentTimeMillis()),
            )
            true
        }
    }

    override suspend fun submitAnswer(bankId: String, question: Question, selected: Set<String>): GradeResult {
        val result = Grading.grade(question, selected)
        answerRecordDao.insert(
            AnswerRecordEntity(
                bankId = bankId,
                questionId = question.id,
                selected = selected,
                isCorrect = result.isCorrect,
                answeredAt = System.currentTimeMillis(),
            ),
        )
        return result
    }

    override suspend fun importBank(rawJson: String): ImportResult {
        return when (val parsed = parser.parse(rawJson)) {
            is BankJsonParser.Result.Invalid -> ImportResult.Failure(parsed.errors)
            is BankJsonParser.Result.Ok -> writeBank(parsed.bank)
        }
    }

    /**
     * 写入一个已通过校验的题库。
     *
     * [BankDao.replaceBankContent] 内部是 @Transaction：删旧题、写库头、写题目、写标签
     * 要么全成功要么全回滚。这点很重要——导入中途崩溃留下一半题目，用户会以为题库坏了。
     */
    private suspend fun writeBank(bank: QuizBank): ImportResult {
        val existed = bankDao.getBank(bank.id) != null
        val (bankEntity, questionEntities, tagEntities) = bank.toRows(System.currentTimeMillis())
        bankDao.replaceBankContent(bankEntity, questionEntities, tagEntities)
        return ImportResult.Success(
            bankTitle = bank.title,
            questionCount = bank.questions.size,
            replaced = existed,
        )
    }

    override fun observeOverallStats(): Flow<Pair<Int, Int>> =
        combine(
            answerRecordDao.observeTotalAnswered(),
            answerRecordDao.observeTotalCorrect(),
        ) { answered, correct -> answered to correct }

    override suspend fun deleteBank(bankId: String) = bankDao.deleteBank(bankId)

    override suspend fun clearAll() {
        // 外键 CASCADE 会带走题目/标签/记录/收藏，这里显式删一遍更直观也不慢
        bankDao.deleteAllBanks()
        answerRecordDao.deleteAll()
        favoriteDao.deleteAll()
    }
}
