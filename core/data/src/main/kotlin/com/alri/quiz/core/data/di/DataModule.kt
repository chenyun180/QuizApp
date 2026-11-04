package com.alri.quiz.core.data.di

import com.alri.quiz.core.data.importer.BankJsonParser
import com.alri.quiz.core.data.repository.DefaultQuizRepository
import com.alri.quiz.core.data.repository.QuizRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindQuizRepository(impl: DefaultQuizRepository): QuizRepository

    companion object {
        @Provides
        @Singleton
        fun provideBankJsonParser(): BankJsonParser = BankJsonParser()
    }
}
