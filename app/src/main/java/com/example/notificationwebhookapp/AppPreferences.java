package com.example.notificationwebhookapp;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

final class AppPreferences {
    private static final String PREFS_NAME = "NotificationWebhookPrefs";
    private static final String SELECTED_APPS = "SelectedApps";
    private static final String WEBHOOK_URL = "webhookUrl";
    private static final String FORWARDING_ENABLED = "forwardingEnabled";

    private AppPreferences() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    static Set<String> getSelectedApps(Context context) {
        SharedPreferences preferences = prefs(context);
        Object stored = preferences.getAll().get(SELECTED_APPS);
        Set<String> selected = parseSelectedApps(stored);
        if (stored instanceof String) {
            preferences.edit().putStringSet(SELECTED_APPS, selected).apply();
        }
        return selected;
    }

    static Set<String> parseSelectedApps(Object stored) {
        Set<String> selected = new HashSet<>();
        if (stored instanceof Set<?>) {
            for (Object item : (Set<?>) stored) {
                if (item instanceof String && !((String) item).isEmpty()) {
                    selected.add((String) item);
                }
            }
        } else if (stored instanceof String && !((String) stored).isEmpty()) {
            Arrays.stream(((String) stored).split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .forEach(selected::add);
        }
        return selected;
    }

    static void setSelectedApps(Context context, Set<String> selected) {
        prefs(context).edit().putStringSet(SELECTED_APPS, new HashSet<>(selected)).apply();
    }

    static String getWebhookUrl(Context context) {
        return prefs(context).getString(WEBHOOK_URL, "");
    }

    static void setWebhookUrl(Context context, String url) {
        prefs(context).edit().putString(WEBHOOK_URL, url).apply();
    }

    static boolean isForwardingEnabled(Context context) {
        return prefs(context).getBoolean(FORWARDING_ENABLED, true);
    }

    static void setForwardingEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(FORWARDING_ENABLED, enabled).apply();
    }
}
