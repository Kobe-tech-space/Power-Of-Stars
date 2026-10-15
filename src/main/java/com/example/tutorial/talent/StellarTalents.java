package com.example.tutorial.talent;

import com.example.tutorial.TutorialMod;
import com.example.tutorial.config.ModSettings;
import com.example.tutorial.enchantment.ModEnchantments;
import com.example.tutorial.entity.AstralSlashEntity;
import com.example.tutorial.event.*;
import com.example.tutorial.item.*;
import com.example.tutorial.network.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Server-owned progression. No external code or textures are bundled. */
@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID)
public final class StellarTalents {
    private static final String SAVE = "tutorial_mod:stellar_talents";
    private static CompoundTag data(ServerPlayer p) {
        if (!p.getPersistentData().contains(SAVE)) p.getPersistentData().put(SAVE, new CompoundTag());
        return p.getPersistentData().getCompound(SAVE);
    }
    public static int[] ranks(ServerPlayer p) { return TalentRules.sanitize(data(p).getIntArray("Ranks")); }
    public static int rank(ServerPlayer p, int node) { return ModSettings.enabled("talents.enabled") ? ranks(p)[node] : 0; }
    public static double damageBonus(ServerPlayer p) {
        double value = .04 * rank(p, 0);
        if (StarEnergy.maximum(p) > 0 && StarEnergy.get(p) == StarEnergy.maximum(p)) value += .08 * rank(p, 3);
        return value;
    }
    public static int experience(ServerPlayer p) { return Math.max(0, Math.min(20000, data(p).getInt("Experience"))); }
    public static void sync(ServerPlayer p, boolean open) {
        ModNetwork.sendToPlayer(p, new TalentSnapshotPacket(open, experience(p), ranks(p), ModSettings.enabled("talents.enabled"),
                ModSettings.i("talents.resetLevels"), ModSettings.i("talents.resetIngots")));
    }
    public static void award(ServerPlayer p, int amount) {
        if (!ModSettings.enabled("talents.enabled") || amount <= 0 || TalentRules.earned(experience(p)) >= TalentRules.MAX_POINTS) return;
        int old = TalentRules.earned(experience(p));
        data(p).putInt("Experience", (int)Math.min(20000L, (long)experience(p) + amount));
        int gained = TalentRules.earned(experience(p)) - old;
        if (gained > 0) p.sendSystemMessage(Component.translatable("talent.tutorial_mod.earned", gained));
        sync(p, false);
    }
    public static void action(ServerPlayer p, int action, int node) {
        if (action == 0) { sync(p, true); return; }
        if (!ModSettings.enabled("talents.enabled")) { sync(p, false); return; }
        int[] ranks = ranks(p);
        if (action == 1 && TalentRules.canLearn(experience(p), ranks, node)) {
            ranks[node]++; data(p).putIntArray("Ranks", ranks);
        } else if (action == 2 && TalentRules.spent(ranks) > 0) {
            int levels = ModSettings.i("talents.resetLevels"), ingots = ModSettings.i("talents.resetIngots");
            if (!p.isCreative() && (p.experienceLevel < levels || p.getInventory().countItem(ModItems.STAR_INGOT.get()) < ingots)) {
                p.sendSystemMessage(Component.translatable("talent.tutorial_mod.reset_fail", levels, ingots));
            } else {
                if (!p.isCreative()) {
                    p.giveExperienceLevels(-levels);
                    int remaining = ingots;
                    for (int i = 0; i < p.getInventory().getContainerSize() && remaining > 0; i++) {
                        ItemStack s = p.getInventory().getItem(i);
                        if (s.is(ModItems.STAR_INGOT.get())) { int take = Math.min(remaining, s.getCount()); s.shrink(take); remaining -= take; }
                    }
                    p.getInventory().setChanged();
                }
                data(p).putIntArray("Ranks", new int[TalentRules.NODES]);
            }
        }
        updateAttributes(p); sync(p, false);
    }
    private static void attribute(ServerPlayer p, Attribute type, String name, double amount, AttributeModifier.Operation op) {
        var instance = p.getAttribute(type); if (instance == null) return;
        UUID id = UUID.nameUUIDFromBytes((SAVE + ":" + name).getBytes(StandardCharsets.UTF_8));
        var current = instance.getModifier(id);
        if (current != null && current.getAmount() == amount && current.getOperation() == op) return;
        if (current != null) instance.removeModifier(id);
        if (amount != 0) instance.addTransientModifier(new AttributeModifier(id, name, amount, op));
    }
    public static void updateAttributes(ServerPlayer p) {
        boolean sword = p.getMainHandItem().getItem() instanceof AstralGreatswordItem;
        boolean set = StarArmorItem.hasFullSet(p);
        int ward = enchant(p.getItemBySlot(EquipmentSlot.CHEST), ModEnchantments.STAR_WARD.get());
        int stride = enchant(p.getItemBySlot(EquipmentSlot.FEET), ModEnchantments.STAR_STRIDE.get());
        attribute(p, Attributes.ATTACK_SPEED, "talent_haste", sword ? .03 * rank(p, 1) : 0, AttributeModifier.Operation.MULTIPLY_BASE);
        attribute(p, Attributes.ARMOR, "talent_armor", set ? 4 * rank(p, 4) : 0, AttributeModifier.Operation.ADDITION);
        attribute(p, Attributes.MAX_HEALTH, "talent_health", set ? 10 * rank(p, 5) : 0, AttributeModifier.Operation.ADDITION);
        attribute(p, com.example.tutorial.attribute.ModAttributes.DAMAGE_IMMUNITY.get(), "talent_ward", (set ? .01 * rank(p, 6) : 0) + .01 * ward, AttributeModifier.Operation.ADDITION);
        attribute(p, Attributes.MOVEMENT_SPEED, "talent_speed", (set ? .03 * rank(p, 10) : 0) + .03 * stride, AttributeModifier.Operation.MULTIPLY_BASE);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }
    private static int enchant(ItemStack stack, net.minecraft.world.item.enchantment.Enchantment enchantment) {
        return enchantment.canEnchant(stack) ? Math.max(0, Math.min(3, stack.getEnchantmentLevel(enchantment))) : 0;
    }
    private static ItemStack weapon(net.minecraft.world.damagesource.DamageSource source, ServerPlayer p) {
        if (source.getDirectEntity() instanceof AstralSlashEntity wave) return wave.stellarWeaponSnapshot();
        return com.example.tutorial.damage.ModDamageTypes.isDirectMelee(source) ? p.getMainHandItem() : ItemStack.EMPTY;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void death(LivingDeathEvent e) {
        if (e.isCanceled() || !(e.getSource().getEntity() instanceof ServerPlayer p) || !AstralSafety.valid(p)
                || !(e.getEntity() instanceof Monster) || e.getEntity().getPersistentData().getBoolean(SAVE + ":awarded")) return;
        if (!(weapon(e.getSource(), p).getItem() instanceof AstralGreatswordItem)) return;
        e.getEntity().getPersistentData().putBoolean(SAVE + ":awarded", true);
        award(p, ModSettings.i("talents.killExperience"));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void hit(LivingDamageEvent e) {
        if (e.isCanceled() || !(e.getSource().getEntity() instanceof ServerPlayer p) || !AstralSafety.valid(p) || e.getAmount() <= 0
                || !Float.isFinite(e.getAmount()) || !(e.getEntity() instanceof Monster)) return;
        ItemStack stack = weapon(e.getSource(), p);
        if (!(stack.getItem() instanceof AstralGreatswordItem)) return;
        long now = p.server.overworld().getGameTime(); CompoundTag d = data(p);
        // Physical/magic halves and rapid fire share a bounded one-second proc window.
        if (d.contains("LastProc") && now >= d.getLong("LastProc") && now - d.getLong("LastProc") < 20) return;
        d.putLong("LastProc", now);
        int gather = enchant(stack, ModEnchantments.STAR_GATHERING.get());
        if (gather > 0) StarEnergy.add(p, gather);
        double steal = .01 * enchant(stack, ModEnchantments.STAR_LEECH.get()) + .005 * rank(p, 2);
        if (steal > 0) p.heal((float)Math.min(2, Math.min(e.getEntity().getHealth(), e.getAmount()) * steal));
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || !AstralSafety.valid(p)) return;
        if (p.tickCount % 20 != 0) return;
        updateAttributes(p);
        if (!StarArmorItem.hasFullSet(p)) return;
        int shield = rank(p, 7);
        if (shield > 0 && StarArmorEventHandler.needsShieldRestore(p) && StarEnergy.consume(p, shield * 2))
            StarArmorEventHandler.setShield(p, StarArmorEventHandler.getStellarShield(p) + shield);
        if (p.tickCount % 100 == 0 && rank(p, 9) > 0) StarEnergy.add(p, rank(p, 9));
        if (p.tickCount % 200 == 0 && ModSettings.enabled("repair.enabled")) {
            int mend = enchant(p.getItemBySlot(EquipmentSlot.CHEST), ModEnchantments.STAR_MENDING.get()) + rank(p, 11);
            if (mend > 0) for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = p.getItemBySlot(slot);
                if (!(stack.getItem() instanceof StarArmorItem || stack.getItem() instanceof AstralGreatswordItem) || !stack.isDamaged()) continue;
                if (!ModSettings.enabled("repair.stackWithMending") && stack.getEnchantmentLevel(Enchantments.MENDING) > 0) continue;
                if (StarEnergy.consume(p, 1)) stack.setDamageValue(Math.max(0, stack.getDamageValue() - mend));
            }
        }
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone e) {
        if (e.getOriginal().getPersistentData().contains(SAVE)) {
            CompoundTag copy = e.getOriginal().getPersistentData().getCompound(SAVE).copy(); copy.remove("LastProc");
            e.getEntity().getPersistentData().put(SAVE, copy);
        }
    }
    @SubscribeEvent public static void equipment(LivingEquipmentChangeEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) updateAttributes(p);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e) { if(e.getEntity() instanceof ServerPlayer p) {updateAttributes(p);sync(p,false);} }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e) { if(e.getEntity() instanceof ServerPlayer p) {updateAttributes(p);sync(p,false);} }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { if(e.getEntity() instanceof ServerPlayer p) {updateAttributes(p);sync(p,false);} }
    @SubscribeEvent public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(net.minecraft.commands.Commands.literal("astral")
                .then(net.minecraft.commands.Commands.literal("talents").executes(c -> { sync(c.getSource().getPlayerOrException(), true); return 1; })));
    }
    private StellarTalents() {}
}
