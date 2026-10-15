package com.example.tutorial.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HeldAttackStateTest {
    @Test void holdingFiresEveryTickAndReleaseStops() {
        var held = new HeldAttackState();
        var gate = new AttackInputGate();
        Object world = new Object();
        assertFalse(held.isHeld());
        held.update(true);
        for (long tick = 0; tick < 100; tick++) {
            assertTrue(held.isHeld());
            assertTrue(gate.accept(world, tick));
            assertFalse(gate.accept(world, tick));
        }
        held.update(false);
        assertFalse(held.isHeld());
    }

    @Test void invalidContextRequiresFreshPress() {
        var held = new HeldAttackState();
        held.update(true);
        held.update(false);
        for (int tick = 0; tick < 100; tick++) assertFalse(held.isHeld());
        held.update(true);
        assertTrue(held.isHeld());
    }
}
