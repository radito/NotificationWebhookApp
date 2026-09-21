# Notification2Webhook

An Android app that forwards notifications from selected apps to a webhook. Notification text that contains a JSON object or array is sent unchanged, which is useful for services that put a JSON webhook body in a notification. Other text is sent as a JSON object with `package`, `title`, and `text` fields.

## Requirements

- Android 12 (API 31) or newer
- Notification listener access
- A reachable HTTP or HTTPS webhook URL

## Setup

1. Install the APK and open the app.
2. Tap **Settings**, enter your webhook URL, and save it. HTTPS is recommended because HTTP exposes notification contents in transit.
3. Search by app name or package ID, select the apps to monitor, and tap **Save Apps**. System apps are included in the list.
4. Tap **Grant Notification Access** and grant Notification2Webhook notification access in Android settings. Return to the app and check that the status says access is enabled.
5. Leave **Forward notifications** on. Turn it off to stop forwarding and cancel queued deliveries. A request already in flight may finish.

Only notifications from saved app selections are queued. Notifications without text are skipped. Delivery runs as Android background work, so it may be delayed. Temporary network errors and HTTP 408, 429, or 5xx responses are retried up to five attempts. HTTP 4xx responses other than 408 and 429 are treated as permanent failures. A webhook may receive a duplicate if the app is interrupted after the server accepts a request but before Android records its completion.

The webhook URL and app selections are stored in private app preferences. Pending webhook bodies are stored in private app storage until delivery finishes, and app data is excluded from backup. The app does not log notification contents.

The app requests broad package visibility so the selection list can include notification sources without launcher icons. Google Play restricts this permission and requires a [permission declaration](https://support.google.com/googleplay/android-developer/answer/10158779) if you distribute the app there.

## Build

Open the project in Android Studio, or install Android SDK Platform 35 and run:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. The [GitHub Actions workflow](.github/workflows/android.yml) runs on pushes, pull requests, and manual dispatch; each successful run uploads the debug APK as a workflow artifact. The artifact is a debug build, not a signed release.

## Troubleshooting

- If the status says notification access is disabled, enable the listener in Android settings.
- If nothing is forwarded, confirm the app selection was saved, the forwarding switch is on, and the webhook URL is reachable from the device.
- Logcat reports webhook HTTP status codes and delivery errors under `WebhookWorker`. It does not print notification bodies or the URL.
- HTTP webhooks are supported. A server error, invalid URL, or background network restriction can still prevent delivery.

## License

MIT; see [LICENSE](LICENSE).
