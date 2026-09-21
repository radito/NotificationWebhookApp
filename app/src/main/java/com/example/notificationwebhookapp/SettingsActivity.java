package com.example.notificationwebhookapp;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        SystemBarInsets.apply(getWindow(), findViewById(R.id.settingsRoot));

        EditText urlInput = findViewById(R.id.webhookUrlEditText);
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
    }
}
