package sa.mytv.home.tilawah;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

/**
 * Plays the adhan when its alarm goes off — pausing a surah that is playing and resuming it after — or, in the silent mode
 * («تنبيه بإشعار»), shows AdhanPopup instead; and sets the next alarm again after a reboot, a clock change or an update.
 *
 * The adhan only starts while the TV is on: an alarm that arrives in standby is skipped, and none is
 * made up after a reboot; one that is playing stops when the screen goes off. (Without the exact-alarm
 * permission Android may deliver the alarm itself minutes late; the adhan then plays when it arrives.) One later than LATE,
 * or a Friday reminder outside its two hours, is dropped: after a power cut the clock jumps two years and every alarm
 * set on the old clock arrives at once (SystemTime).
 */
public class AdhanReceiver extends BroadcastReceiver {
    /** an alarm without its prayer time (one set by an older version): the same adhan never plays twice within this */
    private static final long DUPLICATE = 5 * 60_000L;
    /** an alarm this late is not shown or played: Android delivers past alarms at once after the clock jumps (see SystemTime) */
    static final long LATE = 10 * 60_000L;

    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        if (!sa.mytv.home.SystemTime.isSet()) return;   // the TV's clock is not set yet: TIME_SET comes with the right one
        Prefs prefs = new Prefs(context);
        long now = System.currentTimeMillis();
        if (JumuahScheduler.ACTION.equals(intent.getAction())) {   // «تذكير الجمعة», two hours before; the TV must be on, as for the adhan
            long dhuhr = intent.getLongExtra(AdhanScheduler.EXTRA_DUE, 0);
            if (prefs.jumuahOn() && screenOn(context) && jumuahInTime(dhuhr, now) && prefs.jumuahDue() != dhuhr) {
                prefs.setJumuahDue(dhuhr);
                AdhanPopup.showJumuah(context, dhuhr);
            }
            JumuahScheduler.schedule(context);
            return;
        }
        boolean alarm = AdhanScheduler.ACTION.equals(intent.getAction());

        if (!alarm) {
            // boot, clock or timezone change, or the app was just updated: set the next alarm, and ask Aladhan for today
            AdhanScheduler.schedule(context);
            PrayerTimes.refresh(context, null);
            return;
        }

        long due = intent.getLongExtra(AdhanScheduler.EXTRA_DUE, 0);
        if (prefs.adhanOn() && screenOn(context) && adhanInTime(due, now) && !alreadyPlayed(prefs, due, now)) {
            if (due > 0) prefs.setLastDue(due); else prefs.setLastAlarmAt(now);
            if (Prefs.MODE_POPUP.equals(prefs.adhanMode())) AdhanPopup.show(context, due);   // «تنبيه بإشعار»: the card, no sound
            else MusicService.adhan(context, prefs.playVoice(context), null, 0);
        }
        AdhanScheduler.schedule(context);
        PrayerTimes.refresh(context, null);
    }

    /**
     * The alarm for this prayer time already played. Judged by the prayer's own time, not by when something last
     * played: a preview of the muezzin (a tap on a prayer in «تلاوة») five minutes before must not silence the real adhan.
     */
    private static boolean alreadyPlayed(Prefs prefs, long due, long now) {
        return due > 0 ? prefs.lastDue() == due : now - prefs.lastAlarmAt() <= DUPLICATE;
    }

    /** The prayer's own minutes: from a minute before its time to LATE after (an alarm without its time, 0, is an old one's: as before). */
    static boolean adhanInTime(long due, long now) {
        return due <= 0 || (now >= due - 60_000 && now <= due + LATE);
    }

    /** The Friday reminder's own hours: from its alarm (two hours before) to the Jumu'ah itself, and never on another day. */
    static boolean jumuahInTime(long dhuhr, long now) {
        return dhuhr > 0 && now >= dhuhr - JumuahScheduler.BEFORE - 60_000 && now < dhuhr;
    }

    /** Standby is off: the adhan is never played to a dark screen, and never made up later. */
    private static boolean screenOn(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm == null || pm.isInteractive();
    }
}
