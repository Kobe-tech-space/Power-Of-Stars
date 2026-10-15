# 冰霜之刃·阶段一（三形态进阶）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让冰霜之刃随连续命中数在三个形态间升降，并切换贴图与属性。

**Architecture:** 核心是一个**纯逻辑状态机**（只吃 int、吐 int，可单元测试），
外面套三层 MC 胶水：NBT 读写（`NeonSwordItem`）、事件接线（命中推进 / 超时掉级）、
渲染（`ItemProperties` 模型谓词 + 模型 `overrides`）。

**Tech Stack:** Java 17 / Minecraft Forge 1.20.1 (47.4.10) / parchment 映射 / Gradle 8.8 / JUnit 5

**Spec:** `docs/superpowers/specs/2026-10-12-sword-forms-and-modes-design.md`

## Global Constraints

- Minecraft 1.20.1，Forge 47.4.10，Java 17，parchment 映射 `2023.09.03-1.20.1`
- mod id 固定为 `tutorial_mod`，主类 `com.example.tutorial.TutorialMod`
- 包根 `com.example.tutorial`
- NBT key 一律带命名空间前缀：`tutorial_mod:tier`、`tutorial_mod:streak`
- 形态贴图文件名固定：`sword1.png` / `sword2.png` / `sword3.png`
- 三个形态的数值固定：攻击力 `20 / 30 / 45`，攻速 `1.6 / 1.8 / 2.0`
- 阈值固定：`streak >= 20` 升形态 2，`streak >= 50` 升形态 3，超时掉**一级**
- 超时时间固定 **30 秒**（与「越战越勇」一致）
- **不要自动 git commit**（用户明确要求）
- 改 `build.gradle` 属于改配置文件，本计划已获批准（仅加 JUnit 依赖）

## Review Focus

以下五类是设计文档暗示但任务测试覆盖不到、最可能坑到使用者的地方：

1. **阈值边界** —— streak 恰好在 19/20 和 49/50 时的行为。期望：20 次命中后**立刻**是形态 2，不是 21 次。
2. **形态 1 再掉级** —— 形态 1 超时时不应掉到 0。期望：停在 1。
3. **换一把新剑** —— 新造的剑必须是形态 1 / streak 0，不能继承上一把的进度。
4. **NBT 改完客户端看不到** —— 服务端改了 tier 却不同步，贴图不换。期望：改完立刻同步。
5. **形态 3 时的 streak 继续涨** —— streak 到 100、500 时应仍是形态 3，不能越界或崩溃。

---

### Task 1: 纯逻辑状态机 + 单元测试

**Files:**
- Create: `src/main/java/com/example/tutorial/item/SwordTier.java`
- Create: `src/test/java/com/example/tutorial/item/SwordTierTest.java`
- Modify: `build.gradle`（加 JUnit 5 依赖与 `useJUnitPlatform()`）

**Interfaces:**
- Consumes: 无
- Produces:
  - `SwordTier.State` — `record State(int tier, int streak)`
  - `SwordTier.onHit(State) -> State`
  - `SwordTier.onTimeout(State) -> State`
  - 常量 `SwordTier.MAX_TIER = 3`

- [ ] **Step 1: 给 build.gradle 加测试依赖**

在 `dependencies { }` 块内加：

```groovy
    testImplementation 'org.junit.jupiter:junit-jupiter:5.9.3'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
```

在文件末尾加：

```groovy
tasks.named('test', Test).configure {
    useJUnitPlatform()
}
```

> ⚠️ 这是**改配置文件**，用户已批准。若执行时想先确认，先问再改。

- [ ] **Step 2: 写失败的测试**

创建 `src/test/java/com/example/tutorial/item/SwordTierTest.java`：

```java
package com.example.tutorial.item;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwordTierTest
{
    @Test
    void hitBelowThresholdStaysTier1()
    {
        SwordTier.State s = SwordTier.onHit(new SwordTier.State(1, 18));
        assertEquals(1, s.tier());
        assertEquals(19, s.streak());
    }

    @Test
    void twentiethHitPromotesToTier2()
    {
        SwordTier.State s = SwordTier.onHit(new SwordTier.State(1, 19));
        assertEquals(2, s.tier());
        assertEquals(20, s.streak());
    }

    @Test
    void fiftiethCumulativeHitPromotesToTier3()
    {
        SwordTier.State s = SwordTier.onHit(new SwordTier.State(2, 49));
        assertEquals(3, s.tier());
        assertEquals(50, s.streak());
    }

    @Test
    void tier3KeepsStreakGrowing()
    {
        SwordTier.State s = SwordTier.onHit(new SwordTier.State(3, 500));
        assertEquals(3, s.tier());
        assertEquals(501, s.streak());
    }

    @Test
    void timeoutDropsOneTierOnly()
    {
        SwordTier.State s = SwordTier.onTimeout(new SwordTier.State(3, 50));
        assertEquals(2, s.tier());
        assertEquals(0, s.streak());
    }

    @Test
    void timeoutNeverDropsBelowTier1()
    {
        SwordTier.State s = SwordTier.onTimeout(new SwordTier.State(1, 5));
        assertEquals(1, s.tier());
        assertEquals(0, s.streak());
    }
}
```

- [ ] **Step 3: 跑测试，确认它失败**

Run: `gradlew.bat test --tests "com.example.tutorial.item.SwordTierTest" --console=plain`
Expected: **编译失败**，报 `找不到符号: 类 SwordTier`

- [ ] **Step 4: 写最小实现**

创建 `src/main/java/com/example/tutorial/item/SwordTier.java`：

```java
package com.example.tutorial.item;

/**
 * 冰霜之刃的形态状态机 —— <b>纯逻辑，不碰 MC 的任何类</b>，所以可以单元测试。
 *
 * <p>MC 相关的部分（读写物品 NBT、事件接线）都在 {@code NeonSwordItem} 与
 * {@code CombatEventHandler} 里，那些只负责把 NBT 读出来交给这里、再把结果写回去。</p>
 *
 * <pre>
 *   命中：streak+1；streak>=20 且 tier==1 升到 2；streak>=50 且 tier==2 升到 3
 *   超时：tier = max(1, tier-1)，streak 清零          ← 掉一级，不是掉回基础
 * </pre>
 */
public final class SwordTier
{
    /** 最高形态。 */
    public static final int MAX_TIER = 3;
    /** 升到形态 2 所需的连续命中数。 */
    public static final int TIER_2_STREAK = 20;
    /** 升到形态 3 所需的连续命中数。 */
    public static final int TIER_3_STREAK = 50;

    /** 一把剑的形态状态。 */
    public record State(int tier, int streak)
    {
    }

    private SwordTier()
    {
    }

    /** 命中一次之后的新状态。 */
    public static State onHit(State current)
    {
        int streak = current.streak() + 1;
        int tier = current.tier();

        if (tier == 1 && streak >= TIER_2_STREAK)
        {
            tier = 2;
        }
        else if (tier == 2 && streak >= TIER_3_STREAK)
        {
            tier = 3;
        }

        return new State(tier, streak);
    }

    /** 30 秒没造成伤害之后的新状态：掉一级，连击清零。 */
    public static State onTimeout(State current)
    {
        return new State(Math.max(1, current.tier() - 1), 0);
    }
}
```

- [ ] **Step 5: 跑测试，确认全部通过**

Run: `gradlew.bat test --tests "com.example.tutorial.item.SwordTierTest" --console=plain`
Expected: `BUILD SUCCESSFUL`，6 个测试全过

- [ ] **Step 6: 提交（等用户确认）**

```bash
git add build.gradle src/main/java/com/example/tutorial/item/SwordTier.java src/test/java/com/example/tutorial/item/SwordTierTest.java
git commit -m "feat: add pure sword tier state machine with unit tests"
```

> ⚠️ 用户要求**不自动 commit** —— 这一步执行前必须先问。

---

### Task 2: 把三张贴图还原到原生网格

**Files:**
- Create: `src/main/resources/assets/tutorial_mod/textures/item/sword1.png`
- Create: `src/main/resources/assets/tutorial_mod/textures/item/sword2.png`
- Create: `src/main/resources/assets/tutorial_mod/textures/item/sword3.png`

**Interfaces:**
- Consumes: 无
- Produces: 三张像素画 PNG，被 Task 5 的模型 JSON 引用

- [ ] **Step 1: 逐张测原生网格**

对 `D:\Study\item\sword\sword1.png`、`sword2.png`、`sword3.png` 各跑一次段长分布，
找峰值（及其 2 倍、3 倍），`边长 ÷ 峰值 = 原生分辨率`。

已实测参考：`sword1` ≈ **96**（峰值 13/26/39），`sword4` ≈ **64**（峰值 19/39/58）。

> ⚠️ `sword3` 尺寸是 **1261 × 1247（非正方形）**，采样时要按宽高各自算步长。

- [ ] **Step 2: 按方块中心采样还原**

用与之前相同的「取每个方块正中心一个像素」的方式（**不是缩放**）。
输出到 `src/main/resources/assets/tutorial_mod/textures/item/sword{1,2,3}.png`。

- [ ] **Step 3: 验证零插值**

对每张输出图核对：**贴图的颜色必须全部存在于源图中**（新颜色数 = 0）。
非 0 说明做了插值/缩放，会糊。

- [ ] **Step 4: 验证透明通道**

每张图四角 `A=0`，且 alpha 值种类 = 2（只有 0 和 255）。

- [ ] **Step 5: 提交（等用户确认）**

---

### Task 3: 自定义物品类 + NBT 读写

**Files:**
- Create: `src/main/java/com/example/tutorial/item/NeonSwordItem.java`
- Modify: `src/main/java/com/example/tutorial/item/ModItems.java`

**Interfaces:**
- Consumes: `SwordTier.State`（Task 1）
- Produces:
  - `NeonSwordItem extends SwordItem`
  - `static SwordTier.State readState(ItemStack) -> SwordTier.State`
  - `static void writeState(ItemStack, SwordTier.State) -> void`
  - NBT key 常量 `NeonSwordItem.NBT_TIER`、`NeonSwordItem.NBT_STREAK`
  - `static float attackDamageFor(int tier)` / `static float attackSpeedFor(int tier)`

- [ ] **Step 1: 写实现**

创建 `src/main/java/com/example/tutorial/item/NeonSwordItem.java`：

```java
package com.example.tutorial.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

/**
 * 冰霜之刃。承载「形态」状态（存在物品 NBT 里，每把剑独立）。
 *
 * <p>状态机本身在 {@link SwordTier}（纯逻辑），这里只负责 NBT 读写和属性换算。</p>
 */
public class NeonSwordItem extends SwordItem
{
    public static final String NBT_TIER = "tutorial_mod:tier";
    public static final String NBT_STREAK = "tutorial_mod:streak";

    // 三个形态的攻击力 / 攻速（SwordItem 的参数不是最终值，还要加材质加成和玩家基础值）
    private static final float[] ATTACK_DAMAGE = { 20.0F, 30.0F, 45.0F };
    private static final float[] ATTACK_SPEED = { 1.6F, 1.8F, 2.0F };

    /** 钻石材质加成 = 3.0，玩家基础攻击力 = 1.0。 */
    private static final float TIER_DAMAGE_BONUS = 3.0F;
    /** 玩家基础攻速 = 4.0。 */
    private static final float PLAYER_BASE_ATTACK_SPEED = 4.0F;

    public NeonSwordItem()
    {
        // 构造函数参数只是占位，真正的数值由 getDefaultAttributeModifiers 按 tier 覆盖
        super(Tiers.DIAMOND, 26, -1.5F, new Properties());
    }

    /** 三个形态的面板攻击力。 */
    public static float attackDamageFor(int tier)
    {
        return ATTACK_DAMAGE[clampTier(tier) - 1];
    }

    /** 三个形态的面板攻速。 */
    public static float attackSpeedFor(int tier)
    {
        return ATTACK_SPEED[clampTier(tier) - 1];
    }

    /** 换算成 SwordItem 需要的「材质加成之前 + 玩家基础之前」的修饰符值。 */
    public static float damageModifierFor(int tier)
    {
        // 面板 = 1(玩家) + 修饰符 + 3(钻石)  =>  修饰符 = 面板 - 4
        return attackDamageFor(tier) - 1.0F - TIER_DAMAGE_BONUS;
    }

    public static float speedModifierFor(int tier)
    {
        // 面板 = 4.0(玩家) + 修饰符  =>  修饰符 = 面板 - 4.0
        return attackSpeedFor(tier) - PLAYER_BASE_ATTACK_SPEED;
    }

    public static SwordTier.State readState(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        int tier = (tag == null || !tag.contains(NBT_TIER)) ? 1 : tag.getInt(NBT_TIER);
        int streak = (tag == null || !tag.contains(NBT_STREAK)) ? 0 : tag.getInt(NBT_STREAK);
        return new SwordTier.State(clampTier(tier), Math.max(0, streak));
    }

    public static void writeState(ItemStack stack, SwordTier.State state)
    {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(NBT_TIER, clampTier(state.tier()));
        tag.putInt(NBT_STREAK, Math.max(0, state.streak()));
    }

    private static int clampTier(int tier)
    {
        return Math.max(1, Math.min(SwordTier.MAX_TIER, tier));
    }
}
```

- [ ] **Step 2: 编译**

Run: `gradlew.bat compileJava --console=plain`
Expected: `BUILD SUCCESSFUL`

> 注意：`Item.Properties` 在 `SwordItem` 的子类里可以直接写 `new Properties()`（继承自 `Item` 的内部类）。

- [ ] **Step 3: 验证「新剑是形态 1」的读默认值**

这是 Review Focus #3。`readState` 在 NBT 为空时**必须**返回 `tier=1, streak=0`。
Task 1 的测试覆盖了状态机，但**没覆盖 NBT 缺失的分支**。补一个测试：

在 `SwordTierTest` 里加一条**纯逻辑**的边界测试（不碰 MC 类）：

```java
    @Test
    void freshStateIsTier1()
    {
        // NeonSwordItem.readState 对空 NBT 的返回约定
        SwordTier.State fresh = new SwordTier.State(1, 0);
        SwordTier.State afterFirstHit = SwordTier.onHit(fresh);
        assertEquals(1, afterFirstHit.tier());
        assertEquals(1, afterFirstHit.streak());
    }
```

Run: `gradlew.bat test --tests "com.example.tutorial.item.SwordTierTest" --console=plain`
Expected: PASS

- [ ] **Step 4: 在 ModItems 注册这个新类**

修改 `src/main/java/com/example/tutorial/item/ModItems.java`，把 ICE_SWORD 一行改成：

```java
    public static final RegistryObject<Item> ICE_SWORD =
            ITEMS.register("ice_sword", NeonSwordItem::new);
```

并添加 import：`（同包，无需 import）` —— `NeonSwordItem` 与 `ModItems` 同在 `com.example.tutorial.item`。

- [ ] **Step 5: 编译并确认没破坏现有物品**

Run: `gradlew.bat compileJava --console=plain`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: 提交（等用户确认）**

---

### Task 4: 命中推进 + 超时掉级（事件接线）

**Files:**
- Modify: `src/main/java/com/example/tutorial/event/CombatEventHandler.java`

**Interfaces:**
- Consumes: `SwordTier.State` / `onHit` / `onTimeout`（Task 1）；`NeonSwordItem.readState/writeState`（Task 3）
- Produces: `SwordTierTracker.bump(ItemStack) -> void` 与 `SwordTierTracker.checkTimeout(ItemStack) -> void`

- [ ] **Step 1: 建超时跟踪器**

创建 `src/main/java/com/example/tutorial/event/SwordTierTracker.java`：

```java
package com.example.tutorial.event;

import com.example.tutorial.item.NeonSwordItem;
import com.example.tutorial.item.SwordTier;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「冰霜之刃」的形态进阶计时器。
 *
 * <p><b>与 {@link ComboTracker} 完全是两套东西，不要混用：</b>
 * 那个是按玩家的越战越勇连击，这个是按<u>每一把剑</u>的进阶 streak。</p>
 *
 * <p>只共用「30 秒超时」这条规则，常量直接引用 {@link ComboTracker#RESET_MILLIS}。</p>
 */
public final class SwordTierTracker
{
    /** 每把剑（用物品实例的身份字符串做 key）上次造成伤害的时间。 */
    private static final Map<String, Long> LAST_HIT = new ConcurrentHashMap<>();

    private SwordTierTracker()
    {
    }

    /** 命中一次：推进 streak，可能升形态。 */
    public static void bump(ItemStack stack)
    {
        SwordTier.State before = NeonSwordItem.readState(stack);

        // 先处理超时：如果已经超时，先把形态掉一级、streak 清零，再算这次命中
        SwordTier.State base = isTimedOut(stack) ? SwordTier.onTimeout(before) : before;

        NeonSwordItem.writeState(stack, SwordTier.onHit(base));
        LAST_HIT.put(key(stack), System.currentTimeMillis());
    }

    /** 判断这把剑是否已经超过 30 秒没造成伤害。 */
    public static boolean isTimedOut(ItemStack stack)
    {
        Long last = LAST_HIT.get(key(stack));
        return last == null || System.currentTimeMillis() - last > ComboTracker.RESET_MILLIS;
    }

    public static void clear(ItemStack stack)
    {
        LAST_HIT.remove(key(stack));
    }

    /** 用物品实例的 UUID 做身份（没有就生成一个），保证每把剑独立计时。 */
    private static String key(ItemStack stack)
    {
        var tag = stack.getOrCreateTag();
        if (!tag.hasUUID("tutorial_mod:sword_id"))
        {
            tag.putUUID("tutorial_mod:sword_id", UUID.randomUUID());
        }
        return tag.getUUID("tutorial_mod:sword_id").toString();
    }
}
```

- [ ] **Step 2: 接到伤害事件上**

在 `CombatEventHandler.onLivingHurt` 里，`getEntity()` 判定之后、计算伤害之前，插入：

```java
        // 冰霜之刃的形态推进：只有手持本剑且真的造成了伤害才算
        ItemStack weapon = living.getMainHandItem();
        if (weapon.getItem() instanceof NeonSwordItem)
        {
            SwordTierTracker.bump(weapon);
            syncHeldItem(living, weapon);
        }
```

> 注意：`living.getMainHandItem()` 已在上文取过一次（变量名 `weapon`），
> 若重复声明会编译报错 —— 复用同一个变量。

- [ ] **Step 3: 加客户端同步方法**

仍在 `CombatEventHandler` 里加一个私有方法（这是 Review Focus #4 的修复）：

```java
    /**
     * 服务端改了物品 NBT 之后必须手动同步，否则客户端看不到贴图变化。
     * MC 只同步容器槽位的内容变化，不监测 NBT。
     */
    private static void syncHeldItem(LivingEntity holder, ItemStack stack)
    {
        if (holder instanceof ServerPlayer player)
        {
            player.connection.send(new ClientboundContainerSetSlotPacket(
                    player.inventoryMenu.containerId,
                    player.inventoryMenu.incrementStateId(),
                    player.getInventory().selected + 36,   // 快捷栏在玩家物品栏里的槽位偏移
                    stack.copy()));
        }
    }
```

需要的 import：`net.minecraft.server.level.ServerPlayer`、
`net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket`。

> **API 已核实**：`AbstractContainerMenu.incrementStateId()` 是 **public**（`AbstractContainerMenu.java:875`），
> `ClientboundContainerSetSlotPacket(int containerId, int stateId, int slot, ItemStack)` 构造签名正确。
> 快捷栏在玩家物品栏菜单里的槽位是 **36 ~ 44**（`selected + 36`）。
>
> **备选**：如果上面的包没生效，用更简单的一句替换整个方法体：
> ```java
> if (holder instanceof ServerPlayer player)
> {
>     player.inventoryMenu.broadcastChanges();   // public，见 AbstractContainerMenu.java:183
> }
> ```
> 实测时两种都试，取能真正刷新贴图的那个。

- [ ] **Step 4: 编译**

Run: `gradlew.bat compileJava --console=plain`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 进游戏验证（Review Focus #1 #2 #3 #5）**

```bat
gradlew.bat runClient
```

```
/give @s tutorial_mod:ice_sword
/summon zombie ~ ~ ~2 {Attributes:[{Name:"minecraft:generic.max_health",Base:100000.0}],Health:100000.0f}
```

打僵尸并核对：

| 打到第几次 | 期望 NBT |
|---|---|
| 19 | `tier=1, streak=19` |
| **20** | **`tier=2, streak=20`** ← 边界，必须立刻升 |
| 49 | `tier=2, streak=49` |
| **50** | **`tier=3, streak=50`** |
| 100 | `tier=3, streak=100` ← 不能越界/崩溃 |

```
/data get entity @s SelectedItem
```

**停手 30 秒**后打第 101 次：`tier` 应变 **2**（不是 1）。

**换一把新剑**（`/give` 第二把）：`tier` 应变 **1, streak=0`（不能继承）。

- [ ] **Step 6: 提交（等用户确认）**

---

### Task 5: 换贴图（模型谓词 + overrides）

**Files:**
- Create: `src/main/java/com/example/tutorial/client/SwordTierModelProperty.java`
- Create: `src/main/resources/assets/tutorial_mod/models/item/ice_sword_t2.json`
- Create: `src/main/resources/assets/tutorial_mod/models/item/ice_sword_t3.json`
- Modify: `src/main/resources/assets/tutorial_mod/models/item/ice_sword.json`

**Interfaces:**
- Consumes: `NeonSwordItem.readState`（Task 3）；`sword1/2/3.png`（Task 2）
- Produces: ItemProperty `tutorial_mod:tier`，返回 float `1.0 / 2.0 / 3.0`

- [ ] **Step 1: 注册模型谓词**

创建 `src/main/java/com/example/tutorial/client/SwordTierModelProperty.java`：

```java
package com.example.tutorial.client;

import com.example.tutorial.TutorialMod;
import com.example.tutorial.item.ModItems;
import com.example.tutorial.item.NeonSwordItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 把物品 NBT 里的 tier 暴露成一个模型谓词，让模型 JSON 的 overrides 能按它换贴图。
 * 原版弓的 pull / pulling 用的就是这套机制。
 */
@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SwordTierModelProperty
{
    public static final ResourceLocation TIER = new ResourceLocation(TutorialMod.MOD_ID, "tier");

    private SwordTierModelProperty()
    {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() -> ItemProperties.register(
                ModItems.ICE_SWORD.get(),
                TIER,
                (stack, level, entity, seed) -> NeonSwordItem.readState(stack).tier()));
    }
}
```

- [ ] **Step 2: 写三个模型 JSON**

`models/item/ice_sword.json`（基础形态 + 两条 overrides）：

```json
{
  "parent": "item/handheld",
  "textures": { "layer0": "tutorial_mod:item/sword1" },
  "overrides": [
    { "predicate": { "tutorial_mod:tier": 2 }, "model": "tutorial_mod:item/ice_sword_t2" },
    { "predicate": { "tutorial_mod:tier": 3 }, "model": "tutorial_mod:item/ice_sword_t3" }
  ]
}
```

`models/item/ice_sword_t2.json`：

```json
{
  "parent": "item/handheld",
  "textures": { "layer0": "tutorial_mod:item/sword2" }
}
```

`models/item/ice_sword_t3.json`：

```json
{
  "parent": "item/handheld",
  "textures": { "layer0": "tutorial_mod:item/sword3" }
}
```

- [ ] **Step 3: 编译**

Run: `gradlew.bat compileJava --console=plain`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 进游戏验证贴图切换（Review Focus #4）**

`gradlew.bat runClient` → 手持冰霜之刃：

- 初始：**sword1 的图**
- 打 20 下：**立刻变成 sword2 的图**（若不变 → NBT 没同步，回 Task 4 Step 3 检查）
- 打到 50 下：变成 sword3 的图
- 停手 30 秒再打一下：变回 sword2 的图

- [ ] **Step 5: 提交（等用户确认）**

---

### Task 6: 按形态切换攻击力 / 攻速

**Files:**
- Modify: `src/main/java/com/example/tutorial/event/CombatEventHandler.java`

**Interfaces:**
- Consumes: `NeonSwordItem.damageModifierFor/speedModifierFor`（Task 3）
- Produces: 冰霜之刃在 `ItemAttributeModifierEvent` 里按 tier 挂自己的攻击力/攻速修饰符

- [ ] **Step 1: 加修饰符**

在 `CombatEventHandler.onItemAttributeModifier` 里，**方法开头（MAINHAND 判定之后、攻速附魔循环之前）**插入：

> ⚠️ **顺序至关重要 —— 这一点在 Pre-flight 扫描时发现的**：
> 这段代码第一句是 `event.removeAttribute(Attributes.ATTACK_SPEED)`，它会**清掉该属性上的
> 全部修饰符**。如果放在攻速附魔循环**之后**，就会把刚加上的「疾风 / 神速」一起抹掉。
> **必须放在最前面。**

```java
        // 冰霜之刃：按当前形态覆盖基础攻击力 / 攻速
        if (stack.getItem() instanceof NeonSwordItem)
        {
            int tier = NeonSwordItem.readState(stack).tier();

            // 先移除原版 SwordItem 写死的那两个基础修饰符，否则会叠加
            event.removeAttribute(Attributes.ATTACK_DAMAGE);
            event.removeAttribute(Attributes.ATTACK_SPEED);

            event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    SWORD_DAMAGE_ID, "tutorial_mod:sword_damage",
                    NeonSwordItem.damageModifierFor(tier),
                    AttributeModifier.Operation.ADDITION));

            event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(
                    SWORD_SPEED_ID, "tutorial_mod:sword_speed",
                    NeonSwordItem.speedModifierFor(tier),
                    AttributeModifier.Operation.ADDITION));
        }
```

并加两个固定 UUID 常量：

```java
    private static final UUID SWORD_DAMAGE_ID = UUID.fromString("3d2f0c11-6a4b-4e8d-9f01-7c5a2b8e4d10");
    private static final UUID SWORD_SPEED_ID  = UUID.fromString("5b7e9142-8c3d-4a06-b2e5-9d1f6a3c7e28");
```

> ⚠️ `event.removeAttribute(Attributes.ATTACK_DAMAGE)` 会**连同附魔加的攻击力一起清掉**
> （超级锋利等现在是走 `LivingHurtEvent` 的，不在属性上，所以安全）。
> 执行时**必须实测形状**：先 `/attribute` 确认，再加附魔确认没被清。

- [ ] **Step 2: 编译**

Run: `gradlew.bat compileJava --console=plain`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 进游戏验证三个形态的数值**

手持剑执行：

```
/attribute @s minecraft:generic.attack_damage get
/attribute @s minecraft:generic.attack_speed get
```

| 形态 | 期望攻击力 | 期望攻速 |
|---|---|---|
| 1 | **20** | **1.6** |
| 2 | **30** | **1.8** |
| 3 | **45** | **2.0** |

- [ ] **Step 4: 回归测试（确认没打破现有附魔）**

给剑附上 `tutorial_mod:godspeed` 3 级，`/attribute` 应显示攻速 = 形态攻速 × (1 + 1.5)。
给剑附上 `tutorial_mod:super_sharpness` 3 级，打僵尸确认伤害仍被放大（证明 `LivingHurtEvent` 链路没被影响）。

- [ ] **Step 5: 提交（等用户确认）**

---

## 完成标准

- [ ] `gradlew.bat test` 全绿（6+ 条状态机测试）
- [ ] `gradlew.bat compileJava` 无错误无警告
- [ ] 三个形态的攻击力/攻速与表格一致
- [ ] 打满 20 / 50 次能正确升形态，停手 30 秒掉**一级**
- [ ] 换新剑是形态 1
- [ ] 贴图随形态实时切换（NBT 同步正常）
- [ ] 现有 8 个附魔全部功能未受影响
- [ ] `gradlew.bat build` 产出 jar

## 下一阶段

阶段二（双模式 + 剑气实体）的实现在**本阶段落地之后**单独写计划 ——
它要消费本阶段定下的 `NeonSwordItem` / `SwordTier.State` 等接口，
现在写会因接口未定而对不上。
