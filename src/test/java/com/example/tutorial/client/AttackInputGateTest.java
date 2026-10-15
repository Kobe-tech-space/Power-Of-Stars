package com.example.tutorial.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AttackInputGateTest {
    @Test void overlappingInputPathsOnlyFireOnce() {
        var gate = new AttackInputGate();
        Object world = new Object();
        assertTrue(gate.accept(world, 100));
        assertFalse(gate.accept(world, 100));
        assertFalse(gate.accept(world, 100));
        assertTrue(gate.accept(world, 101));
    }
    @Test void everyTickIsAvailableWithoutCooldown() {
        var gate = new AttackInputGate();
        Object world = new Object();
        for (long tick = 0; tick < 1000; tick++) assertTrue(gate.accept(world, tick));
    }
    @Test void dimensionAndReconnectCannotInheritOldGate() {
        var gate = new AttackInputGate();
        assertTrue(gate.accept(new Object(), 100));
        assertTrue(gate.accept(new Object(), 100));
        Object world = new Object();
        assertTrue(gate.accept(world, 100));
        gate.reset();
        assertTrue(gate.accept(world, 100));
        assertTrue(gate.accept(world, 0));
    }
}
