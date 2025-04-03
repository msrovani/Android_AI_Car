package com.example.androidiacar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import android.widget.LinearLayout
import android.widget.Button
import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var kittScanner: ImageView
    private lateinit var kittText: TextView

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "KITT está pronto para ajudar!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "KITT precisa das permissões para funcionar corretamente",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        kittScanner = findViewById(R.id.kittScanner)
        kittText = findViewById(R.id.kittText)

        // Inicializa o TextToSpeech
        textToSpeech = TextToSpeech(this, this)

        // Inicia a animação do scanner
        val scannerAnimation = AnimationUtils.loadAnimation(this, R.anim.kitt_scanner_animation)
        kittScanner.startAnimation(scannerAnimation)

        // Mostra o texto após 2 segundos
        kittScanner.postDelayed({
            kittText.visibility = TextView.VISIBLE
            // Fala a frase do KITT
            textToSpeech.speak(
                "Eu sou a voz do Knight Industries Two Thousand",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "kitt_intro"
            )
        }, 2000)

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

        // Verifica e solicita permissões
        checkAndRequestPermissions()

        setContentView(mainLayout)
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale("pt", "BR"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Idioma não suportado
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
} 