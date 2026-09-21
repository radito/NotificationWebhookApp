package com.example.notificationwebhookapp;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

final class PendingWebhookStore {
    private static final String DIRECTORY = "pending_webhooks";

    private PendingWebhookStore() {}

    private static File directory(Context context) {
        return new File(context.getNoBackupFilesDir(), DIRECTORY);
    }

    private static File file(Context context, String id) {
        UUID.fromString(id);
        return new File(directory(context), id + ".json");
    }

    static String write(Context context, String body) throws IOException {
        File directory = directory(context);
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Could not create webhook queue");
        }
        String id = UUID.randomUUID().toString();
        File temporary = new File(directory, id + ".tmp");
        File target = file(context, id);
        try (FileOutputStream output = new FileOutputStream(temporary)) {
            output.write(body.getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
        } catch (IOException error) {
            temporary.delete();
            throw error;
        }
        if (!temporary.renameTo(target)) {
            temporary.delete();
            throw new IOException("Could not save webhook");
        }
        return id;
    }

    static String read(Context context, String id) throws IOException {
        return new String(Files.readAllBytes(file(context, id).toPath()), StandardCharsets.UTF_8);
    }

    static void delete(Context context, String id) {
        file(context, id).delete();
    }

    static void clear(Context context) {
        File[] files = directory(context).listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }
}
