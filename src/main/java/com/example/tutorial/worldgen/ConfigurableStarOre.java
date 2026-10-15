package com.example.tutorial.worldgen;
import com.example.tutorial.TutorialMod;
import com.example.tutorial.config.ModSettings;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraftforge.registries.*;
import java.util.stream.Stream;
public final class ConfigurableStarOre extends OreFeature {
    public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(Registries.FEATURE,TutorialMod.MOD_ID);
    public static final RegistryObject<Feature<OreConfiguration>> ORE=FEATURES.register("configurable_star_ore",ConfigurableStarOre::new);
    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENTS=DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE,TutorialMod.MOD_ID);
    public static final RegistryObject<PlacementModifierType<Placement>> PLACEMENT=PLACEMENTS.register("star_ore_distribution",()->()->Placement.CODEC);
    private ConfigurableStarOre() {super(OreConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<OreConfiguration> c) {
        if(!ModSettings.enabled("worldgen.enabled")) return false;
        boolean end=c.level().getLevel().dimension()==net.minecraft.world.level.Level.END;
        var config=new OreConfiguration(c.config().targetStates,ModSettings.i(end?"worldgen.endVeinSize":"worldgen.veinSize"),c.config().discardChanceOnAirExposure);
        return super.place(new FeaturePlaceContext<>(c.topFeature(),c.level(),c.chunkGenerator(),c.random(),c.origin(),config));
    }
    public static final class Placement extends PlacementModifier {
        public static final Codec<Placement> CODEC=Codec.BOOL.fieldOf("end").xmap(Placement::new,p->p.end).codec();
        private final boolean end;
        public Placement(boolean end) {this.end=end;}
        @Override public Stream<BlockPos> getPositions(PlacementContext c,RandomSource random,BlockPos origin) {
            if(!ModSettings.enabled("worldgen.enabled")) return Stream.empty();
            int count=ModSettings.i(end?"worldgen.endVeinsPerChunk":"worldgen.veinsPerChunk");
            int a=ModSettings.i(end?"worldgen.endMinY":"worldgen.minY"),b=ModSettings.i(end?"worldgen.endMaxY":"worldgen.maxY");
            int min=Math.max(c.getLevel().getMinBuildHeight(),Math.min(a,b)),max=Math.min(c.getLevel().getMaxBuildHeight()-1,Math.max(a,b));
            if(max<min) return Stream.empty();
            return java.util.stream.IntStream.range(0,count).mapToObj(i->new BlockPos(origin.getX()+random.nextInt(16),min+random.nextInt(max-min+1),origin.getZ()+random.nextInt(16)));
        }
        @Override public PlacementModifierType<?> type() {return PLACEMENT.get();}
    }
}
