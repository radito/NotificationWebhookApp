package com.example.notificationwebhookapp;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.work.WorkManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private final List<AppInfo> installedApps = new ArrayList<>();
    private AppListAdapter adapter;
    private TextView listenerStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView appList = findViewById(R.id.appList);
        adapter = new AppListAdapter();
        appList.setAdapter(adapter);
        listenerStatus = findViewById(R.id.listenerStatus);

        findViewById(R.id.settingsButton).setOnClickListener(
                view -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.saveAppsButton).setEnabled(false);
        findViewById(R.id.saveAppsButton).setOnClickListener(view -> saveSelectedApps());
        findViewById(R.id.enableNotificationsButton).setOnClickListener(
                view -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));

        SwitchCompat forwardingSwitch = findViewById(R.id.forwardingSwitch);
        forwardingSwitch.setChecked(AppPreferences.isForwardingEnabled(this));
        forwardingSwitch.setOnCheckedChangeListener((button, enabled) -> {
            AppPreferences.setForwardingEnabled(this, enabled);
            if (!enabled) {
                WorkManager.getInstance(this).cancelAllWorkByTag(WebhookWorker.WORK_TAG);
                PendingWebhookStore.clear(this);
            }
        });

        loadInstalledApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean enabled = isNotificationServiceEnabled();
        listenerStatus.setText(enabled ? R.string.listener_enabled : R.string.listener_disabled);
    }

    private void loadInstalledApps() {
        PackageManager packageManager = getPackageManager();
        Set<String> selected = AppPreferences.getSelectedApps(this);
        new Thread(() -> {
            List<AppInfo> apps = new ArrayList<>();
            for (ApplicationInfo application : packageManager.getInstalledApplications(0)) {
                try {
                    String name = application.loadLabel(packageManager).toString();
                    Drawable icon = application.loadIcon(packageManager);
                    apps.add(new AppInfo(name, application.packageName, icon,
                            selected.contains(application.packageName)));
                } catch (RuntimeException ignored) {
                    // An app may disappear while the installed-app list is loading.
                }
            }
            apps.sort(Comparator.comparing(app -> app.name, String.CASE_INSENSITIVE_ORDER));
            runOnUiThread(() -> {
                installedApps.clear();
                installedApps.addAll(apps);
                adapter.notifyDataSetChanged();
                findViewById(R.id.saveAppsButton).setEnabled(true);
            });
        }).start();
    }

    private void saveSelectedApps() {
        Set<String> selected = new HashSet<>();
        for (AppInfo app : installedApps) {
            if (app.selected) {
                selected.add(app.packageName);
            }
        }
        AppPreferences.setSelectedApps(this, selected);
        Toast.makeText(this, R.string.apps_saved, Toast.LENGTH_SHORT).show();
    }

    private boolean isNotificationServiceEnabled() {
        String listeners = Settings.Secure.getString(
                getContentResolver(), "enabled_notification_listeners");
        if (listeners == null) {
            return false;
        }
        ComponentName ownService = new ComponentName(this, NotificationListener.class);
        for (String flattened : listeners.split(":")) {
            if (ownService.equals(ComponentName.unflattenFromString(flattened))) {
                return true;
            }
        }
        return false;
    }

    private static class AppInfo {
        final String name;
        final String packageName;
        final Drawable icon;
        boolean selected;

        AppInfo(String name, String packageName, Drawable icon, boolean selected) {
            this.name = name;
            this.packageName = packageName;
            this.icon = icon;
            this.selected = selected;
        }
    }

    private class AppListAdapter extends BaseAdapter {
        @Override public int getCount() { return installedApps.size(); }
        @Override public AppInfo getItem(int position) { return installedApps.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View recycled, ViewGroup parent) {
            View row = recycled == null
                    ? LayoutInflater.from(parent.getContext()).inflate(R.layout.app_list_item, parent, false)
                    : recycled;
            AppInfo app = getItem(position);
            ((ImageView) row.findViewById(R.id.appIcon)).setImageDrawable(app.icon);
            ((TextView) row.findViewById(R.id.appName)).setText(app.name);
            CheckBox checkBox = row.findViewById(R.id.appCheckbox);
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(app.selected);
            checkBox.setOnCheckedChangeListener((button, checked) -> app.selected = checked);
            row.setOnClickListener(view -> checkBox.setChecked(!checkBox.isChecked()));
            return row;
        }
    }
}
