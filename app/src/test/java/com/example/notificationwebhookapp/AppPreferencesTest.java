package com.example.notificationwebhookapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

public class AppPreferencesTest {
    @Test
    public void migratesLegacyCommaSeparatedSelection() {
        assertEquals(new HashSet<>(Arrays.asList("app.one", "app.two")),
                AppPreferences.parseSelectedApps("app.one, app.two,,"));
    }

    @Test
    public void acceptsSavedEmptySetWithoutTypeConversion() {
        assertTrue(AppPreferences.parseSelectedApps(new HashSet<String>()).isEmpty());
    }

    @Test
    public void copiesSavedSet() {
        HashSet<String> stored = new HashSet<>(Arrays.asList("app.one"));
        HashSet<String> selected = new HashSet<>(AppPreferences.parseSelectedApps(stored));
        selected.add("app.two");
        assertEquals(new HashSet<>(Arrays.asList("app.one")), stored);
    }
}
