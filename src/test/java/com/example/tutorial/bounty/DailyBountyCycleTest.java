package com.example.tutorial.bounty;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DailyBountyCycleTest {
    @Test void refreshAtNewDayNotAtNight() {
        assertFalse(DailyBountyCycle.shouldRefresh(0, 23999L/24000));
        assertTrue(DailyBountyCycle.shouldRefresh(0, 24000L/24000));
        assertFalse(DailyBountyCycle.shouldRefresh(1, 1));
    }
    @Test void backwardsTimeCannotDuplicateRewards() {
        assertFalse(DailyBountyCycle.shouldRefresh(8, 3));
        assertFalse(DailyBountyCycle.shouldRefresh(8, 8));
        assertTrue(DailyBountyCycle.shouldRefresh(8, 9));
    }
    @Test void offlineDaysOnlyYieldOneCurrentCycle() {
        assertTrue(DailyBountyCycle.shouldRefresh(2, 30));
        assertFalse(DailyBountyCycle.shouldRefresh(30, 30));
    }
}
