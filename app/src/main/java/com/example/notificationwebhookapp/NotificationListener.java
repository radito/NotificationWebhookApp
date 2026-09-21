package com.example.notificationwebhookapp;

import android.app.Notification;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class NotificationListener extends NotificationListenerService {
    private static final String TAG = "NotificationListener";

    @Override
    public void onNotificationPosted(StatusBarNotification posted) {
        if (!AppPreferences.isForwardingEnabled(this)) {
            return;
        }
        Set<String> selectedApps = AppPreferences.getSelectedApps(this);
        if (!selectedApps.contains(posted.getPackageName())) {
            return;
        }
        String url;
        try {
            url = WebhookUrl.normalize(AppPreferences.getWebhookUrl(this));
        } catch (IllegalArgumentException error) {
            Log.w(TAG, "Webhook URL is missing or invalid");
            return;
        }
        Notification notification = posted.getNotification();
        if (notification == null || notification.extras == null) {
            return;
        }
        CharSequence content = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (content == null) {
            content = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
        }
        if (content == null || content.length() == 0) {
            return;
        }
        CharSequence title = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
        String body = WebhookPayload.fromNotification(
                posted.getPackageName(), title == null ? "" : title.toString(), content.toString(),
                AppPreferences.getDeviceId(this), Build.MANUFACTURER + " " + Build.MODEL,
                Build.VERSION.RELEASE);
        String id = null;
        try {
            id = PendingWebhookStore.write(this, body);
            Data data = new Data.Builder()
                    .putString(WebhookWorker.KEY_ID, id)
                    .putString(WebhookWorker.KEY_URL, url)
                    .build();
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(WebhookWorker.class)
                    .setInputData(data)
                    .setConstraints(new Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .addTag(WebhookWorker.WORK_TAG)
                    .build();
            WorkManager.getInstance(this).enqueue(request);
        } catch (IOException | IllegalStateException error) {
            if (id != null) {
                PendingWebhookStore.delete(this, id);
            }
            Log.e(TAG, "Could not queue webhook", error);
        }
    }
}
