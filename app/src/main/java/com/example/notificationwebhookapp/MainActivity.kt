package com.example.notificationwebhookapp

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainActivity : ComponentActivity() {
    private val model: MainViewModel by viewModels()
    private var notificationAccess by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotificationWebhookTheme {
                MainScreen(
                    model = model,
                    notificationAccess = notificationAccess,
                    onSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                    onGrantAccess = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                    onSaveApps = {
                        model.saveSelectedApps()
                        Toast.makeText(this, R.string.apps_saved, Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val ownService = ComponentName(this, NotificationListener::class.java)
        notificationAccess = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.split(':')?.any { ComponentName.unflattenFromString(it) == ownService } == true
    }
}

@Composable
private fun MainScreen(
    model: MainViewModel,
    notificationAccess: Boolean,
    onSettings: () -> Unit,
    onGrantAccess: () -> Unit,
    onSaveApps: () -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val visibleApps = remember(model.apps, search) {
        val query = search.trim()
        model.apps.filter {
            it.name.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.app_name),
                actions = {
                    TextButton(text = stringResource(R.string.settings), onClick = onSettings)
                },
            )
        },
        bottomBar = {
            Column(
                Modifier.fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                TextButton(
                    text = stringResource(R.string.save_apps),
                    onClick = { focusManager.clearFocus(); onSaveApps() },
                    enabled = !model.loading,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(insideMargin = PaddingValues(16.dp)) {
                    Text(stringResource(R.string.notification_access), style = MiuixTheme.textStyles.title4)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(if (notificationAccess) R.string.listener_enabled else R.string.listener_disabled),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        text = stringResource(R.string.enable_notifications),
                        onClick = onGrantAccess,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                Card {
                    Row(
                        Modifier.fillMaxWidth().toggleable(
                            value = model.forwardingEnabled,
                            role = Role.Switch,
                            onValueChange = model::setForwarding,
                        ).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(stringResource(R.string.forward_notifications), modifier = Modifier.weight(1f))
                        Switch(checked = model.forwardingEnabled, onCheckedChange = null)
                    }
                }
            }
            item {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.selected_apps), style = MiuixTheme.textStyles.title4)
                    Text(
                        pluralStringResource(R.plurals.selected_apps_count, model.selectedPackages.size, model.selectedPackages.size),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
            item {
                TextField(
                    value = search,
                    onValueChange = { search = it },
                    label = stringResource(R.string.search_apps),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (model.loading || visibleApps.isEmpty()) {
                item {
                    Text(
                        stringResource(if (model.loading) R.string.loading_apps else R.string.no_apps_found),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
            items(visibleApps, key = { it.packageName }) { app ->
                AppRow(
                    app = app,
                    selected = app.packageName in model.selectedPackages,
                    onSelected = { model.selectApp(app.packageName, it) },
                )
            }
        }
    }
}

@Composable
private fun AppRow(app: InstalledApp, selected: Boolean, onSelected: (Boolean) -> Unit) {
    val iconSize = with(LocalDensity.current) { 48.dp.roundToPx() }
    val icon = remember(app.icon, iconSize) { app.icon.toBitmap(iconSize, iconSize).asImageBitmap() }
    Card {
        Row(
            modifier = Modifier.fillMaxWidth().toggleable(
                value = selected, role = Role.Checkbox, onValueChange = onSelected,
            ).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(48.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(app.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    app.packageName,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Checkbox(state = ToggleableState(selected), onClick = null)
        }
    }
}
