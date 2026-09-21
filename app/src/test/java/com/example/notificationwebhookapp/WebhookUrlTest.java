package com.example.notificationwebhookapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class WebhookUrlTest {
    @Test
    public void acceptsHttpsAndExistingHttpWebhooks() {
        assertEquals("https://example.com/hook?a=1", WebhookUrl.normalize(
                "  https://example.com/hook?a=1  "));
        assertEquals("http://192.0.2.1:8080/hook", WebhookUrl.normalize(
                "http://192.0.2.1:8080/hook"));
    }

    @Test
    public void rejectsMalformedAndUnsafeUrls() {
        assertThrows(IllegalArgumentException.class, () -> WebhookUrl.normalize(""));
        assertThrows(IllegalArgumentException.class, () -> WebhookUrl.normalize("file:///tmp/hook"));
        assertThrows(IllegalArgumentException.class, () -> WebhookUrl.normalize("https://u:p@example.com/hook"));
        assertThrows(IllegalArgumentException.class, () -> WebhookUrl.normalize("https://example.com/hook#fragment"));
        assertThrows(IllegalArgumentException.class, () -> WebhookUrl.normalize("http://not a host"));
    }
}
