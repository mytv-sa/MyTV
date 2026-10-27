package sa.mytv.screensaver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * «تحديث التطبيق» in the settings: both apps of the newest release (v3.N on this repository) are downloaded here, checked
 * (whole, the right package, the right version), and handed to Android's installer, which asks for «تثبيت» once per app.
 * The home app goes first: installing this app ends its own process. An app already at that version is skipped.
 * Needs «تثبيت تطبيقات غير معروفة» for this app, which optimize/install.sh grants over ADB (REQUEST_INSTALL_PACKAGES).
 */
final class AppUpdate {
    static final String DOWNLOAD = "https://github.com/mytv-sa/MyTV/releases/download/v";
    /** package, file of the release; in the order they are installed */
    private static final String[][] APPS = {{"sa.mytv.home", "mytv-home.apk"}, {"sa.mytv.screensaver", "mytv-screensaver.apk"}};
    static final String ACTION_STATUS = "sa.mytv.screensaver.INSTALL_STATUS";

    interface Ui { void say(String line); }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static boolean running;
    private static List<File> queue = new ArrayList<>();
    private static Ui ui;

    private AppUpdate() { }

    static boolean running() { return running; }

    static boolean allowed(Context c) {
        return Build.VERSION.SDK_INT < 26 || c.getPackageManager().canRequestPackageInstalls();
    }

    /** version: "3.8", the newest release. say: the progress, in Arabic with Latin digits (the caller converts), on the main thread. */
    static void start(final Context context, final String version, Ui say) {
        if (running) return;
        running = true;
        ui = say;
        final Context c = context.getApplicationContext();
        new Thread(() -> {
            List<File> files = new ArrayList<>();
            try {
                for (String[] app : APPS) {
                    if (version.equals(installed(c, app[0]))) continue;   // this one is new already
                    File f = new File(c.getCacheDir(), app[1]);
                    download(DOWNLOAD + version + "/" + app[1], f, app[0].endsWith("home") ? "الواجهة" : "شاشة التوقف");
                    PackageInfo p = c.getPackageManager().getPackageArchiveInfo(f.getAbsolutePath(), 0);
                    if (p == null || !app[0].equals(p.packageName) || !version.equals(p.versionName))
                        throw new Exception("الملف المحمّل غير صحيح");
                    files.add(f);
                }
            } catch (Exception e) {
                done("تعذّر التحميل: " + (e.getMessage() == null ? "لا يوجد اتصال" : e.getMessage()));
                return;
            }
            MAIN.post(() -> {
                if (files.isEmpty()) { done("التطبيقان على هذا الإصدار"); return; }
                queue = files;
                next(c);
            });
        }, "app-update").start();
    }

    /** the next app to Android's installer; the answer comes to Status */
    private static void next(Context c) {
        if (queue.isEmpty()) { done("تم التحديث"); return; }
        File f = queue.remove(0);
        say("اضغط «تثبيت» في النافذة…");
        try {
            PackageInstaller pi = c.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            int id = pi.createSession(params);
            try (PackageInstaller.Session s = pi.openSession(id)) {
                try (InputStream in = new FileInputStream(f); OutputStream out = s.openWrite(f.getName(), 0, f.length())) {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    s.fsync(out);
                }
                Intent back = new Intent(c, Status.class).setAction(ACTION_STATUS);
                int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
                s.commit(PendingIntent.getBroadcast(c, id, back, flags).getIntentSender());
            }
        } catch (Exception e) {
            done("تعذّر التثبيت");
        }
    }

    /** Android's installer answers here: ask the person, go on to the next app, or stop with why. */
    public static final class Status extends BroadcastReceiver {
        @Override public void onReceive(Context c, Intent i) {
            int status = i.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
            if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                Intent confirm = i.getParcelableExtra(Intent.EXTRA_INTENT);
                if (confirm != null) c.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } else if (status == PackageInstaller.STATUS_SUCCESS) {
                next(c.getApplicationContext());
            } else {
                done(status == PackageInstaller.STATUS_FAILURE_ABORTED ? "أُلغي التثبيت" : "تعذّر التثبيت");
                Toast.makeText(c, status == PackageInstaller.STATUS_FAILURE_ABORTED ? "أُلغي التثبيت" : "تعذّر التثبيت", Toast.LENGTH_LONG).show();
            }
        }
    }

    private static void download(String url, File to, String which) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();   // GitHub sends it on to its file host
        con.setConnectTimeout(15000);
        con.setReadTimeout(30000);
        try {
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) throw new Exception("الإصدار غير موجود (" + con.getResponseCode() + ")");
            long size = con.getContentLengthLong(), got = 0;
            int shown = -1;
            try (InputStream in = con.getInputStream(); OutputStream out = new FileOutputStream(to)) {
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    got += n;
                    int pct = size > 0 ? (int) (got * 100 / size) : -1;
                    if (pct != shown && pct % 5 == 0) { shown = pct; say("جارٍ تحميل " + which + " " + pct + "٪"); }
                }
            }
            if (size > 0 && got != size) throw new Exception("انقطع التحميل");
        } finally {
            con.disconnect();
        }
    }

    private static String installed(Context c, String pkg) {
        try { return c.getPackageManager().getPackageInfo(pkg, 0).versionName; }
        catch (PackageManager.NameNotFoundException e) { return null; }
    }

    private static void say(final String line) {
        MAIN.post(() -> { if (ui != null) ui.say(line); });
    }

    private static void done(final String line) {
        MAIN.post(() -> {
            running = false;
            queue = new ArrayList<>();
            if (ui != null) ui.say(line);
        });
    }
}
