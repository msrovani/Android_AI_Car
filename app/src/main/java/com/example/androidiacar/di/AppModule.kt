package com.example.androidiacar.di

import android.content.Context
import com.example.androidiacar.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.IO)
    }

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel {
        // Validação básica da chave API
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNullOrEmpty() || apiKey == "YOUR_API_KEY_HERE") {
            // Lançar um erro ou logar um aviso crítico
            // Isso impede a execução se a chave não estiver configurada
            throw IllegalStateException("API Key do Gemini não configurada em local.properties!")
        }
        return GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = apiKey
        )
    }
} 