package com.example.tutorial.ornament;

/** Stable identity and bounded, deliberately varied effects. */
public enum OrnamentKind {
    STARFALL_AMULET("starfall_amulet"), ORBIT_RING("orbit_ring"), WING_BROOCH("wing_brooch"),
    CRESCENT_EARRING("crescent_earring"), DAWN_CROWN("dawn_crown"), CONSTELLATION_BRACELET("constellation_bracelet"),
    METEOR_PENDANT("meteor_pendant"), ASTRAL_COMPASS("astral_compass"), ASTRAL_BADGE("astral_badge"), TWILIGHT_TALISMAN("twilight_talisman");
    public final String id;
    OrnamentKind(String id) { this.id=id; }
    public static final int MAX_EQUIPPED=3;
}
