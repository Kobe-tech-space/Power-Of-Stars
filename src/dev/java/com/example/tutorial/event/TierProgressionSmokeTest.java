package com.example.tutorial.event;

import com.example.tutorial.TutorialMod;
import com.example.tutorial.attribute.ModAttributes;
import com.example.tutorial.item.ModItems;
import com.example.tutorial.item.StarArmorItem;
import com.example.tutorial.network.ActivateStarSkillPacket;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Isolated dev-world assertions; excluded from ordinary and release builds. */
@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID)
public final class TierProgressionSmokeTest
{
    private static int ticks;
    private static void check(boolean condition, String message) { if (!condition) throw new IllegalStateException("TIER_TEST: " + message); }
    private static void near(double actual, double expected, String message) { check(Math.abs(actual-expected) < .0001, message + " got " + actual); }
    private static void equip(ServerPlayer player, boolean high)
    {
        player.setItemSlot(EquipmentSlot.HEAD, (high ? ModItems.COSMIC_HELMET : ModItems.STAR_HELMET).get().getDefaultInstance());
        player.setItemSlot(EquipmentSlot.CHEST, (high ? ModItems.COSMIC_CHESTPLATE : ModItems.STAR_CHESTPLATE).get().getDefaultInstance());
        player.setItemSlot(EquipmentSlot.LEGS, (high ? ModItems.COSMIC_LEGGINGS : ModItems.STAR_LEGGINGS).get().getDefaultInstance());
        player.setItemSlot(EquipmentSlot.FEET, (high ? ModItems.COSMIC_BOOTS : ModItems.STAR_BOOTS).get().getDefaultInstance());
        player.containerMenu.broadcastChanges();
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void tick(TickEvent.PlayerTickEvent event)
    {
        if (!Boolean.getBoolean("tutorial_mod.verifyTiers") || event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)) return;
        ++ticks;
        if (ticks == 10)
        {
            player.setGameMode(GameType.SURVIVAL);
            player.serverLevel().getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, player.server);
            player.serverLevel().setDayTime(6000);
            for (BlockPos pos : BlockPos.betweenClosed(-6, 200, -6, 6, 200, 6)) player.serverLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            player.connection.teleport(.5, 201, .5, 0, 0);
            player.removeAllEffects();
            player.getPersistentData().remove(StarDomains.BREAK_CD);
            player.getPersistentData().remove(StarDomains.CONTROL_CD);
            player.getPersistentData().remove(StarDomains.BARRIER);
            player.getPersistentData().remove(StarDomains.END);
            player.getPersistentData().remove(StarDomains.KIND);
            player.getPersistentData().putFloat("tutorial_mod:energy_shield", 0);
            equip(player, false);
            player.setItemSlot(EquipmentSlot.MAINHAND, ModItems.ICE_SWORD.get().getDefaultInstance());
            StarEnergy.set(player, 250);
            player.setAbsorptionAmount(25);
        }
        if (ticks == 40)
        {
            near(player.getAttributeValue(Attributes.ARMOR), 100, "low armor");
            near(player.getAttributeValue(ModAttributes.DAMAGE_IMMUNITY.get()), .3, "low immunity");
            near(player.getAttributeValue(ModAttributes.DAMAGE_BONUS.get()), .13, "low damage");
            near(player.getAttributeValue(Attributes.ATTACK_SPEED), 3.39, "low speed including vanilla Haste II");
            check(StarEnergy.maximum(player) == 250, "low cap");
            StarEnergy.set(player, 149);
            ActivateStarSkillPacket.activate(player, ActivateStarSkillPacket.Skill.ARMOR_SET);
            check(StarEnergy.get(player) == 149 && StarArmorEventHandler.getStellarShield(player) == 0, "insufficient shield energy");
            StarEnergy.set(player, 150);
            ActivateStarSkillPacket.activate(player, ActivateStarSkillPacket.Skill.ARMOR_SET);
            check(StarEnergy.get(player) == 0 && StarArmorEventHandler.getStellarShield(player) == 50, "paid shield");
            near(player.getAbsorptionAmount(), 25, "golden apple absorption unaffected");
            check(player.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() == 1, "low strength II");
            check(player.hasEffect(MobEffects.DAMAGE_RESISTANCE) && player.hasEffect(MobEffects.NIGHT_VISION), "low effects");
            var fall = new LivingFallEvent(player, 20, 1);
            MinecraftForge.EVENT_BUS.post(fall);
            near(fall.getDamageMultiplier(), .2, "fall buffering X");
            LogUtils.getLogger().info("TIER_LOW_PASS: armor100, immunity30%, additive13%, cap250, shield150->50, effects");
        }
        if (ticks == 50) player.setItemSlot(EquipmentSlot.HEAD, ModItems.COSMIC_HELMET.get().getDefaultInstance());
        if (ticks == 55)
        {
            check(StarArmorItem.fullSetTier(player) == 0 && StarEnergy.maximum(player) == 0, "mixed set gating");
            StarEnergy.add(player, 50);
            check(StarEnergy.get(player) == 0 && StarArmorEventHandler.getStellarShield(player) == 0, "mixed resource clear");
            near(player.getAttributeValue(Attributes.ARMOR), 110, "mixed attributes only");
            check(!player.hasEffect(MobEffects.DAMAGE_BOOST) && !player.hasEffect(MobEffects.NIGHT_VISION), "mixed effects removed");
            LogUtils.getLogger().info("TIER_MIXED_PASS: no set bonuses, effects or energy");
        }
        if (ticks == 60) equip(player, true);
        if (ticks == 70)
        {
            near(player.getAttributeValue(Attributes.ARMOR), 250, "high armor");
            near(player.getAttributeValue(ModAttributes.DAMAGE_IMMUNITY.get()), .6, "high immunity");
            near(player.getAttributeValue(ModAttributes.DAMAGE_BONUS.get()), .30, "high additive damage");
            near(player.getAttributeValue(Attributes.ATTACK_SPEED), 6.5, "high speed including vanilla Haste X");
            check(StarEnergy.maximum(player) == 1000, "high cap");
            var enchanted = ModItems.ICE_SWORD.get().getDefaultInstance();
            enchanted.enchant(com.example.tutorial.enchantment.ModEnchantments.SUPER_SHARPNESS.get(), 1);
            enchanted.enchant(com.example.tutorial.enchantment.ModEnchantments.ULTIMATE_SHARPNESS.get(), 1);
            enchanted.enchant(com.example.tutorial.enchantment.ModEnchantments.SUPREME_SHARPNESS.get(), 1);
            near(com.example.tutorial.enchantment.ModEnchantments.getDamageFactor(enchanted, .3), 4,
                    "armor .3 + super .2 + ultimate .5, then independent supreme x2");
            check(player.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() == 9 && player.getEffect(MobEffects.DIG_SPEED).getAmplifier() == 9, "high X effects");
            check(player.hasEffect(MobEffects.FIRE_RESISTANCE) && !player.hasEffect(MobEffects.DAMAGE_RESISTANCE), "high fire replaces low resistance");
            var fall = new LivingFallEvent(player, 50, 1);
            check(MinecraftForge.EVENT_BUS.post(fall), "high fall immunity");
            verifySmithing(player);
            double damage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            double speed = player.getAttributeValue(Attributes.ATTACK_SPEED);
            StarEnergy.set(player, 999);
            check(!StarDomains.activate(player) && StarEnergy.get(player) == 999, "domain requires all 1000 energy");
            StarEnergy.set(player, 1000);
            check(StarDomains.activate(player), "break activation");
            near(player.getAttributeValue(Attributes.ATTACK_DAMAGE), damage * 3, "independent attack damage");
            near(player.getAttributeValue(Attributes.ATTACK_SPEED), speed * 3, "independent attack speed");
            near(StarDomains.damageFactor(player), 1.5, "independent outgoing damage");
            check(StarEnergy.get(player) == 0 && StarDomains.cooldown(player, false) == 6000, "all energy and 300 second CD");
            LogUtils.getLogger().info("TIER_HIGH_BREAK_PASS: armor250, immunity60%, additive30%, cap1000, x3/x3/x1.5");
        }
        if (ticks == 75)
        {
            StarEnergy.set(player, 10);
            check(!StarDomains.activate(player) && StarEnergy.get(player) == 10, "cooldown blocks without consuming");
        }
        if (ticks == 80)
        {
            player.getMainHandItem().getOrCreateTag().putBoolean("tutorial_mod:ranged_mode", true);
            StarEnergy.set(player, 1000);
            check(StarDomains.activate(player), "control activation");
            check(StarDomains.kind(player) == 2 && StarDomains.barrier(player) == 1000 && StarEnergy.get(player) == 0, "control state");
            near(player.getAttributeValue(Attributes.ATTACK_SPEED), 6.5, "break speed cleaned on switch");
            check(StarDomains.cooldown(player, true) == 6000, "300 second control CD");
            check(!player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1)), "negative effect denied");
            check(!player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0)), "darkness denied");
            check(!player.addEffect(new MobEffectInstance(MobEffects.BAD_OMEN, 200, 0)), "neutral negative denied");
            player.forceAddEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0), null);
            check(!player.hasEffect(MobEffects.DARKNESS), "force-add darkness denied");
            var warden = net.minecraft.world.entity.EntityType.WARDEN.create(player.level());
            net.minecraft.world.effect.MobEffectUtil.addEffectToPlayersAround(player.serverLevel(), warden,
                    player.position(), 16, new MobEffectInstance(MobEffects.DARKNESS, 260, 0), 200);
            check(!player.hasEffect(MobEffects.DARKNESS), "actual warden-style area darkness denied");
            check(player.hasEffect(com.example.tutorial.effect.ModEffects.DOMINION.get()), "real potion status visible");
            check(!player.hasEffect(com.example.tutorial.effect.ModEffects.ARMY_BREAKER.get()), "old domain status removed");
            if (warden != null) warden.discard();
            var big = new LivingAttackEvent(player, player.damageSources().magic(), 2);
            var small = new LivingAttackEvent(player, player.damageSources().magic(), 1);
            check(MinecraftForge.EVENT_BUS.post(big), "raw damage above 1 blocked");
            check(!MinecraftForge.EVENT_BUS.post(small), "damage of 1 allowed");
            var finalBig = new LivingDamageEvent(player, player.damageSources().magic(), 2);
            check(MinecraftForge.EVENT_BUS.post(finalBig) && StarDomains.barrier(player) == 1000,
                    "final damage fallback does not consume barrier");
            verifyElites(player);
            LogUtils.getLogger().info("TIER_CONTROL_PASS: shield1000, damage threshold, harmful effect immunity, separate CD");
        }
        if (ticks == 285)
        {
            check(StarDomains.kind(player) == 0 && StarDomains.barrier(player) == 1000, "barrier outlives ten second domain");
            near(player.getAttributeValue(ModAttributes.DOMAIN.get()), 0, "visible domain cleared");
            check(!player.hasEffect(com.example.tutorial.effect.ModEffects.DOMINION.get()), "potion marker expires");
            check(player.addEffect(new MobEffectInstance(MobEffects.DARKNESS,40,0)), "darkness returns outside domain, as designed");
            player.removeEffect(MobEffects.DARKNESS);
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }
        if (ticks == 290)
        {
            check(StarDomains.barrier(player) == 1000, "barrier survives unequip");
            var hit = new LivingDamageEvent(player, player.damageSources().magic(), 600);
            MinecraftForge.EVENT_BUS.post(hit);
            near(StarDomains.barrier(player), 400, "barrier takes damage"); near(hit.getAmount(), 0, "barrier absorption");
            var overflow = new LivingDamageEvent(player, player.damageSources().magic(), 450);
            MinecraftForge.EVENT_BUS.post(overflow);
            near(StarDomains.barrier(player), 0, "barrier shatters"); near(overflow.getAmount(), 50, "overflow damage");
            LogUtils.getLogger().info("TIER_BARRIER_PASS: survives expiration/unequip; exact depletion and overflow");
        }
        if (ticks == 300)
        {
            equip(player, true);
            player.setGameMode(GameType.CREATIVE);
            StarEnergy.set(player, 0);
        }
        if (ticks == 305)
        {
            check(StarDomains.activate(player), "creative zero-energy activation");
            check(StarDomains.cooldown(player, true) == 0, "creative no CD");
        }
        if (ticks == 310)
        {
            check(StarDomains.activate(player), "creative repeat no CD");
            LogUtils.getLogger().info("TIER_CREATIVE_PASS: no energy requirement and no cooldown");
        }
        if (ticks == 320)
        {
            player.getMainHandItem().getOrCreateTag().putBoolean("tutorial_mod:ranged_mode", false);
            StarDomains.activate(player);
            StarEnergy.set(player, 1000);
            StarArmorEventHandler.restoreStellarShield(player);
            LogUtils.getLogger().info("TIER_PROGRESSION_SMOKE_PASS: all server assertions completed");
            if (AttributeCompatibility.hasApothicAttributes()) {
                near(AttributeCompatibility.extraArmorReduction(250),0,"Apothic no double armor reduction");
                LogUtils.getLogger().info("APOTHIC_COMPAT_PASS: 1.3.7 loaded, tier stats preserved, legacy armor compensation disabled");
            }
        }
    }
    private static void verifyElites(ServerPlayer player)
    {
        for(int type=0;type<4;type++) {
            var mob=net.minecraft.world.entity.EntityType.ZOMBIE.create(player.level());
            check(mob!=null,"elite test mob");
            AstralEliteHandler.empower(mob,type);
            near(mob.getMaxHealth(),120,"elite sixfold health");
            double health=mob.getMaxHealth(),armor=mob.getAttributeValue(Attributes.ARMOR);
            AstralEliteHandler.empower(mob,type);
            near(mob.getMaxHealth(),health,"elite empowerment idempotent");
            near(mob.getAttributeValue(Attributes.ARMOR),armor,"elite armor no repeated stacking");
            if(type==1) near(mob.getAbsorptionAmount(),80,"elite shield80");
            mob.setPos(player.getX()+3,player.getY(),player.getZ());
            mob.setTarget(player);
            mob.tickCount=type==0?60:type==2?80:120;
            if(type==0) mob.setHealth(mob.getMaxHealth()*.5F);
            if(type==1) mob.setAbsorptionAmount(0);
            player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            AstralEliteHandler.onTick(new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(mob));
            if(type==0) check(mob.hasEffect(MobEffects.DAMAGE_BOOST),"berserker burst");
            if(type==1) near(mob.getAbsorptionAmount(),10,"shield out-of-combat regeneration");
            if(type==2) check(mob.getDeltaMovement().lengthSqr()>.1,"swift lunge");
            if(type==3) {
                near(player.getDeltaMovement().lengthSqr(),0,"Dominion blocks elite gravity pull");
                check(!player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),"Dominion blocks elite pulse debuff");
                near(StarDomains.barrier(player),1000,"Dominion blocks elite pulse damage");
                var victim=net.minecraft.world.entity.EntityType.ZOMBIE.create(player.level());
                check(victim!=null,"gravity target");
                victim.setPos(player.position());
                mob.setTarget(victim);
                mob.tickCount=130;
                AstralEliteHandler.onTick(new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(mob));
                check(victim.getDeltaMovement().x>0,"gravity pulls unprotected targets");
                victim.discard();
            }
            player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            mob.discard();
        }
        LogUtils.getLogger().info("ELITE_REFINEMENT_PASS: all four types, burst/regen/lunge/pull skills, no repeated stacking");
        var boosted=net.minecraft.world.entity.EntityType.ZOMBIE.create(player.level());
        check(boosted!=null,"modified elite test mob");
        boosted.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                java.util.UUID.randomUUID(),"other_mod_health",1,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE));
        AstralEliteHandler.empower(boosted,0);
        near(boosted.getMaxHealth(),240,"external health bonus is not multiplied twice");
        boosted.discard();
    }
    private static void verifySmithing(ServerPlayer player)
    {
        for (String part : new String[]{"helmet", "chestplate", "leggings", "boots"})
        {
            var id = ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID, "cosmic_" + part);
            var recipe = (SmithingRecipe)player.server.getRecipeManager().byKey(id).orElseThrow();
            var low = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID, "star_" + part));
            var base = new ItemStack(low);
            base.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
            base.setDamageValue(321);
            base.setHoverName(Component.literal("Ascension test"));
            var container = new SimpleContainer(ModItems.STAR_UPGRADE_TEMPLATE.get().getDefaultInstance(), base, ModItems.STAR_SINGULARITY.get().getDefaultInstance());
            check(recipe.matches(container, player.level()), "smithing ingredient order: " + part);
            var result = recipe.assemble(container, player.level().registryAccess());
            check(result.getItem() instanceof StarArmorItem armor && armor.isAdvanced(), "smithing result high");
            check(result.getDamageValue() == 321 && result.getEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION) == 4
                    && result.getHoverName().getString().equals("Ascension test"), "smithing preserves NBT");
        }
        LogUtils.getLogger().info("TIER_SMITHING_PASS: all four recipes preserve enchantments, name and damage");
    }
}
