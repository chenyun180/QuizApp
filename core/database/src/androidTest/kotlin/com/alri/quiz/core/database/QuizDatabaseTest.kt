package com.alri.quiz.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.alri.quiz.core.database.db.QuizDatabase
import com.alri.quiz.core.database.db.toDomain
import com.alri.quiz.core.database.db.toRows
import com.alri.quiz.core.database.entity.AnswerRecordEntity
import com.alri.quiz.core.database.entity.FavoriteEntity
import com.alri.quiz.core.model.QuestionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 数据库层集成测试（instrumented）。
 *
 * 必须跑在真机/模拟器上，因为它验证的是真实的 SQLite：外键级联、
 * 事务回滚、索引上的查询语义。这些在 JVM 单元测试里验证不了。
 *
 * 运行：./gradlew :core:database:connectedDebugAndroidTest（需要连接设备）
 * 只编译不跑：./gradlew :core:database:assembleDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class QuizDatabaseTest {

    private lateinit var db: QuizDatabase

    private val t0 = 1_700_000_000_000L

    @Before
    fun setUp() {
        db = TestRepo.inMemoryDb()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun insert(bank: com.alri.quiz.core.model.QuizBank, importedAt: Long = t0) = runBlocking {
        val (bankRow, questions, tags) = bank.toRows(importedAt)
        db.bankDao().replaceBankContent(bankRow, questions, tags)
    }

    @Test
    fun importBank_writesBankQuestionsAndTags() = runBlocking {
        insert(TestRepo.bank(questions = listOf(TestRepo.question("q1"), TestRepo.question("q2"), TestRepo.question("q3"))))

        assertEquals(1, db.bankDao().countBanks())
        val bank = db.bankDao().getBank("bank-1")
        assertNotNull(bank)
        assertEquals("测试题库", bank!!.title)
        assertEquals(3, bank.questionCount)

        val questions = db.questionDao().getQuestions("bank-1")
        assertEquals(3, questions.size)
        // order_index 必须跟 JSON 里的顺序一致，否则每次都乱序
        assertEquals(listOf("q1", "q2", "q3"), questions.map { it.questionId })

        // tags 摊平成了独立表，才能按标签走索引查询
        val tags = db.questionDao().observeTags("bank-1").first()
        assertEquals(listOf("基础"), tags)
    }

    @Test
    fun reimportSameBank_replacesInsteadOfDuplicating() = runBlocking {
        insert(TestRepo.bank(questions = listOf(TestRepo.question("q1"), TestRepo.question("q2"))))
        // 第二次导入只剩一题：旧题必须消失，不能残留
        insert(
            TestRepo.bank(questions = listOf(TestRepo.question("q9"))),
            importedAt = t0 + 1_000,
        )

        assertEquals(1, db.bankDao().countBanks())
        val questions = db.questionDao().getQuestions("bank-1")
        assertEquals(listOf("q9"), questions.map { it.questionId })
        assertEquals(1, db.bankDao().getBank("bank-1")!!.questionCount)
    }

    @Test
    fun twoBanksWithSameQuestionId_coexist() = runBlocking {
        // question_id 只在题库内唯一，复合主键要能撑住跨库同名
        insert(TestRepo.bank(id = "bank-a", questions = listOf(TestRepo.question("q1"))))
        insert(TestRepo.bank(id = "bank-b", questions = listOf(TestRepo.question("q1"))))

        assertEquals(2, db.bankDao().countBanks())
        assertEquals(1, db.questionDao().countQuestions("bank-a"))
        assertEquals(1, db.questionDao().countQuestions("bank-b"))
    }

    @Test
    fun roundTrip_preservesOptionsAnswersAndType() = runBlocking {
        val multi = TestRepo.question(
            id = "m1",
            type = QuestionType.MULTIPLE,
            options = listOf(
                com.alri.quiz.core.model.Option("A", "甲"),
                com.alri.quiz.core.model.Option("B", "乙"),
                com.alri.quiz.core.model.Option("C", "丙"),
            ),
            answers = setOf("A", "C"),
            explanation = "选甲和丙",
            tags = listOf("多选", "进阶"),
        )
        insert(TestRepo.bank(questions = listOf(multi)))

        val stored = db.questionDao().getQuestion("bank-1", "m1")!!.toDomain()
        assertEquals(QuestionType.MULTIPLE, stored.type)
        assertEquals(3, stored.options.size)
        assertEquals("丙", stored.options[2].text)
        assertEquals(setOf("A", "C"), stored.answers)
        assertEquals("选甲和丙", stored.explanation)
        assertEquals(listOf("多选", "进阶"), stored.tags)
    }

    @Test
    fun wrongBook_tracksLatestRecordOnly() = runBlocking {
        insert(TestRepo.bank(questions = listOf(TestRepo.question("q1"), TestRepo.question("q2"))))
        val records = db.answerRecordDao()

        // q1 先答错、后答对 → 应离开错题本
        records.insert(AnswerRecordEntity(bankId = "bank-1", questionId = "q1", selected = setOf("B"), isCorrect = false, answeredAt = t0))
        records.insert(AnswerRecordEntity(bankId = "bank-1", questionId = "q1", selected = setOf("A"), isCorrect = true, answeredAt = t0 + 10))
        // q2 只答错过一次 → 留在错题本
        records.insert(AnswerRecordEntity(bankId = "bank-1", questionId = "q2", selected = setOf("B"), isCorrect = false, answeredAt = t0 + 5))

        assertEquals(listOf("q2"), db.questionDao().observeWrongQuestions().first().map { it.questionId })
        assertEquals(1, db.questionDao().observeWrongCount().first())
    }

    @Test
    fun answerStats_countEveryAttempt() = runBlocking {
        insert(TestRepo.bank(questions = listOf(TestRepo.question("q1"), TestRepo.question("q2"))))
        val records = db.answerRecordDao()
        records.insert(AnswerRecordEntity(bankId = "bank-1", questionId = "q1", selected = setOf("B"), isCorrect = false, answeredAt = t0))
        records.insert(AnswerRecordEntity(bankId = "bank-1", questionId = "q1", selected = setOf("A"), isCorrect = true, answeredAt = t0 + 1))
        records.insert(AnswerRecordEntity(bankId = "bank-1", questionId = "q2", selected = setOf("A"), isCorrect = true, answeredAt = t0 + 2))

        assertEquals(3, records.observeTotalAnswered().first())
        assertEquals(2, records.observeTotalCorrect().first())
        // 不重复题数：q1 答了两次只算一道
        assertEquals(2, records.observeDistinctAnswered().first())
        assertEquals(3, records.countAnsweredInBank("bank-1"))
        assertEquals(2, records.countCorrectInBank("bank-1"))
    }

    @Test
    fun favorites_toggleIsIdempotent() = runBlocking {
        insert(TestRepo.bank(questions = listOf(TestRepo.question("q1"))))
        val favorites = db.favoriteDao()

        assertFalse(favorites.isFavorite("bank-1", "q1"))
        favorites.add(FavoriteEntity(bankId = "bank-1", questionId = "q1", addedAt = t0))
        assertTrue(favorites.isFavorite("bank-1", "q1"))
        // 再 add 一次不应该崩（主键冲突用 upsert 吸收）
        favorites.add(FavoriteEntity(bankId = "bank-1", questionId = "q1", addedAt = t0 + 100))
        assertTrue(favorites.isFavorite("bank-1", "q1"))
        assertEquals(1, db.questionDao().observeFavorites().first().size)

        // remove 返回被删行数，重复删是 0，天然幂等
        assertEquals(1, favorites.remove("bank-1", "q1"))
        assertEquals(0, favorites.remove("bank-1", "q1"))
        assertFalse(favorites.isFavorite("bank-1", "q1"))
    }

    @Test
    fun deleteBank_cascadesToQuestionsTagsRecordsAndFavorites() = runBlocking {
        insert(TestRepo.bank(questions = listOf(TestRepo.question("q1"), TestRepo.question("q2"))))
        db.answerRecordDao().insert(AnswerRecordEntity(bankId = "bank-1", questionId = "q1", selected = setOf("A"), isCorrect = true, answeredAt = t0))
        db.favoriteDao().add(FavoriteEntity(bankId = "bank-1", questionId = "q2", addedAt = t0))

        db.bankDao().deleteBank("bank-1")

        // 外键 CASCADE 生效（Room 默认开启 foreign_keys pragma）
        assertNull(db.bankDao().getBank("bank-1"))
        assertEquals(0, db.questionDao().countQuestions("bank-1"))
        assertEquals(emptyList<String>(), db.questionDao().observeTags("bank-1").first())
        assertEquals(0, db.answerRecordDao().observeTotalAnswered().first())
        assertEquals(0, db.questionDao().observeFavorites().first().size)
    }

    @Test
    fun randomQuestions_returnsAtMostLimitAndOnlyFromThatBank() = runBlocking {
        insert(
            TestRepo.bank(
                id = "bank-1",
                questions = (1..10).map { TestRepo.question("q$it") },
            ),
        )
        insert(TestRepo.bank(id = "bank-2", questions = listOf(TestRepo.question("x1"))))

        val picked = db.questionDao().getRandomQuestions("bank-1", 3)
        assertEquals(3, picked.size)
        assertTrue(picked.all { it.bankId == "bank-1" })
        // 不重复
        assertEquals(3, picked.map { it.questionId }.toSet().size)
    }

    @Test
    fun observeByTag_filtersThroughJoinTable() = runBlocking {
        insert(
            TestRepo.bank(
                questions = listOf(
                    TestRepo.question("q1", tags = listOf("Activity")),
                    TestRepo.question("q2", tags = listOf("Service")),
                    TestRepo.question("q3", tags = listOf("Activity", "Service")),
                ),
            ),
        )

        val activity = db.questionDao().observeByTag("bank-1", "Activity").first()
        assertEquals(listOf("q1", "q3"), activity.map { it.questionId })

        val service = db.questionDao().observeByTag("bank-1", "Service").first()
        assertEquals(listOf("q2", "q3"), service.map { it.questionId })

        assertEquals(emptyList<String>(), db.questionDao().observeByTag("bank-1", "不存在").first())
    }
}
