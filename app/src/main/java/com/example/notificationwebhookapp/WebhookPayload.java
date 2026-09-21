package com.example.notificationwebhookapp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

final class WebhookPayload {
    private WebhookPayload() {}

    static String fromNotification(String packageName, String title, String text) {
        try {
            JSONTokener parser = new JSONTokener(text);
            Object parsed = parser.nextValue();
            if ((parsed instanceof JSONObject || parsed instanceof JSONArray)
                    && parser.nextClean() == 0) {
                return text;
            }
        } catch (JSONException ignored) {
            // Plain text is wrapped as a JSON object below.
        }
        try {
            return new JSONObject()
                    .put("package", packageName)
                    .put("title", title)
                    .put("text", text)
                    .toString();
        } catch (JSONException error) {
            throw new IllegalStateException("Could not encode notification", error);
        }
    }
}
