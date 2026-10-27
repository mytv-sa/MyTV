package sa.mytv.home;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * One read of the screensaver's ConfigProvider (same signing key) for everything the home screen's minute tick needs: the
 * city's zone, the greeting, the bar before the adhan, the prayer times' place. Each of those used to ask the provider (a
 * cross-process query) for itself, eight times a minute.
 */
final class ConfigSnapshot {
    static final Uri URI = Uri.parse("content://sa.mytv.screensaver.config");

    /** "city=..&lat=..&lon=..&tz=..&..." as the page reads it; null when the screensaver is missing or does not answer */
    final String fragment;
    /** an older screensaver does not say: on, the default */
    final boolean greetingOn, adhanBarOn;
    /** the word after the greeting ("" none), and when «طابت أيامكم» and «تصبحون على خير» start (GreetingText.start: off, dhuhr/isha, an hour) */
    final String greetWord, greetDay, greetNight;

    private ConfigSnapshot(String fragment, boolean greetingOn, boolean adhanBarOn, String greetWord, String greetDay, String greetNight) {
        this.fragment = fragment;
        this.greetingOn = greetingOn;
        this.adhanBarOn = adhanBarOn;
        this.greetWord = greetWord;
        this.greetDay = greetDay;
        this.greetNight = greetNight;
    }

    static ConfigSnapshot read(Context c) {
        String fragment = null, word = "", day = "dhuhr", night = "isha";
        boolean greeting = true, bar = true;
        try (Cursor cur = c.getContentResolver().query(URI, null, null, null, null)) {
            if (cur != null && cur.moveToFirst()) {
                fragment = cur.getString(0);
                int g = cur.getColumnIndex("greeting");
                if (g >= 0 && "0".equals(cur.getString(g))) greeting = false;
                int b = cur.getColumnIndex("adhan_bar");
                if (b >= 0 && "0".equals(cur.getString(b))) bar = false;
                word = column(cur, "greet_word", word);
                day = column(cur, "greet_day", day);
                night = column(cur, "greet_night", night);
            }
        } catch (RuntimeException ignored) {
            // the screensaver is missing or signed with another key: the defaults (Riyadh, on)
        }
        return new ConfigSnapshot(fragment, greeting, bar, word, day, night);
    }

    /** a column an older screensaver does not have: the default */
    private static String column(Cursor cur, String name, String fallback) {
        int i = cur.getColumnIndex(name);
        String v = i >= 0 ? cur.getString(i) : null;
        return v == null ? fallback : v;
    }

    /** {city, zone id}; null when the screensaver gave nothing: the TV's own zone then */
    String[] cityZone() { return fragment == null ? null : ClockZone.parse(fragment); }

    boolean latin() { return fragment != null && (fragment.contains("&digits=latin") || fragment.startsWith("digits=latin")); }
}
