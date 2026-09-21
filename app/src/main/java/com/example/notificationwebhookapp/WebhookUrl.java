package com.example.notificationwebhookapp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

final class WebhookUrl {
    private WebhookUrl() {}

    static String normalize(String input) {
        String url = input == null ? "" : input.trim();
        if (url.length() > 2048) {
            throw new IllegalArgumentException("Webhook URL is too long.");
        }
        try {
            URI parsed = new URI(url);
            String scheme = parsed.getScheme();
            if (scheme != null
                    && ("http".equals(scheme.toLowerCase(Locale.ROOT))
                    || "https".equals(scheme.toLowerCase(Locale.ROOT)))
                    && parsed.getHost() != null
                    && parsed.getUserInfo() == null
                    && parsed.getFragment() == null
                    && parsed.getPort() <= 65535) {
                return url;
            }
        } catch (URISyntaxException ignored) {
            // Show one validation message for all invalid URLs.
        }
        throw new IllegalArgumentException("Enter a valid http:// or https:// URL.");
    }
}
