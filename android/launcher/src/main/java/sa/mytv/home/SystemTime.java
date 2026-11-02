package sa.mytv.home;

/**
 * Whether the TV's clock has been set yet. A set cut off at the wall loses its clock: Android starts it at the day its
 * system was built (18 November 2024 on TV 1) and the network time comes some 50 seconds later, as a jump of two years.
 * Until then no alarm is set, no prayer times are asked for, and the home screen shows no time. No Android in here.
 */
public final class SystemTime {
    /** 1 January 2026, UTC: later than any system's build day, earlier than any real today */
    static final long SET_AFTER = 1_767_225_600_000L;

    private SystemTime() { }

    public static boolean isSet() { return isSet(System.currentTimeMillis()); }

    static boolean isSet(long millis) { return millis >= SET_AFTER; }
}
