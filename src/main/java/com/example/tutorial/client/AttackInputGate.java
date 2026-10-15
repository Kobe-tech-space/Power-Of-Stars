package com.example.tutorial.client;

/** Coalesces raw, mapped and interaction events without imposing a gameplay cooldown. */
final class AttackInputGate {
    private Object world;
    private long tick = Long.MIN_VALUE;
    boolean accept(Object currentWorld, long currentTick) {
        if (world == currentWorld && tick == currentTick) return false;
        world = currentWorld;
        tick = currentTick;
        return true;
    }
    void reset() { world = null; tick = Long.MIN_VALUE; }
}
