package com.alri.quiz.core.database

import com.alri.quiz.core.database.db.QuizDatabase
import com.alri.quiz.core.model.Option
import com.alri.quiz.core.model.Question
import com.alri.quiz.core.model.QuestionType
import com.alri.quiz.core.model.QuizBank
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry

/**
 * 测试用的构造数据与内存数据库。
 *
 * 用 inMemoryDatabaseBuilder：每个测试从空库开始，互不影响，跑完即销毁，
 * 也不会污染真机上用户的 quiz.db。
 */
internal object TestRepo {

    fun inMemoryDb(): QuizDatabase {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return Room.inMemoryDatabaseBuilder(context, QuizDatabase::class.java)
            // 允许主线程查询：测试里就是要在 instrumentation 线程直接读，不需要 UI 线程模型
            .allowMainThreadQueries()
            .build()
    }

    fun targetContext(): android.content.Context = InstrumentationRegistry.getInstrumentation().targetContext

    fun question(
        id: String,
        type: QuestionType = QuestionType.SINGLE,
        stem: String = "题干 $id",
        options: List<Option> = listOf(Option("A", "选项 A"), Option("B", "选项 B")),
        answers: Set<String> = setOf("A"),
        explanation: String? = "解析 $id",
        tags: List<String> = listOf("基础"),
    ): Question = Question(
        id = id,
        type = type,
        stem = stem,
        options = options,
        answers = answers,
        explanation = explanation,
        tags = tags,
    )

    fun bank(
        id: String = "bank-1",
        title: String = "测试题库",
        version: Int = 1,
        questions: List<Question> = listOf(question("q1"), question("q2")),
    ): QuizBank = QuizBank(id = id, title = title, version = version, questions = questions)
}
