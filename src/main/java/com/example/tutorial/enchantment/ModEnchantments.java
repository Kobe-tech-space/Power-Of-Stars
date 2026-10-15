package com.example.tutorial.enchantment;

import com.example.tutorial.TutorialMod;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * 注册本模组的附魔。写法与 {@code item/ModItems} 完全一致。
 *
 * <ul>
 *   <li>超级锋利：伤害 +20%/级，加算区，<strong>蓝色</strong></li>
 *   <li>究极锋利：伤害 +50%/级，加算区，<strong>金色</strong></li>
 *   <li>至尊锋利：伤害 ×2^等级，独立乘区，<strong>逐字静态彩虹</strong></li>
 *   <li>越战越勇：连击 50/100/150 给 +50%/100%/150%，独立乘区，<strong>红色</strong></li>
 *   <li>会心一击：暴击概率 +0.1/级，<strong>亮黄</strong></li>
 *   <li>致命一击：暴击伤害 +0.2/级，<strong>绯红</strong></li>
 *   <li>疾风：攻击速度 +0.2/级（固定值），<strong>亮绿</strong></li>
 *   <li>神速：攻击速度 +50%/级（百分比），<strong>青色</strong></li>
 * </ul>
 */
public class ModEnchantments
{
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, TutorialMod.MOD_ID);

    /** 触发暴击时的基础伤害倍率（爆伤加成之前）。 */
    public static final double BASE_CRIT_MULTIPLIER = 2.0D;

    public static final RegistryObject<Enchantment> STAR_EDGE = stellar("star_edge", net.minecraft.world.entity.EquipmentSlot.MAINHAND);
    public static final RegistryObject<Enchantment> STAR_GATHERING = stellar("star_gathering", net.minecraft.world.entity.EquipmentSlot.MAINHAND);
    public static final RegistryObject<Enchantment> STAR_LEECH = stellar("star_leech", net.minecraft.world.entity.EquipmentSlot.MAINHAND);
    public static final RegistryObject<Enchantment> STAR_WARD = stellar("star_ward", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final RegistryObject<Enchantment> STAR_STRIDE = stellar("star_stride", net.minecraft.world.entity.EquipmentSlot.FEET);
    public static final RegistryObject<Enchantment> STAR_MENDING = stellar("star_mending", net.minecraft.world.entity.EquipmentSlot.CHEST);
    private static RegistryObject<Enchantment> stellar(String id, net.minecraft.world.entity.EquipmentSlot slot) {
        return ENCHANTMENTS.register(id, () -> new StellarEnchantment(slot));
    }

    // ===== 伤害（走 LivingHurtEvent 的最终伤害） =====

    // 超级锋利：每级 +20% 伤害，与其他加算类附魔相加，蓝色
    public static final RegistryObject<Enchantment> SUPER_SHARPNESS =
            ENCHANTMENTS.register("super_sharpness",
                    () -> new DamageEnchantment(Enchantment.Rarity.COMMON,
                            DamageEnchantment.Kind.ADDITIVE_PERCENT, 0.20D, 1,
                            (charIndex, time) -> 0x5555FF));

    // 究极锋利：每级 +50% 伤害，与超级锋利相加，金色
    public static final RegistryObject<Enchantment> ULTIMATE_SHARPNESS =
            ENCHANTMENTS.register("ultimate_sharpness",
                    () -> new DamageEnchantment(Enchantment.Rarity.UNCOMMON,
                            DamageEnchantment.Kind.ADDITIVE_PERCENT, 0.50D, 6,
                            (charIndex, time) -> 0xFFAA00));

    // 至尊锋利：每级翻一倍（2^等级），独立乘算，逐字静态彩虹
    public static final RegistryObject<Enchantment> SUPREME_SHARPNESS =
            ENCHANTMENTS.register("supreme_sharpness",
                    () -> new DamageEnchantment(Enchantment.Rarity.RARE,
                            DamageEnchantment.Kind.FINAL_MULTIPLIER, 0.0D, 12,
                            ModEnchantments::rainbow));

    // 越战越勇：单级，按连击档位放大最终伤害，红色
    public static final RegistryObject<Enchantment> MOMENTUM =
            ENCHANTMENTS.register("momentum",
                    () -> new ComboEnchantment(Enchantment.Rarity.RARE, 10,
                            (charIndex, time) -> 0xFF5555));

    // ===== 暴击（走 LivingHurtEvent，自建掷骰系统） =====

    // 会心一击：每级 +0.1 暴击概率（3 级 = 30%），亮黄
    public static final RegistryObject<Enchantment> CRITICAL_STRIKE =
            ENCHANTMENTS.register("critical_strike",
                    () -> new CritEnchantment(Enchantment.Rarity.UNCOMMON,
                            CritEnchantment.Stat.CRIT_CHANCE, 0.10D, 8,
                            (charIndex, time) -> 0xFFEE44));

    // 致命一击：每级 +0.2 暴击伤害（3 级时暴击倍率 = 2.0 + 0.6 = 2.6），绯红
    public static final RegistryObject<Enchantment> LETHAL_STRIKE =
            ENCHANTMENTS.register("lethal_strike",
                    () -> new CritEnchantment(Enchantment.Rarity.RARE,
                            CritEnchantment.Stat.CRIT_DAMAGE, 0.20D, 14,
                            (charIndex, time) -> 0xDD3355));

    // ===== 攻击速度（走属性修饰符，会显示在物品提示框上） =====

    // 疾风：每级 +0.2 固定攻速（ADDITION），亮绿
    public static final RegistryObject<Enchantment> GALE =
            ENCHANTMENTS.register("gale",
                    () -> new AttackSpeedEnchantment(Enchantment.Rarity.UNCOMMON,
                            0.2D, AttributeModifier.Operation.ADDITION,
                            "tutorial_mod:gale", 6,
                            (charIndex, time) -> 0x55FF55));

    // 神速：每级 +50% 攻速（MULTIPLY_BASE，与其他百分比攻速加算），青色
    public static final RegistryObject<Enchantment> GODSPEED =
            ENCHANTMENTS.register("godspeed",
                    () -> new AttackSpeedEnchantment(Enchantment.Rarity.RARE,
                            0.5D, AttributeModifier.Operation.MULTIPLY_BASE,
                            "tutorial_mod:godspeed", 12,
                            (charIndex, time) -> 0x00D8FF));

    /** 所有「攻速类」附魔，供 CombatEventHandler 遍历。 */
    public static final List<RegistryObject<Enchantment>> ATTACK_SPEED_ENCHANTMENTS = List.of(GALE, GODSPEED);

    /** 所有「伤害类」附魔（越战越勇之外），供聚合。 */
    private static final List<RegistryObject<Enchantment>> DAMAGE_ENCHANTMENTS =
            List.of(SUPER_SHARPNESS, ULTIMATE_SHARPNESS, SUPREME_SHARPNESS);

    /** 两个暴击附魔。 */
    private static final List<RegistryObject<Enchantment>> CRIT_ENCHANTMENTS =
            List.of(CRITICAL_STRIKE, LETHAL_STRIKE);

    public static void register(IEventBus eventBus)
    {
        ENCHANTMENTS.register(eventBus);
    }

    /**
     * 聚合物品上三个伤害附魔的总因子：加算区的百分比先相加，再乘以独立乘区的倍率。
     *
     * <pre>
     *   因子 = (1 + Σ(加算类因子 - 1)) × Π(乘算类因子)
     * </pre>
     *
     * <p>倍率完全由等级算出，所以模组把上限提到 3 级以上时，这里会自动跟着涨。</p>
     */
    public static double getDamageFactor(ItemStack stack)
    {
        return getDamageFactor(stack, 0);
    }

    public static double getDamageFactor(ItemStack stack, double armorBonus)
    {
        double additiveSum = Math.max(0, armorBonus);
        if (stack.getItem() instanceof com.example.tutorial.item.AstralGreatswordItem)
            additiveSum += .08 * Math.max(0, Math.min(3, stack.getEnchantmentLevel(STAR_EDGE.get())));
        double finalMultiplier = 1.0D;

        for (RegistryObject<Enchantment> registryObject : DAMAGE_ENCHANTMENTS)
        {
            Enchantment enchantment = registryObject.get();
            int level = stack.getEnchantmentLevel(enchantment);
            if (level <= 0 || !(enchantment instanceof DamageEnchantment damage))
            {
                continue;
            }
            double factor = damage.getFactor(level);
            if (damage.getKind() == DamageEnchantment.Kind.ADDITIVE_PERCENT)
            {
                additiveSum += factor - 1.0D;
            }
            else
            {
                finalMultiplier *= factor;
            }
        }

        return (1.0D + additiveSum) * finalMultiplier;
    }

    /** 物品上是否带着「越战越勇」。 */
    public static boolean hasCombo(ItemStack stack)
    {
        return stack.getEnchantmentLevel(MOMENTUM.get()) > 0;
    }

    /** 按连击数取「越战越勇」的伤害加成（0.5 = +50%）。 */
    public static double getComboBonus(int combo)
    {
        Enchantment enchantment = MOMENTUM.get();
        return enchantment instanceof ComboEnchantment comboEnchantment
                ? comboEnchantment.getBonusForCombo(combo)
                : 0.0D;
    }

    /** 物品提供的总暴击概率（0.0 ~ 1.0 之外的值由调用方夹取）。 */
    public static double getCritChance(ItemStack stack)
    {
        return sumCrit(stack, CritEnchantment.Stat.CRIT_CHANCE);
    }

    /** 物品提供的总暴击伤害加成（0.6 = 暴击倍率 +0.6）。 */
    public static double getCritDamageBonus(ItemStack stack)
    {
        return sumCrit(stack, CritEnchantment.Stat.CRIT_DAMAGE);
    }

    private static double sumCrit(ItemStack stack, CritEnchantment.Stat stat)
    {
        double total = 0.0D;
        for (RegistryObject<Enchantment> registryObject : CRIT_ENCHANTMENTS)
        {
            Enchantment enchantment = registryObject.get();
            int level = stack.getEnchantmentLevel(enchantment);
            if (level > 0 && enchantment instanceof CritEnchantment crit && crit.getStat() == stat)
            {
                total += crit.getBonus(level);
            }
        }
        return total;
    }

    /**
     * 逐字静态彩虹：色相只由字符位置决定 —— 不流动、不闪烁，颜色是固定的。
     *
     * <p>{@link TutorialEnchantment.NameColor} 的第二个参数是时间，保留给「需要动画」的颜色；
     * 这里刻意不用它。</p>
     */
    private static int rainbow(int charIndex, long timeMillis)
    {
        // 相邻字符错开 25 度，整行拼成一条固定的彩虹
        float hue = ((charIndex * 25.0F) % 360.0F) / 360.0F;
        return Mth.hsvToRgb(hue, 0.85F, 1.0F);
    }
}
