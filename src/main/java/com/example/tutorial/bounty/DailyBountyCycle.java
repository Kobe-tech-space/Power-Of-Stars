package com.example.tutorial.bounty;

/** Backwards time changes cannot reset claimed rewards a second time. */
public final class DailyBountyCycle {
    private DailyBountyCycle() {}
    public static boolean shouldRefresh(long savedDay, long currentDay) { return currentDay > savedDay; }
}
