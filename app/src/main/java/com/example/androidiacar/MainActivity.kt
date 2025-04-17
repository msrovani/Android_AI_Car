package com.example.androidiacar

import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import android.util.Log
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var kittScanner: ImageView
    private lateinit var kittText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            initializeViews()
            initializeApp()
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro na inicialização: ${e.message}", e)
            Toast.makeText(this, "Erro ao inicializar o KITT. Por favor, tente novamente.", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeViews() {
        kittScanner = findViewById(R.id.kittScanner)
        kittText = findViewById(R.id.kittText)
        
        // Inicia a animação do scanner
        val scannerAnimation = AnimationUtils.loadAnimation(this, R.anim.kitt_scanner_animation)
        kittScanner.startAnimation(scannerAnimation)
    }

    private fun initializeApp() {
        // Mostra o texto após 2 segundos
        kittScanner.postDelayed({
            kittText.visibility = TextView.VISIBLE
        }, 2000)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
} 