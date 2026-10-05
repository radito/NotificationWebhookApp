package com.example.notificationwebhookapp

import android.app.Application
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response

class SettingsViewModel(application: Application, private val savedState: SavedStateHandle) :
    AndroidViewModel(application) {
    var url by mutableStateOf(savedState.get<String>(WEBHOOK_URL) ?: AppPreferences.getWebhookUrl(application))
        private set
    var urlError by mutableStateOf<String?>(null)
        private set
    var sendingTest by mutableStateOf(false)
        private set
    var testStatus by mutableStateOf<String?>(null)
        private set
    private var testCall: Call? = null

    fun updateUrl(value: String) {
        url = value.take(2048)
        savedState[WEBHOOK_URL] = url
        urlError = null
    }

    fun saveUrl(): Boolean {
        val normalized = normalizedUrl() ?: return false
        AppPreferences.setWebhookUrl(getApplication(), normalized)
        return true
    }

    private fun normalizedUrl(): String? = try {
        WebhookUrl.normalize(url).also { urlError = null }
    } catch (error: IllegalArgumentException) {
        urlError = error.message
        null
    }

    fun sendTest() {
        if (sendingTest) return
        val normalized = normalizedUrl() ?: return
        val context = getApplication<Application>()
        val payload = WebhookPayload.fromNotification(
            context.packageName,
            context.getString(R.string.test_payload_title),
            context.getString(R.string.test_payload_message),
            AppPreferences.getDeviceId(context),
            "${Build.MANUFACTURER} ${Build.MODEL}",
            Build.VERSION.RELEASE,
        )
        sendingTest = true
        testStatus = context.getString(R.string.sending_test_payload)
        val request = WebhookClient.newCall(normalized, payload)
        testCall = request
        request.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showTestResult(context.getString(
                    R.string.test_payload_network_error,
                    e.localizedMessage ?: e.javaClass.simpleName,
                ))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val message = if (it.isSuccessful) R.string.test_payload_success else R.string.test_payload_http_error
                    showTestResult(context.getString(message, it.code))
                }
            }
        })
    }

    private fun showTestResult(message: String) {
        viewModelScope.launch {
            sendingTest = false
            testStatus = message
            testCall = null
        }
    }

    override fun onCleared() {
        testCall?.cancel()
        super.onCleared()
    }

    private companion object {
        const val WEBHOOK_URL = "webhookUrlDraft"
    }
}
