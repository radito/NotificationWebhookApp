package com.example.notificationwebhookapp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

final class WebhookPayload {
    private WebhookPayload() {}

    static String fromNotification(String packageName, String title, String text,
            String deviceId, String deviceModel, String androidVersion) {
        Object parsed = null;
        try {
            JSONTokener parser = new JSONTokener(text);
            Object value = parser.nextValue();
            if ((value instanceof JSONObject || value instanceof JSONArray)
                    && parser.nextClean() == 0) {
                parsed = value;
            }
        } catch (JSONException ignored) {
            // Invalid JSON is sent as notification text below.
        }
        try {
            JSONObject payload = parsed instanceof JSONObject ? (JSONObject) parsed : new JSONObject();
            if (payload.isNull("package")) payload.put("package", packageName);
            if (payload.isNull("title")) payload.put("title", title);
            if (payload.isNull("text")) payload.put("text", text);
            for (String key : new String[] {"title", "text"}) {
                Object value = payload.opt(key);
                if (value != null && !(value instanceof String)) {
                    payload.put(key, value.toString());
                }
            }
            if (parsed instanceof JSONArray) payload.put("data", parsed);
            return payload.put("deviceId", deviceId)
                    .put("deviceModel", deviceModel)
                    .put("androidVersion", androidVersion)
                    .toString();
        } catch (JSONException error) {
            throw new IllegalStateException("Could not encode notification", error);
        }
    }
}
