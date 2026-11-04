package com.alri.quiz.core.database.di

import android.content.Context
import androidx.room.Room
import com.alri.quiz.core.database.dao.AnswerRecordDao
import com.alri.quiz.core.database.dao.BankDao
import com.alri.quiz.core.database.dao.FavoriteDao
import com.alri.quiz.core.database.dao.QuestionDao
import com.alri.quiz.core.database.db.QuizDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 数据库只实例化一次（@Singleton），Room 实例本身就是线程安全的连接池。 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): QuizDatabase =
        Room.databaseBuilder(context, QuizDatabase::class.java, QuizDatabase.NAME)
            // 目前只有 version 1，没有历史迁移要跑。以后新增版本时，
            // 在这里加 .addMigrations(MIGRATION_1_2) 并把 fallback 删掉。
            .build()

    @Provides
    fun provideBankDao(db: QuizDatabase): BankDao = db.bankDao()

    @Provides
    fun provideQuestionDao(db: QuizDatabase): QuestionDao = db.questionDao()

    @Provides
    fun provideAnswerRecordDao(db: QuizDatabase): AnswerRecordDao = db.answerRecordDao()

    @Provides
    fun provideFavoriteDao(db: QuizDatabase): FavoriteDao = db.favoriteDao()
}
