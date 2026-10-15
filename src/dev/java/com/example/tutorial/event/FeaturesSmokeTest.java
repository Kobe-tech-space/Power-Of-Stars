package com.example.tutorial.event;
import com.example.tutorial.TutorialMod;
import com.example.tutorial.config.ModSettings;
import com.example.tutorial.item.*;
import com.example.tutorial.recipe.AstralRecyclingRecipe;
import com.example.tutorial.entity.AstralSlashEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=TutorialMod.MOD_ID)
public final class FeaturesSmokeTest {
    private static int ticks;
    private static int baselineHits,baselineKills;
    private static void check(boolean value,String message) {if(!value) throw new IllegalStateException("FEATURE_TEST: "+message);}
    private static ResourceLocation id(String path) {return ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID,path);}
    private static void equip(ServerPlayer p) {
        p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.STAR_HELMET.get()));p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(ModItems.STAR_CHESTPLATE.get()));
        p.setItemSlot(EquipmentSlot.LEGS,new ItemStack(ModItems.STAR_LEGGINGS.get()));p.setItemSlot(EquipmentSlot.FEET,new ItemStack(ModItems.STAR_BOOTS.get()));
    }
    public static final class Wave extends AstralSlashEntity {
        public Wave(ServerPlayer p,float damage) {super(p.serverLevel(),p,damage,true,false,new Vec3(0,0,1),1);}
        public void hit(LivingEntity target) {onHitEntity(new EntityHitResult(target));}
    }
    private static LivingEntity zombie(ServerPlayer p,int index) {
        var mob=EntityType.ZOMBIE.create(p.serverLevel());mob.setNoAi(true);mob.getPersistentData().putBoolean("tutorial_mod:elite_rolled",true);
        mob.moveTo(p.getX()+index+2,p.getY(),p.getZ()+4,0,0);p.serverLevel().addFreshEntity(mob);return mob;
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if(!Boolean.getBoolean("tutorial_mod.verifyFeatures") || e.phase!=TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        ++ticks;
        if(ticks==10) {
            for(String name:java.util.List.of("root","ore","ingot","sword","ranged","slash_hit","critical_slash","full_set","energy_100","triple_slash","shield_full","reforge","elite","trial","mastery","combo_150","multi_kill","last_stand","guide_gift")) {
                var advancement=p.server.getAdvancements().getAdvancement(id("astral/"+name));
                var progress=p.getAdvancements().getOrStartProgress(advancement);
                var completed=new java.util.ArrayList<String>();progress.getCompletedCriteria().forEach(completed::add);
                for(String criterion:completed) p.getAdvancements().revoke(advancement,criterion);
            }
            p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.removeAllEffects();equip(p);p.setHealth(20);p.getFoodData().setFoodLevel(20);
            p.getPersistentData().remove(StarDomains.END);p.getPersistentData().remove(StarDomains.KIND);p.getPersistentData().putFloat(StarDomains.BARRIER,0);
            p.serverLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,p.server);
            p.serverLevel().setDayTime(6000);p.serverLevel().setWeatherParameters(6000,0,false,false);
            for(BlockPos pos:BlockPos.betweenClosed(-6,200,-6,6,200,12)) p.serverLevel().setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            for(BlockPos pos:BlockPos.betweenClosed(-1,199,-1,1,199,1)) p.serverLevel().setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState());
            p.serverLevel().setBlockAndUpdate(new BlockPos(0,200,0),net.minecraft.world.level.block.Blocks.BEACON.defaultBlockState());
            p.connection.teleport(.5,201,.5,0,0);
        }
        if(ticks==35) {
            check(ModSettings.SPEC.isLoaded(),"world server config loaded");check(StarEnergy.maximum(p)==250 && StarArmorEventHandler.maximumShield()==50,"defaults preserved");
            var recipe=p.level().getRecipeManager().byKey(id("recycle_star_chestplate")).orElseThrow();check(recipe instanceof AstralRecyclingRecipe,"custom blasting recipe loaded");
            var recycling=(AstralRecyclingRecipe)recipe;var armor=new ItemStack(ModItems.STAR_CHESTPLATE.get());
            check(recycling.count(armor)==2,"full chestplate refunds2");armor.setDamageValue(12500);check(recycling.count(armor)==1,"half durability refunds1");
            armor.enchant(Enchantments.ALL_DAMAGE_PROTECTION,1);check(recycling.count(armor)==0,"enchanted rejected");armor.getTag().remove("Enchantments");armor.setDamageValue(25000);check(recycling.count(armor)==0,"depleted rejected");
            var sword=new ItemStack(ModItems.ICE_SWORD.get());sword.setHoverName(net.minecraft.network.chat.Component.literal("Feature Test Sword"));sword.enchant(Enchantments.MENDING,1);sword.setDamageValue(4321);p.setItemSlot(EquipmentSlot.MAINHAND,sword);
            check(!AstralEquipment.reinforce(p,sword) && AstralEquipment.level(sword)==0,"insufficient reinforcement no mutation");
            p.getInventory().placeItemBackInInventory(new ItemStack(ModItems.STAR_INGOT.get(),64));p.getInventory().placeItemBackInInventory(new ItemStack(Items.DIAMOND,40));p.getInventory().placeItemBackInInventory(new ItemStack(ModItems.STAR_SINGULARITY.get(),2));p.giveExperienceLevels(100-p.experienceLevel);
            for(int i=0;i<5;i++) check(AstralEquipment.reinforce(p,sword),"fixed upgrade "+i);
            check(!AstralEquipment.reinforce(p,sword) && sword.getMaxDamage()==37500,"cap and dynamic durability");
            check(p.experienceLevel==25 && p.getInventory().countItem(ModItems.STAR_INGOT.get())==2 && p.getInventory().countItem(Items.DIAMOND)==10 && p.getInventory().countItem(ModItems.STAR_SINGULARITY.get())==0,"exact cost consumption");
            check(sword.hasCustomHoverName() && sword.getEnchantmentLevel(Enchantments.MENDING)==1 && sword.getDamageValue()==4321,"NBT preserved");
            check(sword.getItem().isValidRepairItem(sword,new ItemStack(ModItems.STAR_INGOT.get())),"star ingot anvil repair");
            StarEnergy.set(p,0);int gained=AstralStatistics.value(p,"energy_gained"),spent=AstralStatistics.value(p,"energy_spent");StarEnergy.add(p,70);StarEnergy.add(p,40);check(StarEnergy.consume(p,30),"resource spend");
            check(AstralStatistics.value(p,"energy_gained")==gained+110 && AstralStatistics.value(p,"energy_spent")==spent+30,"real resource stats");
            StarEnergy.set(p,250);int oldTicks=p.tickCount;p.tickCount=200;sword.setDamageValue(100);
            AstralEquipment.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));check(sword.getDamageValue()==100,"mending not stacked");sword.getTag().remove("Enchantments");
            AstralEquipment.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));check(sword.getDamageValue()==90,"full energy auto repair");p.tickCount=oldTicks;
            p.getPersistentData().remove("tutorial_mod:packet_skill_count");p.getPersistentData().remove("tutorial_mod:packet_slash_count");
            for(int i=0;i<8;i++) check(AstralSafety.allowPacket(p,false),"skill packet allowed");check(!AstralSafety.allowPacket(p,false),"skill packet limited");for(int i=0;i<20;i++) check(AstralSafety.allowPacket(p,true),"slash packet allowed");check(!AstralSafety.allowPacket(p,true),"slash packet limited");
            var feature=p.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE).get(id("star_ore"));check(feature.feature() instanceof com.example.tutorial.worldgen.ConfigurableStarOre,"configured feature codec");
            var placement=new com.example.tutorial.worldgen.ConfigurableStarOre.Placement(false);var context=new net.minecraft.world.level.levelgen.placement.PlacementContext(p.serverLevel(),p.serverLevel().getChunkSource().getGenerator(),java.util.Optional.empty());
            var positions=placement.getPositions(context,net.minecraft.util.RandomSource.create(42),BlockPos.ZERO).toList();check(positions.size()==6 && positions.stream().allMatch(pos->pos.getY()>=-32 && pos.getY()<=48 && pos.getX()>=0 && pos.getX()<16),"configured ore distribution");
            LogUtils.getLogger().info("FEATURE_ECONOMY_PASS: reinforcement costs/NBT/cap, recycling, repair, statistics, packet bounds, configured worldgen");
        }
        if(ticks==60) {
            baselineHits=AstralStatistics.value(p,"slash_hits");baselineKills=AstralStatistics.value(p,"slash_kills");
            var weapon=new ItemStack(ModItems.ICE_SWORD.get());weapon.getOrCreateTag().putInt(AstralEquipment.LEVEL,4);weapon.enchant(Enchantments.MOB_LOOTING,3);weapon.enchant(Enchantments.FIRE_ASPECT,2);weapon.enchant(Enchantments.KNOCKBACK,1);weapon.enchant(Enchantments.SHARPNESS,1);p.setItemSlot(EquipmentSlot.MAINHAND,weapon);
            var wave=new Wave(p,2);p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);check(wave.lootingLevel()==3,"launch looting snapshot");
            var first=zombie(p,0);wave.hit(first);check(first.getRemainingFireTicks()>0 && first.getDeltaMovement().lengthSqr()>0 && !wave.isRemoved(),"fire knockback piercing");
            float health=first.getHealth();wave.hit(first);check(first.getHealth()==health,"no repeated target");
            var looting=new LootingLevelEvent(first,com.example.tutorial.damage.ModDamageTypes.astralSlash(wave,p),0);AstralLooting.looting(looting);check(looting.getLootingLevel()==3,"death drop snapshot");
            var saved=wave.saveWithoutId(new net.minecraft.nbt.CompoundTag());var restored=new Wave(p,2);restored.load(saved);check(restored.lootingLevel()==3,"projectile snapshot persistence");
            var second=zombie(p,1);var third=zombie(p,2);wave.hit(second);check(!wave.isRemoved(),"second target allowed");wave.hit(third);check(wave.isRemoved(),"third target terminates");first.discard();second.discard();third.discard();restored.discard();
            p.setItemSlot(EquipmentSlot.MAINHAND,weapon);var lethal=new Wave(p,1000);p.serverLevel().addFreshEntity(lethal);
            lethal.hit(zombie(p,0));lethal.hit(zombie(p,1));lethal.hit(zombie(p,2));
            var multi=p.server.getAdvancements().getAdvancement(id("astral/multi_kill"));check(p.getAdvancements().getOrStartProgress(multi).isDone(),"actual one-wave multikill advancement");
            check(!AstralSafety.canHit(p,EntityType.ARMOR_STAND.create(p.serverLevel())),"armor stand protected");
            var wolf=EntityType.WOLF.create(p.serverLevel());wolf.setTame(true);wolf.setOwnerUUID(p.getUUID());check(!AstralSafety.canHit(p,wolf),"owned pet protected");
            p.setGameMode(GameType.CREATIVE);check(!AstralSafety.canHit(wolf,p),"creative target protected");p.setGameMode(GameType.SPECTATOR);check(!AstralSafety.valid(p),"spectator cannot cast");p.setGameMode(GameType.SURVIVAL);
            check(AstralStatistics.value(p,"slash_hits")>=baselineHits+6 && AstralStatistics.value(p,"slash_kills")==baselineKills+3 && AstralStatistics.value(p,"max_critical_damage")>0,"damage stats");
            LogUtils.getLogger().info("FEATURE_COMBAT_PASS: vanilla snapshot enchantments, projectile persistence, pierce deduplication, actual multikill, safe targets, statistics");
        }
        if(ticks==85) {check(p.getInventory().countItem(ModItems.ASTRAL_GUIDE.get())==1,"one guide gift");FeatureOptionsSmokeTest.verify(p);WeaponMastery.add(p.getMainHandItem(),WeaponMastery.MAX_LEVEL*100);p.getAdvancements().setSelectedTab(p.server.getAdvancements().getAdvancement(id("astral/root")));LogUtils.getLogger().info("FEATURE_GUIDE_PASS: once-only Patchouli gift and advancement tree");}
    }
}
