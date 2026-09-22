package com.example.notificationwebhookapp;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class SettingsActivity extends AppCompatActivity {
    private Call testCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        SystemBarInsets.apply(getWindow(), findViewById(R.id.settingsRoot));

        EditText urlInput = findViewById(R.id.webhookUrlEditText);
        Button testButton = findViewById(R.id.sendTestButton);
        TextView testStatus = findViewById(R.id.testStatusTextView);
        urlInput.setText(AppPreferences.getWebhookUrl(this));
        findViewById(R.id.saveButton).setOnClickListener(view -> {
            try {
                String url = WebhookUrl.normalize(urlInput.getText().toString());
                AppPreferences.setWebhookUrl(this, url);
                Toast.makeText(this, R.string.url_saved, Toast.LENGTH_SHORT).show();
                finish();
            } catch (IllegalArgumentException error) {
                urlInput.setError(error.getMessage());
            }
        });
        testButton.setOnClickListener(view -> sendTest(urlInput, testButton, testStatus));
    }

    private void sendTest(EditText urlInput, Button button, TextView status) {
        final String url;
        try {
            url = WebhookUrl.normalize(urlInput.getText().toString());
            urlInput.setError(null);
        } catch (IllegalArgumentException error) {
            urlInput.setError(error.getMessage());
            return;
        }

        String payload = WebhookPayload.fromNotification(
                getPackageName(), getString(R.string.test_payload_title),
                getString(R.string.test_payload_message), AppPreferences.getDeviceId(this),
                Build.MANUFACTURER + " " + Build.MODEL, Build.VERSION.RELEASE);
        button.setEnabled(false);
        status.setVisibility(View.VISIBLE);
        status.setText(R.string.sending_test_payload);

        testCall = WebhookClient.newCall(url, payload);
        testCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException error) {
                showTestResult(button, status, getString(
                        R.string.test_payload_network_error,
                        error.getLocalizedMessage() == null ? error.getClass().getSimpleName()
                                : error.getLocalizedMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response ignored = response) {
                    int code = response.code();
                    int message = response.isSuccessful()
                            ? R.string.test_payload_success : R.string.test_payload_http_error;
                    showTestResult(button, status, getString(message, code));
                }
            }
        });
    }

    private void showTestResult(Button button, TextView status, String message) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            button.setEnabled(true);
            status.setText(message);
        });
    }

    @Override
    protected void onDestroy() {
        if (testCall != null) testCall.cancel();
        super.onDestroy();
    }
}
