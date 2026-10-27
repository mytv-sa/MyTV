package sa.mytv.home;

/**
 * The words of the home screen's greeting and its weather line. No Android in here, so
 * tests/greeting_test.py runs it on tests/greeting_cases.tsv.
 */
final class GreetingText {
    static final int MORNING = 0, DAY = 1, EVENING = 2, NIGHT = 3;

    private GreetingText() { }

    /** Fajr to Dhuhr, Dhuhr to Asr, Asr to Isha, and the night from Isha to the next Fajr. Times in millis. */
    static int part(long now, long fajr, long dhuhr, long asr, long isha) {
        return part(now, fajr, dhuhr, asr, isha, dhuhr, isha);
    }

    /**
     * The same, with «طابت أيامكم» from dayStart and «تصبحون على خير» from nightStart (-1: that one is off, and the greeting
     * before it goes on: the morning's until Asr, the evening's until Fajr). After midnight the night has begun whatever its start.
     */
    static int part(long now, long fajr, long dhuhr, long asr, long isha, long dayStart, long nightStart) {
        if (now < fajr) return nightStart < 0 ? EVENING : NIGHT;
        if (now < dhuhr) return MORNING;
        if (now < asr) return dayStart >= 0 && now >= dayStart ? DAY : MORNING;
        return nightStart >= 0 && now >= nightStart ? NIGHT : EVENING;
    }

    /**
     * When a greeting starts, from its setting: "off" gives -1, "dhuhr" or "isha" the prayer's moment, an hour ("13", "21"; "0" is
     * midnight, the end of today) that hour of the city's day. midnight: the start of the city's day, in millis.
     */
    static long start(String spec, long prayer, long midnight) {
        if (spec == null || spec.isEmpty() || "dhuhr".equals(spec) || "isha".equals(spec)) return prayer;
        if ("off".equals(spec)) return -1;
        try {
            int h = Integer.parseInt(spec);
            return midnight + (h == 0 ? 24 : h) * 3600000L;
        } catch (NumberFormatException e) {
            return prayer;
        }
    }

    static String greeting(int part) {
        switch (part) {
            case MORNING: return "صباح الخير";
            case DAY: return "طابت أيامكم";
            case EVENING: return "مساء الخير";
            default: return "تصبحون على خير";
        }
    }

    /**
     * «صباح الخير عائلتي»: the word the person chose after the greeting (empty: the greeting alone). Two spaces before it: the tail
     * of «ر» in «الخير» runs into one, and the word looked stuck to it on the TV.
     */
    static String withWord(String greeting, String word) {
        return word == null || word.trim().isEmpty() ? greeting : greeting + "  " + word.trim();
    }

    /** Open-Meteo's WMO weather code, as a short phrase that goes before «في المدينة». */
    static String sky(int code) {
        switch (code) {
            case 0: return "سماء صافية";
            case 1: return "سماء صافية غالبًا";
            case 2: return "غائم جزئيًا";
            case 3: return "سماء غائمة";
            case 45: case 48: return "ضباب";
            case 51: case 53: case 55: case 56: case 57: return "رذاذ";
            case 61: case 63: case 80: case 81: return "أمطار";
            case 65: case 82: return "أمطار غزيرة";
            case 66: case 67: return "مطر متجمد";
            case 71: case 73: case 75: case 77: case 85: case 86: return "ثلوج";
            case 95: return "عواصف رعدية";
            case 96: case 99: return "عواصف رعدية وبرد";
            default: return "";
        }
    }

    /** PM10 or dust in µg/m³ from which the air is «غبار», and «غبار كثيف»: the page's thresholds (it overrides its condition word) */
    static final double DUST = 150, HEAVY_DUST = 400;

    /** The sky's word, or «غبار» / «غبار كثيف» when the dust level (the larger of Open-Meteo's pm10 and dust) says so. */
    static String skyWord(int code, double dust) {
        if (dust >= HEAVY_DUST) return "غبار كثيف";
        if (dust >= DUST) return "غبار";
        return sky(code);
    }

    /** «سماء صافية في الرياض، ٣١°» (no «·» beside a digit: the TV's font draws it as ٠); a code we have no word for gives just «الرياض، ٣١°»; dust (0 when unknown) takes the word over. */
    static String weather(int code, double dust, String city, double temp, boolean latin) {
        String sky = skyWord(code, dust);
        String place = sky.isEmpty() ? city : sky + " في " + city;
        return place + "، " + digits(Math.round(temp) + "°", latin);
    }

    /** «الغروب ٥:٥٧ م» from "17:57". */
    static String sun(String label, String hhmm, boolean latin) {
        int colon = hhmm.indexOf(':');
        int h = Integer.parseInt(hhmm.substring(0, colon));
        String m = hhmm.substring(colon + 1, colon + 3);
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return label + " " + digits(h12 + ":" + m, latin) + " " + (h < 12 ? "ص" : "م");
    }

    /** Arabic-Indic digits (as the page's #digits=hindi), or the Latin ones left alone. */
    static String digits(String s, boolean latin) {
        return DigitsText.convert(s, latin);
    }
}
