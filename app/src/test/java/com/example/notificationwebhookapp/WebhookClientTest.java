package com.example.notificationwebhookapp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import okhttp3.Request;
import okio.Buffer;

public class WebhookClientTest {
    @Test
    public void createsJsonPostRequest() throws Exception {
        Request request = WebhookClient.request("https://example.com/notify", "{\"test\":true}");

        assertEquals("POST", request.method());
        assertEquals("application/json; charset=utf-8", request.body().contentType().toString());
        Buffer body = new Buffer();
        request.body().writeTo(body);
        assertEquals("{\"test\":true}", body.readUtf8());
    }
}
