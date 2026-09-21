package com.example.notificationwebhookapp;

import static org.junit.Assert.assertEquals;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class WebhookPayloadTest {
    private static JSONObject encode(String text) throws Exception {
        return new JSONObject(WebhookPayload.fromNotification(
                "app.example", "Notification title", text,
                "installation-1", "Example Phone", "15"));
    }

    @Test
    public void plainTextIncludesEveryServerField() throws Exception {
        JSONObject payload = encode("Hello");
        assertEquals("app.example", payload.getString("package"));
        assertEquals("Notification title", payload.getString("title"));
        assertEquals("Hello", payload.getString("text"));
        assertEquals("installation-1", payload.getString("deviceId"));
        assertEquals("Example Phone", payload.getString("deviceModel"));
        assertEquals("15", payload.getString("androidVersion"));
    }

    @Test
    public void jsonObjectKeepsFieldsAndFillsMissingServerFields() throws Exception {
        JSONObject payload = encode("{\"title\":\"Alert\",\"extra\":42}");
        assertEquals("Alert", payload.getString("title"));
        assertEquals(42, payload.getInt("extra"));
        assertEquals("app.example", payload.getString("package"));
        assertEquals("{\"title\":\"Alert\",\"extra\":42}", payload.getString("text"));
        assertEquals("installation-1", payload.getString("deviceId"));
    }

    @Test
    public void jsonArrayRemainsReadableAndStructured() throws Exception {
        JSONObject payload = encode("[\"first\",\"second\"]");
        assertEquals("[\"first\",\"second\"]", payload.getString("text"));
        JSONArray data = payload.getJSONArray("data");
        assertEquals("first", data.getString(0));
        assertEquals("second", data.getString(1));
    }

    @Test
    public void malformedJsonIsForwardedAsText() throws Exception {
        JSONObject payload = encode("{broken");
        assertEquals("{broken", payload.getString("text"));
    }

    @Test
    public void nestedTextIsReadableByNodeFormatter() throws Exception {
        JSONObject payload = encode("{\"text\":{\"message\":\"Hello\"}}");
        assertEquals("{\"message\":\"Hello\"}", payload.getString("text"));
    }
}
