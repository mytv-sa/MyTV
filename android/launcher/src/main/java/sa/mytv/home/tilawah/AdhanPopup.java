package sa.mytv.home.tilawah;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * The silent way of the adhan («تنبيه بإشعار»): a card in the middle of the screen, over whatever app is open, for 40 seconds,
 * with «إغلاق». No sound at all. It is a system overlay, so it needs «الظهور فوق التطبيقات» (SYSTEM_ALERT_WINDOW), which
 * optimize/install.sh grants over ADB; without it a toast says the prayer's name instead. While the card is up the remote
 * works the card (OK or Back closes it), not the app behind.
 *
 * The same card, with other words, is the Friday reminder two hours before the Jumu'ah (JumuahScheduler).
 */
final class AdhanPopup {
    static final int SECONDS = 40;
    /** the sunnahs of Friday, under the reminder's time */
    static final String JUMUAH_SUNNAHS = "الاغتسال والطِّيب · التبكير إلى المسجد · قراءة سورة الكهف";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static View shown;   // one card at a time
    private static Runnable ticker;

    private AdhanPopup() {}

    /** due: the prayer's moment (0: the one nearest to now) */
    static void show(Context context, final long due) {
        final Context c = context.getApplicationContext();
        MAIN.post(() -> open(c, due));
    }

    /** the Friday reminder; dhuhr: the Jumu'ah's moment (0: today's Dhuhr) */
    static void showJumuah(Context context, final long dhuhr) {
        final Context c = context.getApplicationContext();
        MAIN.post(() -> {
            PrayerTimes t = PrayerTimes.today(c);
            long at = dhuhr > 0 ? dhuhr : t.at[PrayerTimes.DHUHR];
            // the time left as it is now, not «ساعتين» always: an alarm Android delivered late says so. A Jumu'ah already past (only
            // music.sh jumuah now, after Dhuhr) reads as the reminder would: two hours before
            long left = at - System.currentTimeMillis();
            String title = "صلاة الجمعة " + PrayerTimes.remaining(left > 60_000 ? left : JumuahScheduler.BEFORE);
            present(c, "تذكير الجمعة", title, "الأذان " + PrayerTimes.clock12(t.clock[PrayerTimes.DHUHR]), JUMUAH_SUNNAHS);
        });
    }

    private static void open(final Context c, long due) {
        PrayerTimes t = PrayerTimes.today(c);
        int prayer = nearest(t, due > 0 ? due : System.currentTimeMillis());
        boolean jumuah = prayer == PrayerTimes.DHUHR
                && Calendar.getInstance(TimeZone.getTimeZone(Place.get(c).tz), Locale.US).get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
        String name = jumuah ? "الجمعة" : PrayerTimes.NAMES[prayer];
        present(c, "تنبيه الصلاة", "حان الآن وقت صلاة " + name, PrayerTimes.clock12(t.clock[prayer]), null);
    }

    /** the card: a small gold label, the title, the time, and an optional line under it (note) */
    private static void present(final Context c, String label, String title, String time, String note) {
        close(c);
        if (!canOverlay(c)) {   // no permission for a card over the apps: at least the words
            Toast.makeText(c, title, Toast.LENGTH_LONG).show();
            return;
        }
        float d = c.getResources().getDisplayMetrics().density;

        final FrameLayout root = new FrameLayout(c);
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        int pad = Math.round(30 * d);
        card.setPadding(pad, Math.round(24 * d), pad, Math.round(20 * d));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF2081A11);
        bg.setCornerRadius(22 * d);
        bg.setStroke(Math.round(2 * d), 0xFFD6AF6B);
        card.setBackground(bg);

        card.addView(line(c, label, "Thmanyah-Medium", 15, 0xFFD6AF6B));
        TextView big = line(c, Ui.digits(title), "ThmanyahDisplay-Bold", 30, 0xFFF6EFE0);
        big.setPadding(0, Math.round(6 * d), 0, Math.round(3 * d));
        card.addView(big);
        card.addView(line(c, Ui.digits(time), "Thmanyah-Regular", 18, 0xFFC6BBA4));
        if (note != null) {
            TextView sub = line(c, note, "Thmanyah-Regular", 15, 0xFFE3C68E);
            sub.setPadding(0, Math.round(10 * d), 0, 0);
            card.addView(sub);
        }
        final TextView left = line(c, "", "Thmanyah-Regular", 13, 0xFFC6BBA4);
        left.setPadding(0, Math.round(10 * d), 0, Math.round(12 * d));
        card.addView(left);

        final TextView closeBtn = line(c, "إغلاق", "Thmanyah-Medium", 18, 0xFF1A1A12);
        closeBtn.setPadding(Math.round(34 * d), Math.round(6 * d), Math.round(34 * d), Math.round(9 * d));
        final GradientDrawable btn = new GradientDrawable();
        btn.setCornerRadius(999 * d);
        btn.setColor(0xFFD6AF6B);
        closeBtn.setBackground(btn);
        closeBtn.setFocusable(true);
        closeBtn.setClickable(true);
        closeBtn.setOnClickListener(v -> close(c));
        closeBtn.setOnKeyListener((v, code, e) -> {   // the button always has the focus: Back closes the card like «إغلاق»
            if (code != KeyEvent.KEYCODE_BACK) return false;
            if (e.getAction() == KeyEvent.ACTION_UP) close(c);
            return true;
        });
        closeBtn.setOnFocusChangeListener((v, has) -> btn.setStroke(Math.round(3 * d), has ? 0xFFFFF3D6 : 0x00000000));
        card.addView(closeBtn);

        root.addView(card, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.dimAmount = 0.35f;
        lp.gravity = Gravity.CENTER;
        try {
            wm(c).addView(root, lp);
        } catch (RuntimeException e) {   // the system refused the overlay after all
            Toast.makeText(c, title, Toast.LENGTH_LONG).show();
            return;
        }
        shown = root;
        closeBtn.requestFocus();

        final long until = System.currentTimeMillis() + SECONDS * 1000L;
        ticker = new Runnable() {
            @Override public void run() {
                long s = (until - System.currentTimeMillis() + 999) / 1000;
                if (s <= 0) { close(c); return; }
                left.setText(Ui.digits("تُغلق بعد " + s + " ثانية"));
                MAIN.postDelayed(this, 1000);
            }
        };
        ticker.run();
    }

    static void close(Context c) {
        if (ticker != null) { MAIN.removeCallbacks(ticker); ticker = null; }
        if (shown == null) return;
        try { wm(c).removeView(shown); } catch (RuntimeException ignored) { }
        shown = null;
    }

    static boolean canOverlay(Context c) {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(c);
    }

    /** the prayer whose moment is nearest to this one */
    private static int nearest(PrayerTimes t, long at) {
        int best = PrayerTimes.PRAYERS[0];
        for (int p : PrayerTimes.PRAYERS) if (Math.abs(t.at[p] - at) < Math.abs(t.at[best] - at)) best = p;
        return best;
    }

    private static WindowManager wm(Context c) { return (WindowManager) c.getSystemService(Context.WINDOW_SERVICE); }

    private static TextView line(Context c, String text, String font, int sp, int color) {
        TextView v = new TextView(c);
        v.setText(text);
        v.setTypeface(Ui.font(c, font + ".otf"));
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        v.setTextColor(color);
        v.setGravity(Gravity.CENTER);
        return v;
    }
}
