package com.example.notificationwebhookapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

class SettingsActivity : ComponentActivity() {
    private val model: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotificationWebhookTheme {
                SettingsScreen(
                    model = model,
                    onBack = { finish() },
                    onSave = {
                        if (model.saveUrl()) {
                            Toast.makeText(this, R.string.url_saved, Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(model: SettingsViewModel, onBack: () -> Unit, onSave: () -> Unit) {
    val focusManager = LocalFocusManager.current
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.webhook_settings),
                navigationIcon = {
                    TextButton(text = stringResource(R.string.back), onClick = onBack)
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(insideMargin = PaddingValues(16.dp)) {
                Text(stringResource(R.string.webhook_url), style = MiuixTheme.textStyles.title4)
                Spacer(Modifier.height(12.dp))
                TextField(
                    value = model.url,
                    onValueChange = model::updateUrl,
                    label = stringResource(R.string.webhook_hint),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth().semantics {
                        model.urlError?.let { error(it) }
                    },
                )
                model.urlError?.let { message ->
                    Text(
                        message,
                        color = MiuixTheme.colorScheme.error,
                        style = MiuixTheme.textStyles.body2,
                        modifier = Modifier.padding(top = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.http_warning),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                )
            }
            TextButton(
                text = stringResource(R.string.save),
                onClick = { focusManager.clearFocus(); onSave() },
                colors = ButtonDefaults.textButtonColorsPrimary(),
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                text = stringResource(R.string.send_test_payload),
                onClick = { focusManager.clearFocus(); model.sendTest() },
                enabled = !model.sendingTest,
                modifier = Modifier.fillMaxWidth(),
            )
            model.testStatus?.let { message ->
                Card(insideMargin = PaddingValues(16.dp)) {
                    Text(
                        message,
                        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        }
    }
}
