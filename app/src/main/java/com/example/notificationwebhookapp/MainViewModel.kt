package com.example.notificationwebhookapp

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class InstalledApp(val name: String, val packageName: String, val icon: Drawable)

class MainViewModel(application: Application, private val savedState: SavedStateHandle) :
    AndroidViewModel(application) {
    internal var apps by mutableStateOf<List<InstalledApp>>(emptyList())
        private set
    var loading by mutableStateOf(true)
        private set
    var selectedPackages by mutableStateOf<Set<String>>(
        savedState.get<ArrayList<String>>(SELECTED_PACKAGES)?.toSet()
            ?: AppPreferences.getSelectedApps(application).toSet(),
    )
        private set
    var forwardingEnabled by mutableStateOf(AppPreferences.isForwardingEnabled(application))
        private set

    init {
        viewModelScope.launch {
            val installed = withContext(Dispatchers.IO) {
                val manager = application.packageManager
                manager.getInstalledApplications(0).mapNotNull { app ->
                    try {
                        InstalledApp(app.loadLabel(manager).toString(), app.packageName, app.loadIcon(manager))
                    } catch (_: RuntimeException) {
                        // An app can be removed while its label or icon is being loaded.
                        null
                    }
                }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            }
            apps = installed
            loading = false
        }
    }

    fun selectApp(packageName: String, selected: Boolean) {
        selectedPackages = if (selected) selectedPackages + packageName else selectedPackages - packageName
        savedState[SELECTED_PACKAGES] = ArrayList(selectedPackages)
    }

    fun saveSelectedApps() {
        AppPreferences.setSelectedApps(getApplication(), selectedPackages)
    }

    fun setForwarding(enabled: Boolean) {
        forwardingEnabled = enabled
        val context = getApplication<Application>()
        AppPreferences.setForwardingEnabled(context, enabled)
        if (!enabled) {
            WorkManager.getInstance(context).cancelAllWorkByTag(WebhookWorker.WORK_TAG)
            PendingWebhookStore.clear(context)
        }
    }

    private companion object {
        const val SELECTED_PACKAGES = "selectedPackages"
    }
}
