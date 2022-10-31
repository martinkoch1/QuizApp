package com.example.quizapp.di

import com.example.quizapp.data.QuizApi
import com.example.quizapp.data.QuizRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import javax.inject.Singleton

/**
 * How we construct the dependencies so that dagger can inject the classes.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideQuizRepository(
        api: QuizApi
    ) = QuizRepositoryImpl(api)

    @Singleton
    @Provides
    fun provideQuizApi(): QuizApi {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create())
            .baseUrl("https://opentdb.com/")
            .build()
            .create(QuizApi::class.java)
    }
}