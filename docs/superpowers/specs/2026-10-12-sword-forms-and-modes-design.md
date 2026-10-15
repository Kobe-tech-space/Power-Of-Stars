# 冰霜之刃改版：三形态进阶 + 双模式（近战/剑气）

## Context（背景与目标）

现有的「冰霜之刃」是一个普通的 `SwordItem` + 一张平面贴图。本次要把它改造成一把
**有形态成长、有双战斗模式**的武器：

- **三形态**：随连续命中数在 基础 → 进阶 → 高阶 之间升降，换贴图
- **双模式**：右键切换「近战 / 远程」，左键按当前模式发动
- **剑气**：远程模式发射一个投射物

用户手上有四张成品像素画（`D:\Study\item\sword\sword1~4.png`），均已验证为
**真像素画 + 真透明通道**，直接可用。

### 已与用户确认的需求

| 项 | 决定 |
|---|---|
| 模式切换 | **右键切换，左键攻击** |
| 形态计数 | **连续命中**（和「越战越勇」同一套规则）|
| 连击断开条件 | **30 秒没造成伤害**（与越战越勇一致）|
| 形态掉落 | **掉一级**（形态3→2→1），不是直接掉回基础 |
| 剑气与附魔 | **全部生效**（锋利 / 暴击 / 越战越勇的加成剑气都吃）|

### 数值（用户已确认）

**三种形态**

| 形态 | tier | 贴图 | 攻击力 | 攻速 |
|---|---|---|---|---|
| 基础 | 1 | `sword1` | 20 | 1.6 |
| 进阶 | 2 | `sword2` | 30 | 1.8 |
| 高阶 | 3 | `sword3` | 45 | 2.0 |

**剑气（`sword4`）**

| 项 | 值 |
|---|---|
| 伤害 | 当前形态攻击力 × 60% |
| 飞行速度 | 约 30 格/秒 |
| 射程 | 24 格（超过则消散）|
| 冷却 | 0.8 秒 |
| 耐久消耗 | 每次 1 点 |
| 穿透 | 命中第一个目标后消失 |

> ⚠️ **平衡提醒**：攻击力 20 已远超原版（下界合金剑 = 8），
> 且剑气全额吃附魔。形态3 + 满附魔的理论伤害可达上万。
> 用户已知悉并选择接受。若要调整，只需改上表。

---

## 拆分：两个阶段

这是三个子系统的组合，**难度差异很大**，必须分阶段做，否则出问题无法定位。

| 阶段 | 内容 | 难度 | 依赖 |
|---|---|---|---|
| **一** | 三形态（NBT 状态 + 换贴图 + 数值切换）| 低 | 无 |
| **二** | 双模式 + 剑气实体（新实体类型 + 渲染 + 左键拦截）| **高** | 阶段一 |

分阶段交付：阶段一做完就是一把「能成长」的剑，已经独立可用。

---

## 阶段一：三形态

### NBT 结构

存在**物品自身**的 NBT 里（每把剑独立，跟着物品走）：

| Key | 类型 | 含义 |
|---|---|---|
| `tutorial_mod:tier` | int (1~3) | 当前形态 |
| `tutorial_mod:streak` | int | 当前连续命中数 |

### 状态机

```
命中时（造成伤害且攻击者手持本剑）：
    streak++
    若 streak >= 20 且 tier == 1  ->  tier = 2
    若 streak >= 50 且 tier == 2  ->  tier = 3

30 秒没造成伤害：
    tier   = max(1, tier - 1)      // 掉一级
    streak = 0
```

> **注意：这里的 streak 与「越战越勇」的连击是两套独立的东西，不要混用。**
>
> | | 「越战越勇」连击 | 本剑的 streak |
> |---|---|---|
> | 存在哪 | 内存，**按玩家**（`ComboTracker`）| 物品 NBT，**按物品** |
> | 作用 | 放大最终伤害 | 决定形态 |
> | 共用点 | **只有「30 秒超时」这条规则** | 同左 |
>
> 之所以不能复用 `ComboTracker`：形态是**每把剑独立**的（NBT 在物品上），
> 而玩家连击是全局的。混用会导致「换一把没养过的剑也能直接是形态3」。
>
> 实现时把「30 秒超时」抽成一个共用的小工具，两处都调它，但**计数器各自独立**。

### 贴图与模型切换

**1. 贴图**：四张图各自还原到原生网格后放进 `textures/item/`，
命名 `sword1.png` / `sword2.png` / `sword3.png` / `sword4.png`。

原生网格**每张图都不一样**，必须逐张实测（方法见下方「贴图处理流程」）。

**2. 模型谓词**：注册一个自定义 ItemProperty：

```java
ItemProperties.register(ModItems.ICE_SWORD.get(),
        new ResourceLocation(TutorialMod.MOD_ID, "tier"),
        (stack, level, entity, seed) -> getTier(stack));   // 返回 1.0 / 2.0 / 3.0
```

**3. 模型 JSON**（`models/item/ice_sword.json`）：

```json
{
  "parent": "item/handheld",
  "textures": { "layer0": "tutorial_mod:item/sword1" },
  "overrides": [
    { "predicate": { "tutorial_mod:tier": 2.0 }, "model": "tutorial_mod:item/ice_sword_t2" },
    { "predicate": { "tutorial_mod:tier": 3.0 }, "model": "tutorial_mod:item/ice_sword_t3" }
  ]
}
```

MC 选**阈值 ≤ 当前值中最大的那个**，所以谓词值用 1.0/2.0/3.0 配 2.0/3.0 阈值即可。
`ice_sword_t2.json` / `ice_sword_t3.json` 各是一个继承 `item/handheld` 的简单模型，
只改 `layer0`。

### 数值切换

形态决定攻击力/攻速 → 在 `ItemAttributeModifierEvent` 里按 tier 挂不同的修饰符
（和现有的攻速附魔同一处，复用 `CombatEventHandler`）。

### 文件清单（阶段一）

| 文件 | 动作 |
|---|---|
| `item/ModItems.java` | 冰霜之刃从 `SwordItem` 改为自定义 `NeonSwordItem` |
| `item/NeonSwordItem.java` | 🆕 承载 tier/streak 读写、属性按 tier 变化 |
| `enchantment/…` 或新包 | 🆕 tier 状态机（命中推进 + 超时掉级）|
| `event/CombatEventHandler.java` | ✏️ 命中时推进 streak；按 tier 挂攻击力/攻速 |
| `event/SwordTierModelProperty.java` | 🆕 注册模型谓词（客户端）|
| `models/item/ice_sword.json` + `_t2` / `_t3` | ✏️/🆕 |
| `textures/item/sword1~4.png` | 🆕 处理后的贴图 |

### ⚠️ 已知风险：NBT 改动需要手动同步给客户端

在服务端改 `ItemStack` 的 NBT **不会自动同步**给客户端（MC 只同步容器槽位内容，
不监测 NBT 变化）。不同步的话，玩家看不到贴图变化。

**修法**（实现时验证）：改完 NBT 后调用
`((ServerPlayer) player).connection.send(new ClientboundContainerSetSlotPacket(...))`
或 `player.inventoryMenu.broadcastChanges()`。

---

## 阶段二：双模式 + 剑气

### 模式切换（右键）

`NeonSwordItem` 覆写 `use()`，切换 NBT 里的 `tutorial_mod:mode`（0 = 近战，1 = 远程），
返回 `InteractionResultHolder.success(stack)`（播放挥手动作），并播放一个音效作为反馈。

### 远程发射（左键）

**这是本阶段最麻烦的部分** —— 左键在 MC 里不是单一的 hook，要三处一起拦：

| 情况 | 事件 | 处理 |
|---|---|---|
| 左键点空 | `PlayerInteractEvent.LeftClickEmpty`（客户端）| 发自定义包到服务端 |
| 左键点方块 | `PlayerInteractEvent.LeftClickBlock` | 取消原版挖掘，发射 |
| 左键点实体 | `AttackEntityEvent`（服务端，可取消）| 取消近战，改为发射 |

**仅当 `mode == 远程` 时拦截**；近战模式下一切照旧。

### 剑气实体

- 基类：`ThrowableProjectile`（参考 `LlamaSpit` 的直线飞行写法）
- 注册：`DeferredRegister<EntityType<?>>`
- 渲染：一个朝向视线的**公告板四边形**，贴 `sword4`
  - 注册在 `EntityRenderersEvent.RegisterRenderers`（客户端事件）
- 行为：直线飞行、命中首个目标造成伤害后消失、超过 24 格消散
- 伤害来源：`damageSources().indirectMagic(this, owner)` 或自定义 DamageSource

### 与现有附魔的交互

用户选择「全部生效」，实现方式：**剑气命中时走同一条 `LivingHurtEvent` 链路**，
所以锋利 / 暴击 / 越战越勇会自动生效，无需特殊处理。

> 但这意味着「越战越勇」的连击**会被远程攻击推进**。这是用户选择的后果，符合预期。

### 文件清单（阶段二）

| 文件 | 动作 |
|---|---|
| `item/NeonSwordItem.java` | ✏️ 加 `use()` 切模式 |
| `entity/SwordWaveEntity.java` | 🆕 剑气实体 |
| `entity/ModEntities.java` | 🆕 EntityType 注册 |
| `client/SwordWaveRenderer.java` | 🆕 客户端渲染 |
| `event/RangedAttackHandler.java` | 🆕 三处左键拦截 + 发包 |
| `network/…` | 🆕 客户端→服务端的发射包 |
| `textures/item/sword4.png` | 🆕 剑气贴图 |

---

## 贴图处理流程（四张图都要做）

每张图的**原生网格大小不同**，必须逐张实测：

1. 读段长分布（连续同色段的长度直方图）
2. 找**峰值**（以及它的 2 倍、3 倍），那就是一个方块占多少像素
3. `图片边长 ÷ 峰值 = 原生分辨率`
4. 按**方块中心采样**还原到该分辨率（不是缩放！）
5. 验证：贴图的颜色应**全部存在于源图中**（0 个新颜色 = 零插值）

已实测：

| 图 | 原生网格 |
|---|---|
| `sword1` | ≈ 96×96（块 13.06，峰值 13/26/39）|
| `sword4` | ≈ 64×64（块 19.6，峰值 19/39/58）|
| `sword2` | **待测** |
| `sword3` | **待测**（尺寸是 1261×1247，非正方形，需注意）|

---

## 验证方案

**阶段一**

1. `gradlew compileJava` → BUILD SUCCESSFUL
2. `gradlew runClient`，`/give @s tutorial_mod:ice_sword`
3. 提示框显示「攻击力 20 / 攻速 1.6」
4. 打怪 20 次 → **贴图应变成 sword2**，攻击力变 30
5. 再打 30 次（累计 50）→ 贴图变 sword3，攻击力 45
6. **停手 30 秒** → 掉回形态 2（不是形态 1）
7. **再停 30 秒** → 掉回形态 1
8. 用 `/data get entity @s SelectedItem` 确认 NBT 里的 tier / streak

**阶段二**

1. 右键 → 模式切换（应有音效反馈）
2. 远程模式左键 → 发射剑气
3. 剑气飞行、命中造成伤害、超过 24 格消散
4. 冷却 0.8 秒内无法连发
5. 附上「超级锋利」→ 确认剑气伤害被放大（证明附魔链路生效）
6. 「越战越勇」的连击被远程攻击推进（预期行为）

---

## 已知的坑与遗留

1. **NBT 同步**（见上）—— 阶段一的主要风险
2. **左键拦截**是阶段二最麻烦的部分，三处事件都要处理
3. **四张图的原生网格不同**，用错尺寸会糊成一团噪点
4. **数值严重超模** —— 用户已知悉
5. 本仓库**不是 git 仓库**，设计文档无法提交版本控制
