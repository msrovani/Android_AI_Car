package com.example.androidiacar

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application() { 
    // Você pode adicionar lógica de inicialização aqui se necessário,
    // mas para o Hilt, apenas a anotação e a herança são requeridas.
    override fun onCreate() {
        super.onCreate()
        // Exemplo: Inicializar bibliotecas
    }
} 