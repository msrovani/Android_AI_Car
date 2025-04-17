package com.example.androidiacar

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.google.ai.client.generativeai.GenerativeModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@AndroidEntryPoint
class MainCarAppService : CarAppService() {

    @Inject
    lateinit var generativeModel: GenerativeModel

    @Inject
    lateinit var coroutineScope: CoroutineScope

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return MainCarSession(generativeModel, coroutineScope)
    }
} 