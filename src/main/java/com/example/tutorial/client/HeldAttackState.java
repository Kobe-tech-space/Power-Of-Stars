package com.example.tutorial.client;

/** A new press is required after release or loss of the ranged gameplay context. */
final class HeldAttackState {
    private boolean held;
    void update(boolean pressed) { held = pressed; }
    boolean isHeld() { return held; }
}
