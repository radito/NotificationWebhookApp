package com.example.notificationwebhookapp;

import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

final class WebhookClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .callTimeout(20, TimeUnit.SECONDS)
            .build();

    private WebhookClient() {}

    static Request request(String url, String body) {
        return new Request.Builder()
                .url(WebhookUrl.normalize(url))
                .post(RequestBody.create(body, JSON))
                .build();
    }

    static Call newCall(String url, String body) {
        return CLIENT.newCall(request(url, body));
    }
}
