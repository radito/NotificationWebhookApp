package com.example.notificationwebhookapp;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;
import okhttp3.Response;

public class WebhookWorker extends Worker {
    static final String WORK_TAG = "webhook_delivery";
    static final String KEY_ID = "id";
    static final String KEY_URL = "url";
    private static final String TAG = "WebhookWorker";
    private static final int MAX_ATTEMPTS = 5;
    public WebhookWorker(@NonNull Context context, @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }

    @NonNull
    @Override
    public Result doWork() {
        String id = getInputData().getString(KEY_ID);
        String url = getInputData().getString(KEY_URL);
        if (id == null || url == null) {
            return Result.failure();
        }
        if (!AppPreferences.isForwardingEnabled(getApplicationContext())) {
            PendingWebhookStore.delete(getApplicationContext(), id);
            return Result.success();
        }
        try {
            String body = PendingWebhookStore.read(getApplicationContext(), id);
            try (Response response = WebhookClient.newCall(url, body).execute()) {
                if (response.isSuccessful()) {
                    PendingWebhookStore.delete(getApplicationContext(), id);
                    return Result.success();
                }
                int status = response.code();
                if (status == 408 || status == 429 || status >= 500) {
                    Log.w(TAG, "Webhook returned retryable HTTP " + status);
                    return retryOrFail(id);
                }
                Log.w(TAG, "Webhook returned HTTP " + status);
                PendingWebhookStore.delete(getApplicationContext(), id);
                return Result.failure();
            }
        } catch (IOException error) {
            Log.w(TAG, "Webhook delivery failed", error);
            return retryOrFail(id);
        } catch (IllegalArgumentException error) {
            Log.e(TAG, "Invalid queued webhook", error);
            PendingWebhookStore.delete(getApplicationContext(), id);
            return Result.failure();
        }
    }

    private Result retryOrFail(String id) {
        if (getRunAttemptCount() + 1 < MAX_ATTEMPTS) {
            return Result.retry();
        }
        PendingWebhookStore.delete(getApplicationContext(), id);
        Log.e(TAG, "Webhook delivery abandoned after " + MAX_ATTEMPTS + " attempts");
        return Result.failure();
    }
}
