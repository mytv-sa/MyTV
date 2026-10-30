package sa.mytv.screensaver;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Everything the user picks in settings, and the dashboard URL built from it. */
final class Prefs {
    static final String TZ = "Asia/Riyadh";

    // TheSportsDB id, Arabic name; more teams come from the in-app search
    static final String[][] TEAM_PRESETS = {
        {"136013", "الهلال"}, {"136137", "المنتخب السعودي"},
    };

    // League name exactly as TheSportsDB spells it (checked from CI by .github/tools/team_ids.py), Arabic label.
    // Their clubs come from assets/shared/teams.json (Teams), which CI builds from these leagues' matches
    // (.github/tools/team_ids.py reads this list); a new league here needs its sport and minimum team count there.
    static final String[][] LEAGUES = {
        {"Saudi-Arabian Pro League", "دوري روشن السعودي"},
        {"Saudi First Division League", "دوري يلو (الدرجة الأولى)"},
        {"English Premier League", "الدوري الإنجليزي"},
        {"Spanish La Liga", "الدوري الإسباني"},
        {"Italian Serie A", "الدوري الإيطالي"},
        {"German Bundesliga", "الدوري الألماني"},
        {"French Ligue 1", "الدوري الفرنسي"},
        {"Portuguese Primeira Liga", "الدوري البرتغالي"},
        {"Dutch Eredivisie", "الدوري الهولندي"},
        {"Turkish Super Lig", "الدوري التركي"},
        {"Egyptian Premier League", "الدوري المصري"},
        {"UAE Pro League", "الدوري الإماراتي"},
        {"Qatar Stars League", "الدوري القطري"},
        {"American Major League Soccer", "الدوري الأمريكي"},
        {"NBA", "دوري كرة السلة الأمريكي (NBA)"},
        {"NFL", "دوري كرة القدم الأمريكية (NFL)"},
    };

    // Other sports shown in the matches slot and card beside the followed teams' games: only their next event
    // (#events= key, Arabic label). The page finds each one's league ids in assets/shared/events.json (CI).
    static final String[][] EVENTS = {
        {"f1", "فورمولا 1"}, {"ufc", "UFC"}, {"boxing", "الملاكمة"},
    };

    // TVMaze genre (sent to the page), Arabic label
    static final String[][] GENRES = {
        {"Drama", "دراما"}, {"Comedy", "كوميديا"}, {"Action", "أكشن"}, {"Crime", "جريمة"},
        {"Thriller", "إثارة"}, {"Mystery", "غموض"}, {"Science-Fiction", "خيال علمي"},
        {"Fantasy", "فانتازيا"}, {"Adventure", "مغامرة"}, {"Romance", "رومانسي"},
        {"Family", "عائلي"}, {"History", "تاريخي"}, {"Horror", "رعب"},
        {"Anime", "أنمي"}, {"Documentary", "وثائقي"}, {"Sports", "رياضة"},
    };

    // TVMaze language, Arabic label
    static final String[][] LANGS = {
        {"English", "الإنجليزية"}, {"Arabic", "العربية"}, {"Korean", "الكورية"},
        {"Japanese", "اليابانية"}, {"Turkish", "التركية"}, {"Spanish", "الإسبانية"},
        {"French", "الفرنسية"}, {"Hindi", "الهندية"}, {"German", "الألمانية"},
    };

    static final String[] MIN_RATINGS = {"0", "6", "6.5", "7", "7.5", "8"};

    // minutes between background photos (sent to the page as #photos=; 30 is what it always did)
    static final String[] PHOTO_MINUTES = {"5", "10", "15", "30", "60"};

    // digits on the screen (sent to the page as #digits=; hindi is what it always showed)
    static final String[][] DIGITS = {{"hindi", "هندية ١٢٣"}, {"latin", "عربية 123"}};

    // the national days (sent to the page as #national=; auto shows them on their days, off never)
    static final String[][] NATIONAL = {{"auto", "تلقائي"}, {"off", "إيقاف"}};

    // look of the card beside the clock (sent to the page as #cardstyle=; current is the plain card it always had)
    static final String[][] CARD_STYLES = {{"current", "الحالية"}, {"dark1", "داكنة"}, {"glass1", "زجاجية"},
        {"dark3", "داكنة بإطار"}, {"glass3", "زجاجية بإطار"}, {"sadu", "سدو"}, {"night", "ليلية"}};

    // «الحروف المرسلة»: Thmanyah's swash letters, its ss01 (sent to the page as #swash=; off is how it always looked)
    static final String[][] SWASH = {{"off", "متوقفة"}, {"titles", "في العناوين فقط"}, {"all", "في كل النصوص"}};

    // markets at the top left (sent to the page as #markets=, up to MAX_MARKETS, in this order of choice; tasi,spx,btc is what it always showed)
    static final String[][] MARKETS = {
        {"tasi", "تاسي"}, {"aramco", "أرامكو"}, {"spx", "S&P 500"}, {"btc", "بيتكوين"},
        {"gold", "الذهب (الجرام بالريال)"}, {"silver", "الفضة (الجرام بالريال)"}, {"xrp", "ريبل"},
    };
    static final int MAX_MARKETS = 4;
    static final String DEFAULT_MARKETS = "tasi,spx,btc";

    // how the markets are shown (#mkstyle=; arrows is what it always showed)
    static final String[][] MK_STYLES = {{"arrows", "الاتجاه فقط ▲▼"}, {"prices", "السعر والنسبة"}};
    // what the bottom-left slot shows (sent to the page as #slot=; matches is what it always showed)
    static final String[][] SLOTS = {{"matches", "المباريات"}, {"weather", "الطقس"}, {"fasting", "الصيام"},
        {"countdown", "عدّ تنازلي"}, {"markets", "الأسواق"}, {"none", "بدون"}};

    // minutes of idle before Android starts the screensaver (the TV's own screen_off_timeout; not stored here)
    static final String[] IDLE_MINUTES = {"1", "2", "5", "10", "15", "30"};

    /** May this app change system settings (API 23+, and only if the owner allowed it for the app)? */
    static boolean canWriteSettings(Context c) {
        return Build.VERSION.SDK_INT >= 23 && Settings.System.canWrite(c);
    }

    /** The TV's idle delay in ms; readable without any permission. -1 when the TV does not say. */
    static long idleMs(Context c) {
        return Settings.System.getLong(c.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, -1);
    }

    private final SharedPreferences sp;
    private final Context ctx;

    Prefs(Context c) { ctx = c; sp = c.getSharedPreferences("settings", Context.MODE_PRIVATE); }

    /** {name, latitude, longitude}; Riyadh until the user picks */
    String[] city() {
        return new String[] {sp.getString("cityName", "الرياض"), sp.getString("cityLat", "24.6877"), sp.getString("cityLon", "46.7219")};
    }
    void setCity(String name, String lat, String lon) { setCity(name, lat, lon, ""); }
    /** tz: the place's own time zone, "" for a built-in city or when only the TV's is known (see cityTz) */
    void setCity(String name, String lat, String lon, String tz) {
        sp.edit().putString("cityName", name).putString("cityLat", lat).putString("cityLon", lon).putString("cityTz", tz).apply();
    }

    /** The time zone the page shows: Asia/Riyadh for a built-in (Saudi) city; a place of your own gets the one given
     *  with it, else the TV's own. */
    String cityTz() {
        String own = sp.getString("cityTz", "");
        if (!own.isEmpty()) return own;
        String[] c = city();
        for (Cities.Region r : Cities.REGIONS)
            for (String[] x : r.cities)
                if (x[0].equals(c[0]) && x[1].equals(c[1]) && x[2].equals(c[2])) return TZ;
        return java.util.TimeZone.getDefault().getID();
    }

    static final int MAX_TEAMS = 20;
    /** Whole leagues followed (#leagues=): the page downloads one small file for each, every 30 minutes. */
    static final int MAX_LEAGUES = 5;

    /** The league of LEAGUES that name (English as TheSportsDB spells it, the Arabic label, or its slug) means, else null. */
    static String[] leagueByName(String name) {
        String n = name.trim().toLowerCase().replaceAll("[^a-z0-9\\u0600-\\u06ff]+", "");
        for (String[] l : LEAGUES)
            if (n.equals(l[0].toLowerCase().replaceAll("[^a-z0-9]+", "")) || n.equals(l[1].toLowerCase().replaceAll("[^a-z0-9\\u0600-\\u06ff]+", "")))
                return l;
        return null;
    }

    /** Followed leagues, by their English names (default none). */
    List<String> leagues() {
        List<String> v = new ArrayList<>();
        for (String s : list("leagues")) {
            String[] l = leagueByName(s);
            if (l != null && !v.contains(l[0]) && v.size() < MAX_LEAGUES) v.add(l[0]);
        }
        return v;
    }

    /** Followed teams in priority order, "id|name". Migrates the old two team fields once. */
    List<String> teams() {
        if (!sp.contains("teams")) {
            List<String> v = new ArrayList<>();
            for (int slot = 1; slot <= 2; slot++) {
                String[] def = TEAM_PRESETS[slot - 1];
                String id = sp.getString("team" + slot + "Id", def[0]);
                if (!id.isEmpty()) v.add(id + "|" + sp.getString("team" + slot + "Name", def[1]));
            }
            return v;
        }
        return list("teams");
    }

    /** false when already followed or the list is full */
    boolean addTeam(String id, String name) {
        List<String> v = teams();
        for (String t : v) if (t.startsWith(id + "|")) return false;
        if (v.size() >= MAX_TEAMS) return false;
        v.add(id + "|" + name.replace("|", " ").replace(";", " "));
        setList("teams", v);
        return true;
    }

    // The card beside the clock: value sent to the page (#panel=), Arabic label
    static final String[][] PANELS = {
        {"shows", "جديد المسلسلات"}, {"prayers", "مواقيت الصلاة اليوم"}, {"forecast", "طقس الأيام القادمة"}, {"hourly", "طقس الساعات القادمة"}, {"prayerrow", "الصلوات في صف"},
        {"matches", "مباريات فرقك القادمة"}, {"verse", "آية أو حديث اليوم"}, {"wisdom", "حكمة اليوم"},
        {"adhkar", "أذكار الصباح والمساء"}, {"stocks", "الأسهم"}, {"tasks", "المهام"}, {"year", "السنة"},
        {"smart", "البطاقة الذكية"}, {"today", "يومك"},
        {"none", "لا شيء"},
    };

    /** Older builds had an on/off switch for the card; off becomes "none", otherwise the verse card. */
    String panel() { return sp.getString("panel", sp.getBoolean("showWeek", true) ? "verse" : "none"); }
    void setPanel(String v) { sp.edit().putString("panel", v).apply(); }

    // One plain line about each card, in the words of docs/FEATURES.md (tests/check.py compares them): the pickers show it under the name
    static final String[][] PANEL_ABOUT = {
        {"shows", "حلقات هذا الأسبوع"},
        {"prayers", "الست أوقات، والجاية مميّزة"},
        {"forecast", "خمسة أيام"},
        {"hourly", "الست ساعات الجاية بالساعة والأيقونة والحرارة، وقمر بعد الغروب"},
        {"prayerrow", "الخمس صلوات في صف واحد، والجاية مضيئة"},
        {"matches", "مباريات الأسبوع ونتائج آخر يوم لفرقك، وكل مباراة بسطر واحد"},
        {"verse", "آية يوم، وحديث صحيح من الكتب الستة اليوم اللي بعده، ومعه مصدره"},
        {"wisdom", "مثل أو قول عربي معروف مع مصدره"},
        {"adhkar", "من الفجر للظهر أذكار الصباح، ومن العصر للمغرب أذكار المساء"},
        {"stocks", "أسهمك بالسعر والتغيّر"},
        {"tasks", "قائمة مهامك"},
        {"year", "٣٦٥ نقطة، الأيام اللي راحت تولّع من الغامق للذهبي"},
        {"smart", "تختار لك البطاقة المناسبة للوقت"},
        {"today", "الصلاة القادمة ووقتها، والمباراة القادمة لفرقك، وأول مهمة مفتوحة، والمناسبة القادمة وكم يوم باقي"},
        {"none", "بدون بطاقة"},
    };
    static final String[][] CARD_STYLE_ABOUT = {
        {"current", "البطاقة كما كانت، بخلفية داكنة شفافة"},
        {"dark1", "داكنة بزوايا مدوّرة وخط ذهبي على جهة الساعة"},
        {"glass1", "زجاجية ضبابية فوق الصورة"},
        {"dark3", "داكنة بإطار ذهبي داخلي ومعيّن على الحافتين"},
        {"glass3", "زجاجية بإطار ذهبي داخلي ومعيّن على الحافتين"},
        {"sadu", "داكنة دافئة بشريط من نقش السدو أعلاها وأسفلها"},
        {"night", "كحلية داكنة بلمسة ذهبية في الزاوية"},
    };
    static final String[][] SWASH_ABOUT = {
        {"off", "الحروف كما هي"},
        {"titles", "الحروف المرسلة في التحية وعناوين البطاقات"},
        {"all", "الحروف المرسلة في كل النصوص ما عدا الآيات"},
    };

    static String about(String[][] table, String v) {
        for (String[] r : table) if (r[0].equals(v)) return r[1];
        return "";
    }

    static String panelLabel(String v) {
        for (String[] p : PANELS) if (p[0].equals(v)) return p[1];
        return PANELS[0][1];
    }

    /** The label of a card for a place that may be empty: «none» reads as emptyLabel there. */
    static String panelLabel(String v, String emptyLabel) {
        return "none".equals(v) ? emptyLabel : panelLabel(v);
    }

    List<String> list(String key) {
        List<String> out = new ArrayList<>();
        for (String s : sp.getString(key, "").split(";")) if (!s.isEmpty()) out.add(s);
        return out;
    }
    void setList(String key, List<String> v) { sp.edit().putString(key, join(v, ";")).apply(); }

    /** shows picked one by one, stored as "id|name" */
    List<String> shows() { return list("shows"); }

    String minRating() { return sp.getString("minRating", "7"); }
    void setMinRating(String v) { sp.edit().putString("minRating", v).apply(); }

    String digits() { return sp.getString("digits", "hindi"); }
    void setDigits(String v) { sp.edit().putString("digits", v).apply(); }

    static String digitsLabel(String v) {
        for (String[] d : DIGITS) if (d[0].equals(v)) return d[1];
        return DIGITS[0][1];
    }

    // the stocks card's watchlist (sent as #stocks=): 4-digit Tadawul codes and US tickers, up to MAX_STOCKS
    static final int MAX_STOCKS = 8;
    static final String DEFAULT_STOCKS = "2222,1120,7010,2010";
    static final String STOCK_PATTERN = "\\d{4}|[A-Za-z][A-Za-z.\\-]{0,5}";

    List<String> stocks() {
        List<String> v = list("stocks");
        return v.isEmpty() ? java.util.Arrays.asList(DEFAULT_STOCKS.split(",")) : v;
    }
    void setStocks(List<String> v) { setList("stocks", v); }

    /** Chosen markets in order, the keys of MARKETS. */
    /** the markets at the top left; empty when the person took them all off ("none" kept, so the default does not come back) */
    List<String> markets() {
        if ("none".equals(sp.getString("markets", ""))) return new ArrayList<>();
        List<String> v = list("markets");
        return v.isEmpty() ? java.util.Arrays.asList(DEFAULT_MARKETS.split(",")) : v;
    }
    void setMarkets(List<String> v) {
        if (v.isEmpty()) sp.edit().putString("markets", "none").apply();
        else setList("markets", v);
    }

    String mkStyle() { return sp.getString("mkStyle", "arrows"); }
    void setMkStyle(String v) { sp.edit().putString("mkStyle", v).apply(); }

    static String mkStyleLabel(String v) {
        for (String[] m : MK_STYLES) if (m[0].equals(v)) return m[1];
        return MK_STYLES[0][1];
    }

    String national() { return "off".equals(sp.getString("national", "auto")) ? "off" : "auto"; }
    void setNational(String v) { sp.edit().putString("national", v).apply(); }

    static String nationalLabel(String v) {
        for (String[] n : NATIONAL) if (n[0].equals(v)) return n[1];
        return NATIONAL[0][1];
    }

    String cardStyle() { return sp.getString("cardStyle", "current"); }
    void setCardStyle(String v) { sp.edit().putString("cardStyle", v).apply(); }

    static String cardStyleLabel(String v) {
        for (String[] c : CARD_STYLES) if (c[0].equals(v)) return c[1];
        return CARD_STYLES[0][1];
    }

    String swash() { return sp.getString("swash", "off"); }
    void setSwash(String v) { sp.edit().putString("swash", v).apply(); }

    static String swashLabel(String v) {
        for (String[] s : SWASH) if (s[0].equals(v)) return s[1];
        return SWASH[0][1];
    }

    String slot() { return sp.getString("slot", "matches"); }
    void setSlot(String v) { sp.edit().putString("slot", v).apply(); }

    static String slotLabel(String v) {
        for (String[] s : SLOTS) if (s[0].equals(v)) return s[1];
        return SLOTS[0][1];
    }

    /** the countdown's date (YYYY-MM-DD, "" = none) and its name; sent to the page as #cd= and #cdname= */
    String countdownDate() { return sp.getString("cd", ""); }
    String countdownName() { return sp.getString("cdName", ""); }
    void setCountdown(String date, String name) { sp.edit().putString("cd", date).putString("cdName", name).apply(); }

    String photoMinutes() { return sp.getString("photoMinutes", "30"); }
    void setPhotoMinutes(String v) { sp.edit().putString("photoMinutes", v).apply(); }

    /** The slim bar before the adhan, on the screensaver and on the home screen (default on); sent to the page as #adhanbar=. */
    boolean adhanBar() { return sp.getBoolean("adhanBar", true); }
    void setAdhanBar(boolean v) { sp.edit().putBoolean("adhanBar", v).apply(); }

    /** The slim year strip under the clock, on the screensaver only (default off); sent to the page as #yearstrip=. */
    boolean yearStrip() { return sp.getBoolean("yearStrip", false); }
    void setYearStrip(boolean v) { sp.edit().putBoolean("yearStrip", v).apply(); }

    /** «تعديل وقت الصلاة» (default off): the five prayers moved by their own minutes, −20..20 (PrayerAdj); sent to the page and the home screen as #adj=. */
    boolean prayerAdjOn() { return sp.getBoolean("prayerAdjOn", false); }
    void setPrayerAdjOn(boolean v) { sp.edit().putBoolean("prayerAdjOn", v).apply(); }
    int[] prayerAdj() { return PrayerAdj.decode(sp.getString("prayerAdj", "")); }
    void setPrayerAdj(int[] v) { sp.edit().putString("prayerAdj", PrayerAdj.encode(v)).apply(); }

    boolean autoUpdate() { return sp.getBoolean("autoUpdate", true); }
    void setAutoUpdate(boolean v) { sp.edit().putBoolean("autoUpdate", v).apply(); }

    SharedPreferences raw() { return sp; }

    /** The page reads its settings from the fragment; a ?query fails on file:// in WebView. */
    String dashboardUrl(Context c) { return pageBase(c) + "#" + fragment(); }

    /**
     * The page for a preview in the settings: the card alone (#widget=1) with the current settings, but this card in this style.
     * panel= and cardstyle= are taken out of the fragment first: the page reads the first one it finds.
     */
    String previewUrl(Context c, String panel, String cardStyle) { return previewUrl(c, panel, cardStyle, swash()); }
    String previewUrl(Context c, String panel, String cardStyle, String swash) {
        String frag = fragment().replaceAll("(^|&)(panel|week|cardstyle|swash|home_panel)=[^&]*", "");
        return pageBase(c) + "#" + frag + "&panel=" + panel + "&cardstyle=" + cardStyle + "&swash=" + swash + "&widget=1";
    }

    /**
     * The page without its settings: the downloaded copy when its build stamp is newer than the one the app carries, else the app's own. By
     * the stamp in each page (PageStamp), not by file time: a download from a stale CDN copy gets a newer file time than the APK's install
     * and used to win for hours.
     */
    private String pageBase(Context c) {
        File remote = Updater.pageFile(c);
        boolean useRemote = false;
        if (autoUpdate() && remote.isFile()) {
            try { useRemote = PageStamp.newer(Updater.fileStamp(remote), Updater.assetStamp(c)); }
            catch (Exception ignored) { }
        }
        return useRemote ? Uri.fromFile(remote).toString() : "file:///android_asset/tv.html";
    }

    /** Everything the page needs, as "city=..&lat=..&..."; also read by the launcher (ConfigProvider). */
    String fragment() {
        String[] city = city();
        List<String> ids = new ArrayList<>();
        for (String s : shows()) ids.add(s.split("\\|", 2)[0]);
        return "city=" + Uri.encode(city[0]) + "&lat=" + city[1] + "&lon=" + city[2]
            + "&tz=" + Uri.encode(cityTz())
            + "&teams=" + join(teamIds(), ",")
            + "&events=" + join(list("events"), ",")
            + "&leagues=" + Uri.encode(join(leagues(), ","))
            + "&panel=" + panel()
            + "&week=" + ("none".equals(panel()) ? "0" : "1")   // for a page older than #panel
            + "&genres=" + Uri.encode(join(list("genres"), ","))
            + "&langs=" + Uri.encode(join(list("langs"), ","))
            + "&shows=" + join(ids, ",")
            + "&minrating=" + minRating()
            + "&photos=" + photoMinutes()
            + "&digits=" + digits()
            + "&cardstyle=" + cardStyle()
            + "&swash=" + swash()
            + "&adhanbar=" + (adhanBar() ? "1" : "0")
            + "&yearstrip=" + (yearStrip() ? "1" : "0")
            + PrayerAdj.fragment(prayerAdjOn(), prayerAdj())
            + "&national=" + national()
            + "&markets=" + (markets().isEmpty() ? "none" : join(markets(), ","))
            + "&mkstyle=" + mkStyle()
            + "&stocks=" + join(stocks(), ",")
            + "&slot=" + slot()
            + "&cd=" + countdownDate()
            + "&cdname=" + Uri.encode(countdownName())
            + openTasksParam()
            + ownParam();
    }

    /** The owner's own photos: how many and mix/only; nothing at all while there are none, so the page is as before. */
    private String ownParam() {
        int n = Own.count(ctx);
        return n == 0 ? "" : "&own=" + n + "&ownmode=" + Own.mode(ctx);
    }

    String ownMode() { return sp.getString("ownMode", Own.MIX); }
    void setOwnMode(String v) { sp.edit().putString("ownMode", v).apply(); }

    /** counts every change to the photos, so the launcher can tell it should show them */
    long ownRev() { return sp.getLong("ownRev", 0); }
    void bumpOwnRev() { sp.edit().putLong("ownRev", ownRev() + 1).apply(); }

    static final int MAX_TASKS = 12;
    static final int TASK_CHARS = 60;

    /** One task kept on the TV. */
    static final class Task {
        final String text;
        final boolean done;
        final long at;
        Task(String text, boolean done, long at) { this.text = text; this.done = done; this.at = at; }
    }

    /** All tasks, oldest first (a JSON array of {t, done, at}); the number a person sees is the position + 1. */
    List<Task> tasks() { return parseTasks(sp.getString("tasks", "[]")); }

    /** The tasks of the saved JSON; an entry that is not a task is skipped, not the whole list lost. */
    static List<Task> parseTasks(String json) {
        List<Task> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(json);
            for (int k = 0; k < a.length(); k++) {
                try {
                    JSONObject o = a.getJSONObject(k);
                    out.add(new Task(o.getString("t"), o.optBoolean("done"), o.optLong("at")));
                } catch (JSONException bad) {
                    // one entry that is not a task is skipped; the others are kept (the list is saved back whole)
                }
            }
        } catch (JSONException e) {
            out.clear();
        }
        return out;
    }

    private void setTasks(List<Task> v) {
        JSONArray a = new JSONArray();
        try {
            for (Task t : v) a.put(new JSONObject().put("t", t.text).put("done", t.done).put("at", t.at));
        } catch (JSONException ignored) { }
        sp.edit().putString("tasks", a.toString()).apply();
    }

    /** Trimmed, one line, at most TASK_CHARS characters; "" when nothing is left. */
    static String cleanTask(String raw) {
        String s = raw == null ? "" : raw.replaceAll("\\s+", " ").trim();
        if (s.length() > TASK_CHARS) s = s.substring(0, TASK_CHARS).trim();
        return s;
    }

    /** null when added; otherwise why not (full of open tasks, or empty text). Done tasks make room, oldest first. */
    String addTask(String raw) {
        String text = cleanTask(raw);
        if (text.isEmpty()) return "the task is empty";
        List<Task> v = tasks();
        while (v.size() >= MAX_TASKS) {
            int old = -1;
            for (int k = 0; k < v.size() && old < 0; k++) if (v.get(k).done) old = k;
            if (old < 0) return "at most " + MAX_TASKS + " tasks; finish or delete one first";
            v.remove(old);
        }
        v.add(new Task(text, false, System.currentTimeMillis()));
        setTasks(v);
        return null;
    }

    /** n counts from 1; false when there is no such task */
    boolean doneTask(int n) {
        List<Task> v = tasks();
        if (n < 1 || n > v.size()) return false;
        Task t = v.get(n - 1);
        v.set(n - 1, new Task(t.text, true, t.at));
        setTasks(v);
        return true;
    }

    boolean deleteTask(int n) {
        List<Task> v = tasks();
        if (n < 1 || n > v.size()) return false;
        v.remove(n - 1);
        setTasks(v);
        return true;
    }

    void clearTasks() { sp.edit().remove("tasks").apply(); }

    /** "&tasks=" and the open tasks, one per line, URL-encoded; nothing when there are none (the page shows «لا مهام اليوم»). */
    private String openTasksParam() {
        List<String> open = new ArrayList<>();
        for (Task t : tasks()) if (!t.done) open.add(t.text);
        return open.isEmpty() ? "" : "&tasks=" + Uri.encode(join(open, "\n"));
    }

    /** The cards on the launcher's home screen: up to HOME_CARDS of PANELS, none until the user picks one. */
    static final int HOME_CARDS = 3;

    /**
     * "prayers,hourly" (a single "prayers" is what older builds stored) as the cards it names, in order: each once, "none" and
     * anything unknown left out, at most HOME_CARDS. An empty list is no card.
     */
    static List<String> parseHomePanels(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String s : raw.split(",")) {
            s = s.trim();
            boolean known = false;
            for (String[] p : PANELS) if (p[0].equals(s) && !"none".equals(s)) known = true;
            if (known && !out.contains(s) && out.size() < HOME_CARDS) out.add(s);
        }
        return out;
    }

    /**
     * The three places of the home screen keep their own card ("homeCard0".."homeCard2", "none" = empty) and their own row
     * ("homeRow0".., ROW_TOP or ROW_BOTTOM): emptying one place moves nothing else. A TV from before the places has only the list
     * "homePanel"; it is read as places 0, 1, 2 in order, in the rows that list was drawn in (two on top, the third under them).
     */
    static final String ROW_TOP = "t", ROW_BOTTOM = "b";
    /** a row holds at most two cards side by side */
    static final int ROW_MAX = 2;

    private boolean placesKept() { return sp.contains("homeCard0"); }

    /** The cards that show, in the order of their places: what the page and the launcher get as home_panels. */
    List<String> homePanels() {
        if (!placesKept()) return parseHomePanels(sp.getString("homePanel", "none"));
        List<String> v = new ArrayList<>();
        for (int k = 0; k < HOME_CARDS; k++) { String c = homePanelAt(k); if (!"none".equals(c)) v.add(c); }
        return v;
    }
    /** saver.sh's home_panel=a,b,c: the cards into places 0, 1, 2 in that order, the rest empty, the rows as the list was drawn */
    void setHomePanels(List<String> v) {
        List<String> ok = parseHomePanels(join(v, ","));
        SharedPreferences.Editor e = sp.edit().putString("homePanel", ok.isEmpty() ? "none" : join(ok, ","));
        for (int k = 0; k < HOME_CARDS; k++) e.putString("homeCard" + k, k < ok.size() ? ok.get(k) : "none").remove("homeRow" + k);
        e.apply();
    }
    /** What the page and saver.sh see: "none", or the cards joined by commas. */
    String homePanel() { List<String> v = homePanels(); return v.isEmpty() ? "none" : join(v, ","); }
    /** The card in place i (0 to 2) of the home screen, or "none". */
    String homePanelAt(int i) {
        if (!placesKept()) { List<String> v = homePanels(); return i < v.size() ? v.get(i) : "none"; }
        List<String> one = parseHomePanels(sp.getString("homeCard" + i, "none"));
        return one.isEmpty() ? "none" : one.get(0);
    }
    /** The row of place i: the one chosen, else where the list put it (the third under the other two). */
    String homeRowAt(int i) {
        String r = sp.getString("homeRow" + i, null);
        if (ROW_TOP.equals(r) || ROW_BOTTOM.equals(r)) return r;
        return i == 2 ? ROW_BOTTOM : ROW_TOP;
    }
    /** The rows of the cards that show, in the order of homePanels(): "t,t,b". */
    String homeRows() {
        List<String> v = new ArrayList<>();
        for (int k = 0; k < HOME_CARDS; k++) if (!"none".equals(homePanelAt(k))) v.add(homeRowAt(k));
        return join(v, ",");
    }
    private int inRow(String row, int except) {
        int n = 0;
        for (int k = 0; k < HOME_CARDS; k++) if (k != except && !"none".equals(homePanelAt(k)) && row.equals(homeRowAt(k))) n++;
        return n;
    }
    /** Place i gets that card ("none" empties it; nothing else moves); a card already in another place leaves that place empty. */
    void setHomePanelAt(int i, String card) {
        String[] slot = new String[HOME_CARDS], row = new String[HOME_CARDS];
        for (int k = 0; k < HOME_CARDS; k++) { slot[k] = homePanelAt(k); row[k] = homeRowAt(k); }
        for (int k = 0; k < HOME_CARDS; k++) if (k != i && card.equals(slot[k])) slot[k] = "none";
        slot[i] = card;
        SharedPreferences.Editor e = sp.edit();
        for (int k = 0; k < HOME_CARDS; k++) e.putString("homeCard" + k, slot[k]).putString("homeRow" + k, row[k]);
        e.commit();
        // its row already has two: it goes to the other one
        if (!"none".equals(card) && inRow(row[i], i) >= ROW_MAX) sp.edit().putString("homeRow" + i, other(row[i])).apply();
        List<String> shown = homePanels();
        sp.edit().putString("homePanel", shown.isEmpty() ? "none" : join(shown, ",")).apply();
    }
    /** Place i moves to that row; false (and nothing changes) when the row has two cards already. */
    boolean setHomeRowAt(int i, String r) {
        if (!"none".equals(homePanelAt(i)) && inRow(r, i) >= ROW_MAX) return false;
        if (!placesKept()) setHomePanelAt(i, homePanelAt(i));   // the old list becomes places first
        sp.edit().putString("homeRow" + i, r).apply();
        return true;
    }
    static String other(String row) { return ROW_TOP.equals(row) ? ROW_BOTTOM : ROW_TOP; }

    /** The greeting and the weather line on the launcher's home screen (default on). Off: the plain good-morning it had. */
    boolean greeting() { return sp.getBoolean("greeting", true); }
    void setGreeting(boolean v) { sp.edit().putBoolean("greeting", v).apply(); }

    /** «صباح الخير عائلتي»: a word after the greeting, up to 11 characters with the spaces; shown only while it is on. */
    static final int GREET_WORD_MAX = 11;
    boolean greetWordOn() { return sp.getBoolean("greetWordOn", false); }
    void setGreetWordOn(boolean v) { sp.edit().putBoolean("greetWordOn", v).apply(); }
    String greetWord() { return sp.getString("greetWord", ""); }
    void setGreetWord(String v) { sp.edit().putString("greetWord", v).apply(); }
    /** what the launcher shows after the greeting: the word while it is on, else nothing */
    String greetWordShown() { return greetWordOn() ? greetWord() : ""; }

    /** null when the word may be kept, else why not (in Arabic) */
    static String greetWordProblem(String v) {
        if (v.isEmpty()) return "اكتب الكلمة";
        if (v.length() > GREET_WORD_MAX) return "الحد " + GREET_WORD_MAX + " حرفًا مع المسافات";
        if (v.contains("|") || v.contains("\n")) return "لا يُقبل هذا الرمز";
        return null;
    }

    // when «طابت أيامكم» and «تصبحون على خير» start: at Dhuhr and Isha (the default), or at an hour of the city's day
    static final String[][] GREET_DAY_STARTS = {{"dhuhr", "من الظهر"}, {"13", "من ١ ظهرًا"}, {"14", "من ٢ ظهرًا"}, {"15", "من ٣ عصرًا"}};
    static final String[][] GREET_NIGHT_STARTS = {{"isha", "من العشاء"}, {"21", "من ٩ مساءً"}, {"22", "من ١٠ مساءً"},
        {"23", "من ١١ مساءً"}, {"0", "من ١٢ منتصف الليل"}};
    boolean greetDayOn() { return sp.getBoolean("greetDayOn", true); }
    void setGreetDayOn(boolean v) { sp.edit().putBoolean("greetDayOn", v).apply(); }
    String greetDayStart() { return pick(GREET_DAY_STARTS, sp.getString("greetDayStart", "dhuhr")); }
    void setGreetDayStart(String v) { sp.edit().putString("greetDayStart", v).apply(); }
    boolean greetNightOn() { return sp.getBoolean("greetNightOn", true); }
    void setGreetNightOn(boolean v) { sp.edit().putBoolean("greetNightOn", v).apply(); }
    String greetNightStart() { return pick(GREET_NIGHT_STARTS, sp.getString("greetNightStart", "isha")); }
    void setGreetNightStart(String v) { sp.edit().putString("greetNightStart", v).apply(); }
    /** for the launcher: "off", or when it starts */
    String greetDay() { return greetDayOn() ? greetDayStart() : "off"; }
    String greetNight() { return greetNightOn() ? greetNightStart() : "off"; }

    static String startLabel(String[][] all, String v) {
        for (String[] s : all) if (s[0].equals(v)) return s[1];
        return all[0][1];
    }

    /** a value of the list, else its first (the default) */
    private static String pick(String[][] all, String v) {
        for (String[] s : all) if (s[0].equals(v)) return v;
        return all[0][0];
    }

    List<String> teamIds() {
        List<String> ids = new ArrayList<>();
        for (String t : teams()) ids.add(t.split("\\|", 2)[0]);
        return ids;
    }

    static String join(List<String> v, String sep) {
        StringBuilder b = new StringBuilder();
        for (String s : v) { if (b.length() > 0) b.append(sep); b.append(s); }
        return b.toString();
    }
}
