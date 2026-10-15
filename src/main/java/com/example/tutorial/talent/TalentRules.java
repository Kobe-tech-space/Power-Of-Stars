package com.example.tutorial.talent;

/** Pure, stable-index rules shared by client, server and tests. */
public final class TalentRules {
    public static final int NODES = 12, MAX_RANK = 3, MAX_POINTS = 24;
    public static int threshold(int level) { return 100 + Math.max(0, level) * 25; }
    public static int earned(int experience) {
        int level = 0, remaining = Math.max(0, experience);
        while (level < MAX_POINTS && remaining >= threshold(level)) remaining -= threshold(level++);
        return level;
    }
    public static int progress(int experience) {
        int remaining = Math.max(0, experience);
        for (int i = 0; i < earned(experience); i++) remaining -= threshold(i);
        return earned(experience) == MAX_POINTS ? 0 : remaining;
    }
    public static int[] sanitize(int[] values) {
        int[] ranks = new int[NODES]; int spent = 0;
        for (int i = 0; i < NODES; i++) {
            int value = i < values.length ? Math.max(0, Math.min(MAX_RANK, values[i])) : 0;
            if (i % 4 != 0 && ranks[i - 1] < 2) value = 0;
            ranks[i] = Math.min(value, MAX_POINTS - spent); spent += ranks[i];
        }
        return ranks;
    }
    public static int spent(int[] values) { return java.util.Arrays.stream(sanitize(values)).sum(); }
    public static boolean canLearn(int experience, int[] values, int node) {
        int[] ranks = sanitize(values);
        return node >= 0 && node < NODES && ranks[node] < MAX_RANK
                && (node % 4 == 0 || ranks[node - 1] >= 2) && spent(ranks) < earned(experience);
    }
    private TalentRules() {}
}
