package com.example.androidiacar

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Button
import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Criar layout principal
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        // Título
        val titleText = TextView(this).apply {
            text = "KITT - Android Auto Assistant"
            setTextColor(Color.RED)
            textSize = 24f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = 32
            }
        }
        mainLayout.addView(titleText)

        // Descrição
        val descriptionText = TextView(this).apply {
            text = "Seu assistente de IA para Android Auto, inspirado no KITT de Knight Rider"
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = 32
            }
        }
        mainLayout.addView(descriptionText)

        // Botão para abrir Android Auto
        val openAutoButton = Button(this).apply {
            text = "Abrir Android Auto"
            setBackgroundColor(Color.RED)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                bottomMargin = 16
            }
            setOnClickListener {
                try {
                    val intent = packageManager.getLaunchIntentForPackage("com.google.android.projection.gearhead")
                    if (intent != null) {
                        startActivity(intent)
                    } else {
                        // Se o Android Auto não estiver instalado, abrir na Play Store
                        startActivity(Intent(Intent.ACTION_VIEW, 
                            Uri.parse("market://details?id=com.google.android.projection.gearhead")))
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, 
                        "Erro ao abrir Android Auto: ${e.message}", 
                        Toast.LENGTH_LONG).show()
                }
            }
        }
        mainLayout.addView(openAutoButton)

        // Botão para configurações
        val settingsButton = Button(this).apply {
            text = "Configurações"
            setBackgroundColor(Color.RED)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            setOnClickListener {
                Toast.makeText(this@MainActivity, 
                    "Configurações em desenvolvimento", 
                    Toast.LENGTH_SHORT).show()
            }
        }
        mainLayout.addView(settingsButton)

        setContentView(mainLayout)
    }
} 