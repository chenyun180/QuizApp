package com.alri.quiz.core.database.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.alri.quiz.core.database.dao.AnswerRecordDao
import com.alri.quiz.core.database.dao.BankDao
import com.alri.quiz.core.database.dao.FavoriteDao
import com.alri.quiz.core.database.dao.QuestionDao
import com.alri.quiz.core.database.entity.AnswerRecordEntity
import com.alri.quiz.core.database.entity.BankEntity
import com.alri.quiz.core.database.entity.FavoriteEntity
import com.alri.quiz.core.database.entity.QuestionEntity
import com.alri.quiz.core.database.entity.QuestionTagEntity

/**
 * 应用唯一的 Room 数据库。
 *
 * version = 1：初始版本。以后任何表结构改动都必须 +1 并写 Migration，
 * 否则老用户升级会崩溃。schemas/ 目录里导出的 json 是自动生成迁移的输入，
 * 务必提交到版本库。
 */
@Database(
    entities = [
        BankEntity::class,
        QuestionEntity::class,
        QuestionTagEntity::class,
        AnswerRecordEntity::class,
        FavoriteEntity::class,
    ],
    version = QuizDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(QuizTypeConverters::class)
abstract class QuizDatabase : RoomDatabase() {

    abstract fun bankDao(): BankDao

    abstract fun questionDao(): QuestionDao

    abstract fun answerRecordDao(): AnswerRecordDao

    abstract fun favoriteDao(): FavoriteDao

    companion object {
        const val VERSION: Int = 1
        const val NAME: String = "quiz.db"
    }
}
