package sa.mytv.home.tilawah;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * «تذكير الجمعة»: one alarm, two hours before the next Friday's Dhuhr (the Jumu'ah), that shows AdhanPopup's Friday card.
 * Its own alarm (another request code and action), apart from the adhan's: it works with the adhan off too. Set again with the
 * adhan's alarm (AdhanScheduler.schedule calls it), so a reboot, a clock change or new times from Aladhan move it as well.
 */
final class JumuahScheduler {
    static final String ACTION = "sa.mytv.home.JUMUAH_ALARM";
    static final long BEFORE = 2 * 3_600_000L;
    private static final int REQUEST = 48;

    private JumuahScheduler() {}

    static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        am.cancel(pending(c, 0));
        if (!new Prefs(c).jumuahOn()) return;
        long dhuhr = nextJumuah(c);
        if (dhuhr == 0) return;
        PendingIntent pending = pending(c, dhuhr);
        long at = dhuhr - BEFORE;
        try {
            if (!AdhanScheduler.exactAllowed(c)) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending);
            else if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending);
            else am.setExact(AlarmManager.RTC_WAKEUP, at, pending);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending);
        }
    }

    /** The Dhuhr of the next Friday whose reminder is still ahead (today's, before 2 hours ahead of it); 0 if none is found. */
    static long nextJumuah(Context c) {
        long now = System.currentTimeMillis();
        Place place = Place.get(c);
        Calendar day = Calendar.getInstance(TimeZone.getTimeZone(place.tz), Locale.US);
        for (int d = 0; d < 8; d++) {
            if (day.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
                PrayerTimes t = d == 0 ? PrayerTimes.today(c) : PrayerTimes.of(day, place);   // today's may be Aladhan's
                long dhuhr = t.at[PrayerTimes.DHUHR];
                if (dhuhr - BEFORE > now + 30_000) return dhuhr;
            }
            day.add(Calendar.DAY_OF_MONTH, 1);
        }
        return 0;
    }

    private static PendingIntent pending(Context c, long dhuhr) {
        Intent intent = new Intent(c, AdhanReceiver.class).setAction(ACTION).putExtra(AdhanScheduler.EXTRA_DUE, dhuhr);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c, REQUEST, intent, flags);
    }
}
