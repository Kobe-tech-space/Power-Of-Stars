package com.example.tutorial.enchantment;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 「提升攻击速度」的附魔，数值是等级的纯函数（= 每级数值 × 等级）。
 *
 * <p>数值怎么起作用由 {@link AttributeModifier.Operation} 决定（原版属性合成顺序）：</p>
 * <pre>
 *   d0 = 基础值 + Σ ADDITION
 *   d1 = d0 + d0 × Σ MULTIPLY_BASE
 *   d1 ×= Π (1 + MULTIPLY_TOTAL)
 * </pre>
 *
 * <p>所以「固定值攻速」用 ADDITION，「百分比攻速」用 MULTIPLY_BASE
 * —— 后者会让所有百分比攻速在同一个乘算桶里先【相加】再统一结算一次。</p>
 */
public class AttackSpeedEnchantment extends TutorialEnchantment
{
    // 每级增加的数值（ADDITION 时是固定值，MULTIPLY_BASE 时是百分比）
    private final double perLevel;
    // 该数值以哪种运算作用到攻击速度上
    private final AttributeModifier.Operation operation;
    // 修饰符的固定标识：由名字推导，保证同一个附魔每次都是同一个 UUID
    private final String modifierName;
    private final UUID modifierId;

    public AttackSpeedEnchantment(Rarity rarity, double perLevel, AttributeModifier.Operation operation,
                                  String modifierName, int baseCost, NameColor nameColor)
    {
        super(rarity, baseCost, nameColor);
        this.perLevel = perLevel;
        this.operation = operation;
        this.modifierName = modifierName;
        // UUID 必须稳定，否则每次重建装备属性都会生成一个新修饰符，无法被正确替换
        this.modifierId = UUID.nameUUIDFromBytes(modifierName.getBytes(StandardCharsets.UTF_8));
    }

    /** 该等级对应的数值 = 每级数值 × 等级。等级不设上限。 */
    public double getAmount(int level)
    {
        return this.perLevel * Math.max(level, 0);
    }

    public AttributeModifier.Operation getOperation()
    {
        return this.operation;
    }

    public String getModifierName()
    {
        return this.modifierName;
    }

    public UUID getModifierId()
    {
        return this.modifierId;
    }

    @Override
    public int getMaxLevel()
    {
        return 3;
    }
}
