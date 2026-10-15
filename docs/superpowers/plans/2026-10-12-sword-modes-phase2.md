# 冰霜之刃·阶段二（双模式 + 剑气）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让冰霜之刃可以右键切换「近战 / 远程」，远程模式下左键发射一道剑气。

**Architecture:** 一个新实体类型（`SwordWaveEntity`）+ 一个客户端渲染器 + 一个客户端→服务端的网络包 + 三处左键拦截。
模式状态复用阶段一的物品 NBT 模式；剑气的伤害走**同一条** `LivingHurtEvent` 链路，
所以 8 个附魔自动生效，无需特殊代码。

**Tech Stack:** Java 17 / Minecraft Forge 1.20.1 (47.4.10) / parchment / Gradle 8.8 / JUnit 5

**Spec:** `docs/superpowers/specs/2026-10-12-sword-forms-and-modes-design.md`（阶段二章节）

**前置**：阶段一已完成（`docs/superpowers/plans/2026-10-12-sword-forms-phase1.md`）。
本计划消费阶段一定下的接口：`NeonSwordItem`（物品类与其 NBT key）、`SwordTier`（形态与数值）、
`CombatEventHandler.onLivingHurt`（伤害链路）。

## Global Constraints

- Minecraft 1.20.1，Forge 47.4.10，Java 17，mod id `tutorial_mod`，包根 `com.example.tutorial`
- NBT key 一律带命名空间：新增 `tutorial_mod:mode`、`tutorial_mod:last_fire`
- 剑气贴图：`textures/item/sword4.png`，**原生 64×64**（不是缩放，是按方块中心采样）
- 剑气数值：伤害 = 当前形态攻击力 × **60%**；速度约 **30 格/秒**；射程 **24 格**；
  冷却 **0.8 秒**；每次消耗 **1 点耐久**；命中第一个目标后消失
- 剑气的**朝向 = 飞行方向**（用户选定）
- 剑气命中**算作形态进阶的命中**（用户选定，走同一条 `LivingHurtEvent` 自然成立）
- **不要自动 git commit**（用户明确要求）；本仓库**不是 git 仓库**，全程无 commit
- **以下 API 已用 `javap` / 源码核实过**（写计划时逐一验证，不是猜的）：
  - `NetworkRegistry.newSimpleChannel(ResourceLocation, Supplier<String>, Predicate<String>, Predicate<String>)`
    → `net.minecraftforge.network.simple.SimpleChannel`（**注意 `simple` 子包**）
  - `SimpleChannel.registerMessage(int, Class<MSG>, BiConsumer<MSG,FriendlyByteBuf>, Function<FriendlyByteBuf,MSG>, BiConsumer<MSG,Supplier<NetworkEvent.Context>>)`
  - `SimpleChannel.sendToServer(MSG)`
  - `EntityRenderersEvent.RegisterRenderers.registerEntityRenderer(EntityType<? extends T>, EntityRendererProvider<T>)`
  - `Entity` 的三个抽象方法：`defineSynchedData()` / `readAdditionalSaveData(CompoundTag)` / `addAdditionalSaveData(CompoundTag)`
  - `EntityRenderer.render(T, float yaw, float partialTick, PoseStack, MultiBufferSource, int packedLight)`
  - `PlayerInteractEvent` 的 `LeftClickEmpty`（**仅客户端触发**）/ `LeftClickBlock`；`AttackEntityEvent`（服务端、可取消）
  - `ThrowableProjectile` 的 `tick()` 已实现移动与碰撞并回调 `onHitEntity` / `onHitBlock`

## Review Focus

设计文档暗示、但任务测试覆盖不到、最可能坑到使用者的五类情况（每类都已钉到具体任务）：

1. **剑气打到发射者自己** —— 从玩家脚下向前发射时，剑气不应立刻命中自己。期望：忽略发射者与友方。
2. **左键点方块时把挖掘也取消了** —— 远程模式下点方块应只发射剑气，不能连挖掘逻辑一起坏掉。
3. **冷却绕过** —— 冷却期内连续左键不应刷出一堆剑气。期望：0.8 秒内只出一发。
4. **耐久耗尽** —— 耐久为 0 时剑会消失，此时不应还能发射。
5. **剑气不消失/无限飞** —— 没打中任何东西时必须到 24 格自动消散，否则世界里堆满实体。

---

### Task 1: 剑气实体 + 注册

**Files:**
- Create: `src/main/java/com/example/tutorial/entity/SwordWaveEntity.java`
- Create: `src/main/java/com/example/tutorial/entity/ModEntities.java`
- Modify: `src/main/java/com/example/tutorial/TutorialMod.java`

**Interfaces:**
- Consumes: `SwordTier.attackDamageFor(int)`（阶段一）、`NeonSwordItem.readState(ItemStack)`（阶段一）
- Produces:
  - `ModEntities. SWORD_WAVE` — `RegistryObject<EntityType<SwordWaveEntity>>`
  - `ModEntities.register(IEventBus)` — 和 `ModItems.register` 同样的模式
  - `SwordWaveEntity` 的无参构造工厂 `(EntityType<SwordWaveEntity>, Level)`

- [ ] **Step 1: 写实体类**

创建 `src/main/java/com/example/tutorial/entity/SwordWaveEntity.java`：

```java
package com.example.tutorial.entity;

import com.example.tutorial.item.NeonSwordItem;
import com.example.tutorial.item.SwordTier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 剑气：一道沿飞行方向前进的斩击波。
 *
 * <p>伤害走 {@code target.hurt(playerAttack(...), amount)}，所以会经过同一条
 * {@code LivingHurtEvent} —— 8 个附魔自动生效，形态进阶的连击也会被推进。</p>
 */
public class SwordWaveEntity extends ThrowableProjectile
{
    /** 射程：飞出这么远还没打中就消散。 */
    public static final double MAX_RANGE = 24.0D;
    /** 伤害倍率：基础伤害 = 当前形态攻击力 × 这个值。 */
    public static final float DAMAGE_RATIO = 0.60F;

    /** 发射时记录的起点，用于算射程。 */
    private Vec3 origin = Vec3.ZERO;
    /** 发射时的形态攻击力，服务端算伤害用。 */
    private float baseDamage = 1.0F;

    public SwordWaveEntity(EntityType<? extends SwordWaveEntity> type, Level level)
    {
        super(type, level);
    }

    public SwordWaveEntity(EntityType<? extends SwordWaveEntity> type, LivingEntity shooter, Level level)
    {
        super(type, shooter, level);
        this.origin = shooter.position();
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.2D, shooter.getZ());

        ItemStack weapon = shooter.getMainHandItem();
        int tier = (weapon.getItem() instanceof NeonSwordItem)
                ? NeonSwordItem.readState(weapon).tier()
                : 1;
        this.baseDamage = SwordTier.attackDamageFor(tier) * DAMAGE_RATIO;

        // 沿视线方向给出速度：约 30 格/秒 = 1.5 格/刻
        Vec3 look = shooter.getLookAngle();
        this.setDeltaMovement(look.scale(1.5D));
        this.setOwner(shooter);
    }

    @Override
    protected void defineSynchedData()
    {
        // 本实体不需要额外的同步字段：位置与速度由原版自动同步
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putDouble("OriginX", this.origin.x);
        tag.putDouble("OriginY", this.origin.y);
        tag.putDouble("OriginZ", this.origin.z);
        tag.putFloat("BaseDamage", this.baseDamage);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        this.origin = new Vec3(tag.getDouble("OriginX"), tag.getDouble("OriginY"), tag.getDouble("OriginZ"));
        this.baseDamage = tag.getFloat("BaseDamage");
    }

    @Override
    public void tick()
    {
        super.tick();
        // Review Focus #5：没打中任何东西时必须消散，否则世界里堆满实体
        if (!this.level().isClientSide() && this.origin.distanceTo(this.position()) > MAX_RANGE)
        {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result)
    {
        super.onHitEntity(result);
        if (this.level().isClientSide())
        {
            return;
        }

        Entity target = result.getEntity();
        Entity owner = this.getOwner();

        // Review Focus #1：不能打到发射者自己
        if (target == owner || !(target instanceof LivingEntity living))
        {
            return;
        }
        // 也不能打到发射者的队友（同队玩家）
        if (owner instanceof LivingEntity livingOwner && livingOwner.isAlliedTo(target))
        {
            return;
        }

        DamageSource source = (owner instanceof Player player)
                ? this.damageSources().playerAttack(player)
                : this.damageSources().mobAttack(livingOwnerOrNull(owner));

        living.invulnerableTime = 0;   // 避免因为无敌帧而免伤
        living.hurt(source, this.baseDamage);
        this.discard();
    }

    @Override
    protected void onHitBlock(net.minecraft.world.phys.BlockHitResult result)
    {
        super.onHitBlock(result);
        if (!this.level().isClientSide())
        {
            this.discard();
        }
    }

    @Override
    public boolean isPickable()
    {
        return false;   // 不能被玩家点选
    }

    private static LivingEntity livingOwnerOrNull(Entity owner)
    {
        return (owner instanceof LivingEntity living) ? living : null;
    }

    @Override
    public Packet<?> getAddEntityPacket()
    {
        return new ClientboundAddEntityPacket(this);
    }
}
```

- [ ] **Step 2: 注册 EntityType**

创建 `src/main/java/com/example/tutorial/entity/ModEntities.java`：

```java
package com.example.tutorial.entity;

import com.example.tutorial.TutorialMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 模组的实体类型注册。写法与 {@code item/ModItems} 一致。 */
public class ModEntities
{
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TutorialMod.MOD_ID);

    public static final RegistryObject<EntityType<SwordWaveEntity>> SWORD_WAVE =
            ENTITIES.register("sword_wave", () -> EntityType.Builder
                    .<SwordWaveEntity>of(SwordWaveEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("sword_wave"));

    public static void register(IEventBus eventBus)
    {
        ENTITIES.register(eventBus);
    }
}
```

- [ ] **Step 3: 在模组主类里注册**

修改 `TutorialMod.java` 的构造函数，在 `ModEnchantments.register(modEventBus);` 之后加：

```java
        // Register the DeferredRegister of entities to the mod event bus
        ModEntities.register(modEventBus);
```

并加 import：`import com.example.tutorial.entity.ModEntities;`

- [ ] **Step 4: 编译**

Run: `gradlew.bat compileJava --console=plain`
Expected: `BUILD SUCCESSFUL`，我的代码 0 警告

- [ ] **Step 5: 进游戏验证实体存在**

```bat
gradlew.bat runClient
```

进世界后：

```
/summon tutorial_mod:sword_wave ~ ~ ~2
/data get entity @e[type=tutorial_mod:sword_wave,limit=1,sort=nearest]
```

Expected: 命令不报错；`/data` 显示出该实体（此时还看不见，渲染在 Task 2）。
若报 `Unknown entity type` → 说明注册没生效，回 Step 3 检查。

- [ ] **Step 6: 提交（无 git 仓库，跳过，记录台账）**

---

### Task 2: 剑气渲染（朝向飞行方向）

**Files:**
- Create: `src/main/java/com/example/tutorial/client/SwordWaveRenderer.java`
- Create: `src/main/resources/assets/tutorial_mod/textures/item/sword4.png`
- Modify: `src/main/java/com/example/tutorial/TutorialMod.java`（客户端事件类）
- Modify: `src/main/resources/assets/tutorial_mod/lang/zh_cn.json` / `en_us.json`（实体名）

**Interfaces:**
- Consumes: `ModEntities.SWORD_WAVE`（Task 1）
- Produces: `SwordWaveRenderer`，经 `RegisterRenderers` 注册

- [ ] **Step 1: 处理 sword4 贴图到原生 64×64**

源图 `D:\Study\item\sword\sword4.png`（1254×1254），**按方块中心采样**还原到 **64×64**
（峰值实测 19/39/58 ⇒ 块 19.59 ⇒ 1254 ÷ 19.59 = 64）。

> **不是缩放**。用阶段一同样的脚本，然后校验：alpha 值种类 = 2、四角 A = 0、
> 贴图颜色相对源图**新颜色数 = 0**。

输出到 `src/main/resources/assets/tutorial_mod/textures/item/sword4.png`。

- [ ] **Step 2: 写渲染器**

创建 `src/main/java/com/example/tutorial/client/SwordWaveRenderer.java`：

```java
package com.example.tutorial.client;

import com.example.tutorial.TutorialMod;
import com.example.tutorial.entity.SwordWaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 剑气渲染：一个四边形，法线朝向**飞行方向**（用户选定），而不是朝向镜头。
 *
 * <p>做法：从实体的速度向量反解出 yaw/pitch，把四边形转到那个方向。</p>
 */
public class SwordWaveRenderer extends EntityRenderer<SwordWaveEntity>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID, "textures/item/sword4.png");

    /** 四边形在世界里的半边长（格）。 */
    private static final float HALF_SIZE = 0.8F;

    public SwordWaveRenderer(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SwordWaveEntity entity)
    {
        return TEXTURE;
    }

    @Override
    public void render(SwordWaveEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        Vec3 motion = entity.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-6D)
        {
            motion = new Vec3(0.0D, 0.0D, 1.0D);
        }

        // 由速度反解朝向：yaw 绕 Y 轴，pitch 绕 X 轴
        float yaw = (float) (Mth.atan2(motion.x, motion.z) * (180.0F / Math.PI));
        float pitch = (float) (Mth.atan2(motion.y, motion.horizontalDistance()) * (180.0F / Math.PI));

        poseStack.pushPose();

        // 让四边形正面朝向飞行方向
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        // 一个朝向 +Z 的四边形
        vertex(consumer, pose, normal, -HALF_SIZE, -HALF_SIZE, 0.0F, 0.0F, 1.0F, packedLight);
        vertex(consumer, pose, normal, HALF_SIZE, -HALF_SIZE, 0.0F, 1.0F, 1.0F, packedLight);
        vertex(consumer, pose, normal, HALF_SIZE, HALF_SIZE, 0.0F, 1.0F, 0.0F, packedLight);
        vertex(consumer, pose, normal, -HALF_SIZE, HALF_SIZE, 0.0F, 0.0F, 0.0F, packedLight);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void vertex(VertexConsumer c, Matrix4f pose, Matrix3f normal,
                               float x, float y, float z, float u, float v, int light)
    {
        c.vertex(pose, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0.0F, 0.0F, 1.0F)
                .endVertex();
    }
}
```

- [ ] **Step 3: 注册渲染器**

在 `TutorialMod.java` 的 `ClientModEvents` 里，把 `onClientSetup` 旁边**再加**一个方法：

```java
        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event)
        {
            event.registerEntityRenderer(ModEntities.SWORD_WAVE.get(), SwordWaveRenderer::new);
        }
```

imports：`net.minecraftforge.client.event.EntityRenderersEvent`、
`com.example.tutorial.client.SwordWaveRenderer`、`com.example.tutorial.entity.ModEntities`。

- [ ] **Step 4: 加实体名字（提示框与命令反馈会用）**

`lang/zh_cn.json` 加：`"entity.tutorial_mod.sword_wave": "剑气"`
`lang/en_us.json` 加：`"entity.tutorial_mod.sword_wave": "Sword Wave"`

- [ ] **Step 5: 编译**

Run: `gradlew.bat compileJava --console=plain`
Expected: `BUILD SUCCESSFUL`，0 警告

- [ ] **Step 6: 进游戏看剑气**

```bat
gradlew.bat runClient
```

```
/summon tutorial_mod:sword_wave ~ ~ ~2 {Motion:[0.0,0.0,-1.5]}
```

Expected: 看到一个**蓝白紫的新月弧光**，弧面**朝向飞行方向**。

> **朝向调参**：`Axis.YP.rotationDegrees(-yaw)` 与 `Axis.XP.rotationDegrees(pitch)` 这两行的符号
> 可能要按实际观感翻转。若剑气"背面朝前"或侧躺，先试 `-yaw` → `yaw`，再试 `pitch` → `-pitch`。
> 这一步**必须实测**，两次之内能调对。

- [ ] **Step 7: 提交（无 git，跳过）**

---

### Task 3: 模式切换（右键）

**Files:**
- Modify: `src/main/java/com/example/tutorial/item/NeonSwordItem.java`

**Interfaces:**
- Consumes: 无（阶段一的物品类）
- Produces:
  - `NeonSwordItem.NBT_MODE` = `"tutorial_mod:mode"`
  - `NeonSwordItem.MODE_MELEE = 0` / `MODE_RANGED = 1`
  - `static int readMode(ItemStack)` / `static void writeMode(ItemStack, int)`
  - `static boolean isRanged(ItemStack)`

- [ ] **Step 1: 加模式读写**

在 `NeonSwordItem` 里加：

```java
    public static final String NBT_MODE = "tutorial_mod:mode";
    /** 近战模式。 */
    public static final int MODE_MELEE = 0;
    /** 远程（剑气）模式。 */
    public static final int MODE_RANGED = 1;

    /** 当前模式，0 = 近战，1 = 远程。NBT 缺失时是近战。 */
    public static int readMode(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        return (tag == null || !tag.contains(NBT_MODE)) ? MODE_MELEE : tag.getInt(NBT_MODE);
    }

    public static void writeMode(ItemStack stack, int mode)
    {
        stack.getOrCreateTag().putInt(NBT_MODE, mode == MODE_RANGED ? MODE_RANGED : MODE_MELEE);
    }

    public static boolean isRanged(ItemStack stack)
    {
        return readMode(stack) == MODE_RANGED;
    }
```

- [ ] **Step 2: 覆写 use() 切模式**

仍在 `NeonSwordItem` 里加：

```java
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide())
        {
            return InteractionResultHolder.success(stack);
        }

        int next = isRanged(stack) ? MODE_MELEE : MODE_RANGED;
        writeMode(stack, next);

        // 音效反馈：让玩家知道切了模式
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                next == MODE_RANGED ? SoundEvents.ENDER_EYE_LAUNCH : SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS, 1.0F, next == MODE_RANGED ? 1.4F : 0.8F);

        player.displayClientMessage(
                Component.translatable(next == MODE_RANGED
                        ? "message.tutorial_mod.mode.ranged"
                        : "message.tutorial_mod.mode.melee"),
                true);   // true = 显示在快捷栏上方，不刷屏聊天

        return InteractionResultHolder.success(stack);
    }
```

imports：`net.minecraft.world.InteractionHand`、`net.minecraft.world.InteractionResultHolder`、
`net.minecraft.network.chat.Component`、`net.minecraft.sounds.SoundEvents`、`net.minecraft.sounds.SoundSource`、
`net.minecraft.world.entity.player.Player`、`net.minecraft.world.level.Level`。

语言文件各加两行：

- `zh_cn.json`：`"message.tutorial_mod.mode.melee": "已切换：近战"`、
  `"message.tutorial_mod.mode.ranged": "已切换：远程 · 剑气"`
- `en_us.json`：`"message.tutorial_mod.mode.melee": "Mode: Melee"`、
  `"message.tutorial_mod.mode.ranged": "Mode: Ranged (Sword Wave)"`

- [ ] **Step 3: 编译**

Run: `gradlew.bat compileJava --console=plain`
Expected: `BUILD SUCCESSFUL`，0 警告

- [ ] **Step 4: 进游戏验证切换**

`gradlew.bat runClient` → `/give @s tutorial_mod:ice_sword` → **手持右键**

Expected:
- 有音效（远程是高音末影之眼音，近战是低音拾取音）
- 快捷栏上方出现一行提示
- `/data get entity @s SelectedItem` 里 `tutorial_mod:mode` 在 **0 / 1 之间来回切**

- [ ] **Step 5: 提交（无 git，跳过）**

---

### Task 4: 左键发射（网络包 + 三处拦截 + 冷却与耐久）

**Files:**
- Create: `src/main/java/com/example/tutorial/network/ModNetwork.java`
- Create: `src/main/java/com/example/tutorial/network/FireSwordWavePacket.java`
- Create: `src/main/java/com/example/tutorial/event/RangedAttackHandler.java`
- Modify: `src/main/java/com/example/tutorial/TutorialMod.java`（注册网络通道）

**Interfaces:**
- Consumes: `SwordWaveEntity`（Task 1）、`ModEntities.SWORD_WAVE`（Task 1）、`NeonSwordItem.isRanged/NBT_LAST_FIRE`（Task 3）
- Produces:
  - `ModNetwork.CHANNEL` — `net.minecraftforge.network.simple.SimpleChannel`
  - `ModNetwork.register()` — 在模组构造时调用
  - `FireSwordWavePacket` — 无字段的空包（发射意图）

- [ ] **Step 1: 建网络通道**

创建 `src/main/java/com/example/tutorial/network/ModNetwork.java`：

```java
package com.example.tutorial.network;

import com.example.tutorial.TutorialMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 模组的网络通道。
 *
 * <p>注意 {@link SimpleChannel} 在 {@code net.minecraftforge.network.<b>simple</b>} 子包，
 * 不是 {@code net.minecraftforge.network}。</p>
 */
public final class ModNetwork
{
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private ModNetwork()
    {
    }

    /** 在模组构造函数里调用一次。 */
    public static void register()
    {
        int id = 0;
        CHANNEL.registerMessage(id++, FireSwordWavePacket.class,
                FireSwordWavePacket::encode,
                FireSwordWavePacket::decode,
                FireSwordWavePacket::handle);
    }
}
```

- [ ] **Step 2: 建空包**

创建 `src/main/java/com/example/tutorial/network/FireSwordWavePacket.java`：

```java
package com.example.tutorial.network;

import com.example.tutorial.event.RangedAttackHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端→服务端：玩家在远程模式下点了左键（点空的情况服务端看不到，必须自己发）。
 */
public class FireSwordWavePacket
{
    public FireSwordWavePacket()
    {
    }

    public static void encode(FireSwordWavePacket msg, FriendlyByteBuf buf)
    {
        // 空包，无需写入
    }

    public static FireSwordWavePacket decode(FriendlyByteBuf buf)
    {
        return new FireSwordWavePacket();
    }

    public static void handle(FireSwordWavePacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null)
            {
                RangedAttackHandler.tryFire(player);
            }
        });
        context.setPacketHandled(true);
    }
}
```

- [ ] **Step 3: 写发射逻辑与三处拦截**

创建 `src/main/java/com/example/tutorial/event/RangedAttackHandler.java`：

```java
package com.example.tutorial.event;

import com.example.tutorial.TutorialMod;
import com.example.tutorial.entity.ModEntities;
import com.example.tutorial.entity.SwordWaveEntity;
import com.example.tutorial.item.NeonSwordItem;
import com.example.tutorial.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 远程模式的左键发射。
 *
 * <p><b>左键在 MC 里不是单一 hook，要分三处拦：</b></p>
 * <ul>
 *   <li>点<b>空</b> —— {@link PlayerInteractEvent.LeftClickEmpty} <b>只在客户端触发</b>，
 *       所以这里发文一个包到服务端（见 {@code FireSwordWavePacket}）。</li>
 *   <li>点<b>方块</b> —— {@link PlayerInteractEvent.LeftClickBlock}，取消掉挖掘。</li>
 *   <li>点<b>实体</b> —— {@link AttackEntityEvent}（服务端、可取消），取消近战改发射。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RangedAttackHandler
{
    /** 发射冷却（毫秒）。 */
    private static final long COOLDOWN_MILLIS = 800L;
    /** 单次发射消耗的耐久。 */
    private static final int DURABILITY_COST = 1;

    private RangedAttackHandler()
    {
    }

    /** 客户端点空 -> 发包给服务端。 */
    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event)
    {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof NeonSwordItem && NeonSwordItem.isRanged(stack))
        {
            ModNetwork.CHANNEL.sendToServer(new FireSwordWavePacket());
        }
    }

    /** 客户端点方块 -> 取消挖掘，发包。 */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event)
    {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof NeonSwordItem) || !NeonSwordItem.isRanged(stack))
        {
            return;
        }
        event.setCanceled(true);
        if (event.getLevel().isClientSide())
        {
            ModNetwork.CHANNEL.sendToServer(new FireSwordWavePacket());
        }
    }

    /** 服务端点到实体 -> 取消近战，改为发射。 */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event)
    {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof NeonSwordItem) || !NeonSwordItem.isRanged(stack))
        {
            return;
        }
        event.setCanceled(true);
        if (!player.level().isClientSide())
        {
            tryFire(player);
        }
    }

    /**
     * 尝试发射一道剑气。冷却未到、耐久已空、或不是远程模式时静默跳过。
     */
    public static void tryFire(Player player)
    {
        if (player.level().isClientSide() || !(player instanceof ServerPlayer))
        {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof NeonSwordItem) || !NeonSwordItem.isRanged(stack))
        {
            return;
        }

        // Review Focus #3：#冷却绕过
        long now = System.currentTimeMillis();
        if (now - NeonSwordItem.readLastFire(stack) < COOLDOWN_MILLIS)
        {
            return;
        }

        // Review Focus #4：耐久耗尽后不能再发射
        if (stack.getMaxDamage() - stack.getDamageValue() <= DURABILITY_COST)
        {
            return;
        }

        SwordWaveEntity wave = new SwordWaveEntity(ModEntities.SWORD_WAVE.get(), player, player.level());
        player.level().addFreshEntity(wave);

        NeonSwordItem.writeLastFire(stack, now);
        stack.hurtAndBreak(DURABILITY_COST, player,
                p -> p.broadcastBreakEvent(net.minecraft.world.entity.EquipmentSlot.MAINHAND));
    }
}
```

- [ ] **Step 4: 给 NeonSwordItem 加冷却字段的读写**

在 `NeonSwordItem` 里加：

```java
    public static final String NBT_LAST_FIRE = "tutorial_mod:last_fire";

    /** 上次发射时间（毫秒），0 表示从未发射。 */
    public static long readLastFire(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        return (tag == null || !tag.contains(NBT_LAST_FIRE)) ? 0L : tag.getLong(NBT_LAST_FIRE);
    }

    public static void writeLastFire(ItemStack stack, long timeMillis)
    {
        stack.getOrCreateTag().putLong(NBT_LAST_FIRE, timeMillis);
    }
```

- [ ] **Step 5: 在模组构造函数里注册通道**

在 `TutorialMod.java` 构造函数里 `ModEntities.register(modEventBus);` 之后加：

```java
        // 注册网络通道（客户端→服务端的发射包）
        ModNetwork.register();
```

import：`import com.example.tutorial.network.ModNetwork;`

- [ ] **Step 6: 编译**

Run: `gradlew.bat compileJava --console=plain`
Expected: `BUILD SUCCESSFUL`，0 警告

- [ ] **Step 7: 进游戏验证全部五条 Review Focus**

```bat
gradlew.bat runClient
```

```
/give @s tutorial_mod:ice_sword
/summon zombie ~ ~ ~6
```

| 检查 | 操作 | 期望 |
|---|---|---|
| 基本发射 | 右键切到远程，左键点空 | 射出剑气，命中僵尸造成伤害 |
| **RF#1 打到自己** | 正对着一堵墙贴着发射 | 剑气**不会**立刻消失/伤到自己 |
| **RF#2 点方块** | 对着方块左键 | 发射剑气，**且方块不被挖掉**；切回近战后方块恢复正常可挖 |
| **RF#3 冷却** | 冷却期内狂点左键 | 只出 1 道剑气 |
| **RF#4 耐久** | 用 `/give` 一把快坏的剑，耗到剩 1 点 | 不能再发射 |
| **RF#5 射程** | 朝天空发射 | 飞出约 24 格后自动消散，不会无限飞 |
| 附魔联动 | 附上 `super_sharpness` 3 级后再发射 | 剑气伤害明显变高（走的是同一条 LivingHurtEvent） |
| 进阶联动 | 远程命中 20 次 | 形态升到 2（用户选定：剑气也算命中） |

- [ ] **Step 8: 提交（无 git，跳过）**

---

## 完成标准

- [ ] `gradlew.bat compileJava test` 全绿，我的代码 0 警告
- [ ] `/summon tutorial_mod:sword_wave` 能生成实体
- [ ] 剑气看得见、**朝向飞行方向**、约 24 格后消散
- [ ] 右键能在近战/远程之间切换，有音效与快捷栏提示
- [ ] 远程左键能发射，冷却 0.8 秒、每次 1 耐久、耐久耗尽后不能发射
- [ ] 点方块发射时**不会**挖掉方块
- [ ] 剑气吃附魔，且推进形态进阶
- [ ] `gradlew.bat build` 产出 jar

## 已知风险

1. **渲染朝向**（Task 2 Step 6）需要实测调符号 —— 计划给的是推导公式，两次之内能调对。
2. **左键拦截三处事件**是本阶段最脆的部分：`LeftClickEmpty` 只在客户端触发，
   所以必须有网络包；若发包失败（通道没注册），点空将毫无反应。
3. 剑气走 `playerAttack` 伤害源，会**继承玩家主手物品**参与 `LivingHurtEvent` ——
   这正是「吃附魔」的原因，但也意味着**换到别的武器时剑气伤害会变**。
