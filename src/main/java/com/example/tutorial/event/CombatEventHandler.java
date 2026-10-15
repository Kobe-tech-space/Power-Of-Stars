package com.example.tutorial.event;

import com.example.tutorial.TutorialMod;
import com.example.tutorial.damage.ModDamageTypes;
import com.example.tutorial.enchantment.AttackSpeedEnchantment;
import com.example.tutorial.enchantment.ModEnchantments;
import com.example.tutorial.entity.AstralSlashEntity;
import com.example.tutorial.item.ModItems;
import com.example.tutorial.network.CombatFeedbackPacket;
import com.example.tutorial.network.ModNetwork;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 两条互不相干的加成通道：
 *
 * <ul>
 *   <li><b>攻击速度</b> —— 走 {@link ItemAttributeModifierEvent} 挂到 ATTACK_SPEED 属性上，
 *       所以会显示在物品提示框里。</li>
 *   <li><b>伤害</b> —— 走 {@link LivingHurtEvent} 直接放大最终伤害。
 *       不走属性，因此不受 ATTACK_DAMAGE 的 0~2048 上限夹取，
 *       代价是提示框里不显示任何伤害数字（这是用户明确选择的）。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CombatEventHandler
{
    /** Lives only for the synchronous LivingHurt -> LivingDamage portion of one hit. */
    private static final Map<LivingEntity, PendingFeedback> PENDING_FEEDBACK = new WeakHashMap<>();

    // ===== 攻击速度：挂到 ATTACK_SPEED 属性上 =====

    @SubscribeEvent
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event)
    {
        // 只有主手槽位才影响攻速
        if (event.getSlotType() != EquipmentSlot.MAINHAND)
        {
            return;
        }

        ItemStack stack = event.getItemStack();
        // 每个攻速附魔自带运算类型（固定值 / 百分比）和固定 UUID，这里遍历挂上即可
        for (RegistryObject<Enchantment> registryObject : ModEnchantments.ATTACK_SPEED_ENCHANTMENTS)
        {
            Enchantment enchantment = registryObject.get();
            int level = stack.getEnchantmentLevel(enchantment);
            if (level > 0 && enchantment instanceof AttackSpeedEnchantment speed)
            {
                event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(
                        speed.getModifierId(),
                        speed.getModifierName(),
                        speed.getAmount(level),
                        speed.getOperation()));
            }
        }
    }

    // ===== 伤害：直接放大最终伤害 =====

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        // 伤害由逻辑服务端结算，客户端不重复处理
        if (event.getEntity().level().isClientSide())
        {
            return;
        }

        // Clear feedback from a canceled/fully absorbed previous hit before this one.
        PENDING_FEEDBACK.remove(event.getEntity());

        // Sword waves already contain a launch-time snapshot. Never read the player's
        // current weapon here or their enchantments would be applied a second time.
        if (ModDamageTypes.isAstralSlash(event.getSource())
                && event.getSource().getDirectEntity() instanceof AstralSlashEntity slash)
        {
            if (event.getSource().getEntity() instanceof ServerPlayer player
                    && (slash.isCritical() || slash.getFeedbackCombo() > 0))
            {
                PENDING_FEEDBACK.put(event.getEntity(), new PendingFeedback(
                        player, slash.getFeedbackCombo(), slash.getFeedbackMilestone(), slash.isCritical(),
                        event.getSource()));
            }
            return;
        }

        if (!ModDamageTypes.isDirectMelee(event.getSource()))
        {
            if (event.getSource().getEntity() instanceof Player player)
                event.setAmount((float)(event.getAmount() * (1 + AstralSafety.damageBonus(player)) * StarDomains.damageFactor(player)));
            return;
        }

        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof LivingEntity living))
        {
            return;
        }

        ItemStack weapon = living.getMainHandItem();

        // 加算区（超级 / 究极）先相加，再乘独立乘区（至尊）
        double factor = ModEnchantments.getDamageFactor(weapon,AstralSafety.damageBonus(living));
        factor *= StarDomains.damageFactor(living);
        if (weapon.is(ModItems.ICE_SWORD.get()))
        {
            factor *= WeaponMastery.masteryDamage(weapon);
            if (WeaponMastery.branch(weapon) == WeaponMastery.Branch.STAR_BLADE) factor *= 1.15D;
            if (living instanceof Player player) factor *= 1.0D + AstralResonance.level(player) * 0.02D;
        }

        if (living instanceof ServerPlayer player && weapon.is(ModItems.ICE_SWORD.get()))
        {
            long now = player.level().getGameTime();
            long last = player.getPersistentData().getLong("tutorial_mod:last_melee_combo");
            int stage = now - last <= 24L ? player.getPersistentData().getInt("tutorial_mod:melee_combo") % 3 + 1 : 1;
            player.getPersistentData().putLong("tutorial_mod:last_melee_combo", now);
            player.getPersistentData().putInt("tutorial_mod:melee_combo", stage);
            if (stage == 3)
            {
                factor *= 1.35D;
                if (player.level() instanceof ServerLevel level)
                    com.example.tutorial.event.AstralSafety.particles(level,ParticleTypes.SWEEP_ATTACK, event.getEntity().getX(), event.getEntity().getY(0.6D),
                            event.getEntity().getZ(), 6, 0.5D, 0.35D, 0.5D, 0.05D);
            }
        }

        // 越战越勇：只有带着该附魔时才统计连击，且是独立的一层乘算
        int combo = 0;
        int milestone = 0;
        if (living instanceof Player player && ModEnchantments.hasCombo(weapon))
        {
            combo = ComboTracker.onHit(player);
            factor *= 1.0D + ModEnchantments.getComboBonus(combo);
            if (combo == 50 || combo == 100 || combo == 150)
            {
                milestone = combo;
            }
        }

        // 暴击：模组自建的独立掷骰（原版没有暴击率属性）。
        // 触发时按「基础倍率 + 爆伤加成」放大，与原版跳劈暴击互不干涉。
        double critChance = Math.min(1.0D, ModEnchantments.getCritChance(weapon));
        boolean critical = critChance > 0.0D && living.getRandom().nextDouble() < critChance;
        if (critical)
        {
            factor *= ModEnchantments.BASE_CRIT_MULTIPLIER + ModEnchantments.getCritDamageBonus(weapon);
        }

        if (factor > 1.0D)
        {
            event.setAmount((float) (event.getAmount() * factor));
        }

        if (living instanceof ServerPlayer serverPlayer && (combo > 0 || critical))
        {
            PENDING_FEEDBACK.put(event.getEntity(),
                    new PendingFeedback(serverPlayer, combo, milestone, critical, event.getSource()));
        }
    }

    /** Fired after armor, resistance, enchantment protection and absorption are resolved. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event)
    {
        if (event.getAmount() > 0 && ModDamageTypes.isAstralSlash(event.getSource())
                && event.getSource().getDirectEntity() instanceof AstralSlashEntity wave
                && !wave.claimCombatHit(event.getEntity().getUUID())) {
            PENDING_FEEDBACK.remove(event.getEntity());
            return;
        }
        awardHitEnergy(event);
        PendingFeedback feedback = PENDING_FEEDBACK.remove(event.getEntity());
        applyAstralMarkAndMastery(event);
        if (feedback == null || feedback.source() != event.getSource())
        {
            return;
        }
        if(feedback.critical() && !(event.getSource().getDirectEntity() instanceof AstralSlashEntity)) com.example.tutorial.event.AstralStatistics.critical(feedback.player(),event);

        ModNetwork.sendToPlayer(feedback.player(), new CombatFeedbackPacket(
                feedback.combo(), false, feedback.milestone(), 0.0F));

        if (!(event.getEntity().level() instanceof ServerLevel serverLevel))
        {
            return;
        }
        if (feedback.milestone() > 0)
        {
            com.example.tutorial.event.AstralSafety.particles(serverLevel,ParticleTypes.TOTEM_OF_UNDYING,
                    feedback.player().getX(), feedback.player().getY(0.7D), feedback.player().getZ(),
                    28, 0.45D, 0.7D, 0.45D, 0.12D);
            AstralSounds.play(feedback.player(),"combo",SoundEvents.PLAYER_LEVELUP,.9F,
                    feedback.milestone()==50?.85F:feedback.milestone()==100?1F:1.2F);
        }
    }

    /** A sword-wave kill gets a denser, brighter shatter than an ordinary hit. */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event)
    {
        Entity attacker = event.getSource().getEntity();
        boolean swordWaveKill = (ModDamageTypes.isAstralSlash(event.getSource())
                && event.getSource().getDirectEntity() instanceof AstralSlashEntity)
                || event.getSource().is(ModDamageTypes.STAR_BURST);
        boolean meleeSwordKill = ModDamageTypes.isDirectMelee(event.getSource())
                && attacker instanceof LivingEntity living
                && living.getMainHandItem().is(ModItems.ICE_SWORD.get());
        if ((swordWaveKill || meleeSwordKill) && attacker instanceof ServerPlayer player)
        {
            StarEnergy.add(player, com.example.tutorial.Config.energyPerKill);
            WeaponMastery.add(player.getMainHandItem(), 10);
        }
        if ((!swordWaveKill && !meleeSwordKill)
                || !(event.getEntity().level() instanceof ServerLevel serverLevel))
        {
            return;
        }
        LivingEntity target = event.getEntity();
        com.example.tutorial.event.AstralSafety.particles(serverLevel,com.example.tutorial.particle.ModParticles.STAR.get(),
                target.getX(), target.getY(0.55D), target.getZ(),
                52, 0.65D, 0.75D, 0.65D, 0.24D);
        com.example.tutorial.event.AstralSafety.sound(serverLevel, target.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 0.75F, 1.45F);
    }

    private static void awardHitEnergy(LivingDamageEvent event)
    {
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer player) || event.getAmount() <= 0.0F) return;
        boolean swordWave = ModDamageTypes.isAstralSlash(event.getSource())
                && event.getSource().getDirectEntity() instanceof AstralSlashEntity;
        boolean meleeSword = ModDamageTypes.isDirectMelee(event.getSource())
                && player.getMainHandItem().is(ModItems.ICE_SWORD.get());
        if (swordWave || meleeSword)
        {
            int gain = com.example.tutorial.Config.energyPerHit;
            PendingFeedback feedback = PENDING_FEEDBACK.get(event.getEntity());
            boolean critical = feedback != null && feedback.source() == event.getSource() && feedback.critical();
            if (critical) gain += com.example.tutorial.Config.energyPerCritical;
            StarEnergy.add(player, gain);
        }
    }

    private static void applyAstralMarkAndMastery(LivingDamageEvent event)
    {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0.0F) return;
        boolean swordWave = ModDamageTypes.isAstralSlash(event.getSource());
        boolean meleeSword = ModDamageTypes.isDirectMelee(event.getSource())
                && player.getMainHandItem().is(ModItems.ICE_SWORD.get());
        if (!swordWave && !meleeSword) return;
        WeaponMastery.add(player.getMainHandItem(), 2);
        LivingEntity target = event.getEntity();
        long now = target.level().getGameTime();
        var data = target.getPersistentData();
        int marks = now <= data.getLong("tutorial_mod:star_mark_until")
                ? data.getInt("tutorial_mod:star_marks") + 1 : 1;
        data.putLong("tutorial_mod:star_mark_until", now + 120L);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, false));
        if (marks >= 5)
        {
            data.putInt("tutorial_mod:star_marks", 0);
            target.hurt(ModDamageTypes.starBurst(player), 12.0F + WeaponMastery.level(player.getMainHandItem()) * 0.5F);
            if (target.level() instanceof ServerLevel level)
                com.example.tutorial.event.AstralSafety.particles(level,com.example.tutorial.particle.ModParticles.STAR.get(), target.getX(), target.getY(0.6D), target.getZ(),
                        35, 0.55D, 0.65D, 0.55D, 0.2D);
        }
        else data.putInt("tutorial_mod:star_marks", marks);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPerfectParry(LivingHurtEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !player.getMainHandItem().is(ModItems.ICE_SWORD.get())
                || player.level().getGameTime() > player.getPersistentData().getLong("tutorial_mod:parry_until")) return;
        player.getPersistentData().putLong("tutorial_mod:parry_until", 0L);
        event.setCanceled(true);
        AstralStatistics.add(player,"perfect_parries",1);
        if (event.getSource().getEntity() instanceof LivingEntity attacker)
            attacker.knockback(1.4D, player.getX() - attacker.getX(), player.getZ() - attacker.getZ());
        if (player.level() instanceof ServerLevel level)
        {
            com.example.tutorial.event.AstralSafety.particles(level,ParticleTypes.FIREWORK, player.getX(), player.getY(0.65D), player.getZ(),
                    24, 0.45D, 0.65D, 0.45D, 0.16D);
            com.example.tutorial.event.AstralSafety.sound(level, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6F, 1.8F);
        }
    }

    /** 玩家退出时清掉连击状态，避免残留。 */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        ComboTracker.clear(event.getEntity());
    }

    private record PendingFeedback(ServerPlayer player, int combo, int milestone, boolean critical,
                                   DamageSource source)
    {
    }
}

