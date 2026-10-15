package com.example.tutorial.config;
import net.minecraftforge.common.ForgeConfigSpec;
import java.util.*;

/** World-owned SERVER config: Forge synchronizes this spec to connecting clients. */
public final class ModSettings {
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();
    private static final Map<String,ForgeConfigSpec.IntValue> INTS = new LinkedHashMap<>();
    private static final Map<String,ForgeConfigSpec.DoubleValue> DOUBLES = new LinkedHashMap<>();
    private static final Map<String,ForgeConfigSpec.BooleanValue> BOOLS = new LinkedHashMap<>();
    static {
        flag("ornaments.enabled",true);
        flag("talents.enabled",true); number("talents.killExperience",10,1,1000);
        number("talents.resetLevels",5,0,100); number("talents.resetIngots",8,0,64);
        number("energy.lowCapacity",250,1,100000); number("energy.highCapacity",1000,1,100000);
        number("shield.capacity",50,1,10000); number("shield.reforgeCost",150,0,100000);
        number("skills.domainCost",1000,1,100000); number("skills.armorWear",1,0,100);
        fraction("slash.speed",1.1,.1,4); fraction("slash.skillSpeed",1.35,.1,4); fraction("slash.skillDamage",.75,.01,10);
        for(String part:List.of("helmet","chestplate","leggings","boots")) {
            number("armor.low."+part,20,0,500); number("armor.high."+part,50,0,500);
        }
        number("armor.low.setBonus",20,0,500); number("armor.high.setBonus",50,0,500);
        fraction("armor.toughness",50,0,500);
        fraction("armor.low.pieceImmunity",.05,0,.95); fraction("armor.high.pieceImmunity",.10,0,.95);
        fraction("armor.low.setImmunity",.10,0,.95); fraction("armor.high.setImmunity",.20,0,.95);
        fraction("elites.spawnChance",.03,0,1); fraction("elites.eclipseChance",.14,0,1);
        number("network.skillPacketsPerSecond",8,1,40); number("network.slashPacketsPerSecond",20,1,40);
        flag("combat.friendlyFire",false); flag("combat.breakFragileBlocks",false); flag("energy.keepOnDeath",false);
        flag("effects.lowEffects",false); number("effects.particleBudgetPerChunkPerTick",160,16,1024);
        flag("audio.enabled",true); number("audio.minimumIntervalTicks",8,1,100);
        flag("repair.enabled",true); number("repair.fullEnergyIntervalTicks",200,20,24000); number("repair.amount",10,1,1000);
        fraction("repair.eliteChance",.25,0,1); flag("repair.stackWithMending",false);
        flag("recycling.enabled",true); number("reinforcement.maxLevel",5,1,5); flag("reinforcement.enabled",true);
        flag("guide.autoGift",true); flag("names.dynamic",true);
        for(String name:List.of("nightEnergy","fullMoonDamage","thunderChain","endHalo","netherDecay","beaconShield","amethystResonance","fallingSlash")) flag("environment."+name,false);
        flag("worldgen.enabled",true); number("worldgen.veinSize",6,1,64); number("worldgen.veinsPerChunk",6,0,64);
        number("worldgen.minY",-32,-64,319); number("worldgen.maxY",48,-64,319);
        number("worldgen.endVeinSize",5,1,64); number("worldgen.endVeinsPerChunk",3,0,64);
        number("worldgen.endMinY",0,-64,319); number("worldgen.endMaxY",80,-64,319);
    }
    public static final ForgeConfigSpec SPEC = B.build();
    private static void number(String k,int v,int min,int max) { INTS.put(k,B.defineInRange(k,v,min,max)); }
    private static void fraction(String k,double v,double min,double max) { DOUBLES.put(k,B.defineInRange(k,v,min,max)); }
    private static void flag(String k,boolean v) { BOOLS.put(k,B.define(k,v)); }
    public static int i(String k) { var v=INTS.get(k); return SPEC.isLoaded()?v.get():v.getDefault(); }
    public static double d(String k) { var v=DOUBLES.get(k); return SPEC.isLoaded()?v.get():v.getDefault(); }
    public static boolean enabled(String k) { var v=BOOLS.get(k); return SPEC.isLoaded()?v.get():v.getDefault(); }
    private ModSettings() {}
}
