package com.example.tutorial.entity;

import com.example.tutorial.Config;
import com.example.tutorial.damage.ModDamageTypes;
import com.example.tutorial.enchantment.ModEnchantments;
import com.example.tutorial.event.ComboTracker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import com.example.tutorial.event.AstralSafety;
import com.example.tutorial.config.ModSettings;
import java.util.HashSet;
import java.util.UUID;

public class AstralSlashEntity extends ThrowableProjectile
{
    public static final float BASE_DAMAGE = 18.0F;
    /** Hard safety bounds: at most five seconds and roughly ninety-six blocks. */
    public static final int MAX_LIFETIME_TICKS = 100;
    public static final double MAX_RANGE = 96.0D;
    private static float safeDamage(float value) {
        if(Float.isNaN(value) || value<=0) return 0;
        return Math.min(1_000_000_000F,value);
    }

    private static final EntityDataAccessor<Boolean> CRITICAL =
            SynchedEntityData.defineId(AstralSlashEntity.class, EntityDataSerializers.BOOLEAN);

    private Vec3 origin = Vec3.ZERO;
    private float damage = BASE_DAMAGE;
    private boolean momentumSnapshot;
    private int feedbackCombo;
    private int feedbackMilestone;
    private ItemStack weaponSnapshot = ItemStack.EMPTY;
    private int pierceTargets = 1, kills;
    private final HashSet<UUID> hitTargets = new HashSet<>();
    private boolean stormChain;
    private final HashSet<UUID> combatHits = new HashSet<>(), statisticsHits = new HashSet<>();
    private final java.util.Map<UUID, Float> actualDamage = new java.util.HashMap<>();
    public boolean claimCombatHit(UUID target) { return combatHits.add(target); }
    public boolean claimStatisticsHit(UUID target) { return statisticsHits.add(target); }
    public float addActualDamage(LivingEntity target, float amount) {
        return actualDamage.merge(target.getUUID(), Math.min(amount, Math.max(0, target.getHealth())), Float::sum);
    }
    private boolean hurtMixed(LivingEntity target, Entity owner, float total) {
        float half = safeDamage(total) * .5F;
        boolean physical = target.hurt(ModDamageTypes.astralSlash(this, owner), half);
        if (!target.isAlive()) return physical;
        int immunity = target.invulnerableTime;
        // Only bypass the immunity created by our first half, never an unrelated previous hit.
        if (physical) target.invulnerableTime = 0;
        boolean magic;
        try { magic = target.hurt(ModDamageTypes.astralSlashMagic(this, owner), half); }
        finally { if (physical) target.invulnerableTime = Math.max(immunity, target.invulnerableTime); }
        return physical || magic;
    }
    public int lootingLevel() {return weaponSnapshot.getEnchantmentLevel(Enchantments.MOB_LOOTING);}
    @Override public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(3.0D);
    }
    public void recordKill(ServerPlayer p) {if(++kills>=2) com.example.tutorial.event.AstralProgress.award(p,"multi_kill");}

    public AstralSlashEntity(EntityType<? extends AstralSlashEntity> type, Level level)
    {
        super(type, level);
        this.setNoGravity(true);
    }

    public AstralSlashEntity(Level level, LivingEntity owner, float damage,
                             boolean critical, boolean momentumSnapshot)
    {
        this(level, owner, damage, critical, momentumSnapshot, owner.getLookAngle(), (float)ModSettings.d("slash.speed"));
    }

    public AstralSlashEntity(Level level, LivingEntity owner, float damage,
                             boolean critical, boolean momentumSnapshot, Vec3 direction, float speed)
    {
        super(ModEntities.ASTRAL_SLASH.get(), owner, level);
        this.damage = safeDamage(damage);
        this.momentumSnapshot = momentumSnapshot;
        this.entityData.set(CRITICAL, critical);
        this.origin = owner.position();
        weaponSnapshot = owner.getMainHandItem().copy();if(!weaponSnapshot.isEmpty()) weaponSnapshot.setCount(1);
        pierceTargets = 1 + com.example.tutorial.event.AstralEquipment.level(weaponSnapshot) / 2;
        stormChain = ModSettings.enabled("environment.thunderChain") && level.isThundering();
        this.setNoGravity(true);
        Vec3 look = direction.normalize();
        this.setPos(owner.getX() + look.x * 0.9D,
                owner.getEyeY() - 0.25D + look.y * 0.9D,
                owner.getZ() + look.z * 0.9D);
        this.shoot(look.x, look.y, look.z, speed, 0.0F);
    }

    public ItemStack stellarWeaponSnapshot() { return weaponSnapshot.copy(); }

    @Override
    protected void defineSynchedData()
    {
        this.entityData.define(CRITICAL, false);
    }

    public boolean isCritical()
    {
        return this.entityData.get(CRITICAL);
    }

    public int getFeedbackCombo()
    {
        return this.feedbackCombo;
    }

    public int getFeedbackMilestone()
    {
        return this.feedbackMilestone;
    }

    @Override
    public void tick()
    {
        if(!level().isClientSide() && (getOwner()==null || !getOwner().isAlive() || (getOwner() instanceof net.minecraft.world.entity.player.Player p && p.isSpectator())
                || !level().hasChunkAt(blockPosition()))) {discard();return;}
        super.tick();
        if (this.level().isClientSide())
        {
            if(ModSettings.enabled("effects.lowEffects") && tickCount%3!=0) return;
            if(!com.example.tutorial.client.SlashParticleVisibility.visible(this)) return;
            Vec3 velocity = this.getDeltaMovement();
            Vec3 side = velocity.cross(new Vec3(0, 1, 0));
            if (side.lengthSqr() < 0.0001D) side = new Vec3(1, 0, 0);
            side = side.normalize().scale((random.nextDouble() - .5D) * 2.4D);
            this.level().addParticle(com.example.tutorial.particle.ModParticles.STAR.get(),
                    this.getX() - velocity.x * .35D + side.x,
                    this.getY() - velocity.y * .35D + side.y,
                    this.getZ() - velocity.z * .35D + side.z,
                    -velocity.x * .025D, .012D, -velocity.z * .025D);
            if (this.tickCount % 2 == 0)
            {
                this.level().addParticle(com.example.tutorial.particle.ModParticles.STAR.get(),
                        this.getX() - velocity.x * 0.15D,
                        this.getY() - velocity.y * 0.15D,
                        this.getZ() - velocity.z * 0.15D,
                        -velocity.x * 0.035D, -velocity.y * 0.035D, -velocity.z * 0.035D);
            }
        }
        if (!this.level().isClientSide()
                && (this.tickCount >= Config.slashLifetimeTicks
                || this.origin.distanceToSqr(this.position()) >= Config.slashRange * Config.slashRange))
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

        Entity owner = this.getOwner();
        Entity target = result.getEntity();
        if (!(target instanceof LivingEntity livingTarget) || !AstralSafety.canHit(owner,livingTarget) || hitTargets.contains(target.getUUID()))
        {
            return;
        }
        hitTargets.add(target.getUUID());

        float bonus=EnchantmentHelper.getDamageBonus(weaponSnapshot,livingTarget.getMobType());
        float hitDamage = safeDamage(this.damage + (Float.isFinite(bonus)?bonus:0));
        int nextCombo = 0;
        if (this.momentumSnapshot && owner instanceof ServerPlayer player)
        {
            nextCombo = ComboTracker.previewNextHit(player);
            hitDamage *= (float) (1.0D + ModEnchantments.getComboBonus(nextCombo));
        }
        this.feedbackCombo = nextCombo;
        this.feedbackMilestone = nextCombo == 50 || nextCombo == 100 || nextCombo == 150 ? nextCombo : 0;

        boolean hurt = hurtMixed(livingTarget, owner, hitDamage);
        if (hurt)
        {
            if (this.momentumSnapshot && owner instanceof ServerPlayer player)
            {
                ComboTracker.onHit(player);
            }
            this.spawnHitFeedback((LivingEntity) target);
            applyVanillaEnchantments(livingTarget);
            if(stormChain) {
                stormChain=false;
                int chained=0;
                for(LivingEntity nearby:level().getEntitiesOfClass(LivingEntity.class,target.getBoundingBox().inflate(5),e->!hitTargets.contains(e.getUUID()) && e instanceof net.minecraft.world.entity.monster.Enemy && AstralSafety.canHit(owner,e))) {
                    if(chained>=2 || !livingTarget.hasLineOfSight(nearby)) continue;
                    hitTargets.add(nearby.getUUID());chained++;
                    if(hurtMixed(nearby,owner,hitDamage*.25F)) {applyVanillaEnchantments(nearby);spawnHitFeedback(nearby);}
                }
            }
        }
        if(hitTargets.size()>=pierceTargets) this.discard();
    }
    private void applyVanillaEnchantments(LivingEntity target) {
        int fire=weaponSnapshot.getEnchantmentLevel(Enchantments.FIRE_ASPECT), knockback=weaponSnapshot.getEnchantmentLevel(Enchantments.KNOCKBACK);
        if(fire>0) target.setSecondsOnFire(fire*4);
        if(knockback>0) {Vec3 v=getDeltaMovement();target.knockback(knockback*.5,-v.x,-v.z);}
    }
    @Override protected boolean canHitEntity(Entity e) {return super.canHitEntity(e) && e instanceof LivingEntity living && AstralSafety.canHit(getOwner(),living) && !hitTargets.contains(e.getUUID());}

    @Override
    protected void onHitBlock(BlockHitResult result)
    {
        super.onHitBlock(result);
        if (!this.level().isClientSide())
        {
            if (this.level() instanceof ServerLevel serverLevel)
            {
                AstralSafety.particles(serverLevel,com.example.tutorial.particle.ModParticles.STAR.get(),
                        this.getX(), this.getY(), this.getZ(), 10, 0.2D, 0.2D, 0.2D, 0.08D);
                com.example.tutorial.event.AstralSafety.sound(serverLevel, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK,
                        SoundSource.PLAYERS, 0.55F, 1.5F);
            }
            if(ModSettings.enabled("combat.breakFragileBlocks")) {
                var state=level().getBlockState(result.getBlockPos());
                if(state.getBlock() instanceof net.minecraft.world.level.block.GlassBlock || state.getBlock() instanceof net.minecraft.world.level.block.StainedGlassPaneBlock) {
                    if(getOwner() instanceof ServerPlayer p && level().mayInteract(p,result.getBlockPos()) && net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level(),p.gameMode.getGameModeForPlayer(),p,result.getBlockPos())!=-1) level().destroyBlock(result.getBlockPos(),false,this);
                }
            }
            this.discard();
        }
    }

    @Override
    protected float getGravity()
    {
        return 0.0F;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putDouble("OriginX", this.origin.x);
        tag.putDouble("OriginY", this.origin.y);
        tag.putDouble("OriginZ", this.origin.z);
        tag.putFloat("Damage", this.damage);
        tag.putBoolean("Critical", this.isCritical());
        tag.putBoolean("Momentum", this.momentumSnapshot);
        tag.put("WeaponSnapshot",weaponSnapshot.save(new CompoundTag()));tag.putInt("PierceTargets",pierceTargets);tag.putInt("Kills",kills);tag.putBoolean("StormChain",stormChain);
        var hits=new net.minecraft.nbt.ListTag();for(UUID id:hitTargets) hits.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));tag.put("HitTargets",hits);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        this.origin = new Vec3(tag.getDouble("OriginX"), tag.getDouble("OriginY"), tag.getDouble("OriginZ"));
        this.damage = safeDamage(tag.getFloat("Damage"));
        this.entityData.set(CRITICAL, tag.getBoolean("Critical"));
        this.momentumSnapshot = tag.getBoolean("Momentum");
        weaponSnapshot=ItemStack.of(tag.getCompound("WeaponSnapshot"));pierceTargets=Math.max(1,Math.min(3,tag.getInt("PierceTargets")));kills=Math.max(0,tag.getInt("Kills"));stormChain=tag.getBoolean("StormChain");
        hitTargets.clear();var hits=tag.getList("HitTargets",8);for(int i=0;i<Math.min(5,hits.size());i++) try {hitTargets.add(UUID.fromString(hits.getString(i)));} catch(IllegalArgumentException ignored) {}
    }

    private void spawnHitFeedback(LivingEntity target)
    {
        if (!(this.level() instanceof ServerLevel serverLevel))
        {
            return;
        }
        // A lethal hit already emits its larger burst from CombatEventHandler; do not double it.
        if (target.isAlive()) AstralSafety.particles(serverLevel,com.example.tutorial.particle.ModParticles.STAR.get(),
                target.getX(), target.getY(0.55D), target.getZ(), 28,
                0.45D, 0.55D, 0.45D, 0.15D);
        com.example.tutorial.event.AstralSafety.sound(serverLevel, target.blockPosition(), SoundEvents.TRIDENT_HIT,
                SoundSource.PLAYERS, 0.9F, 1.1F);
        com.example.tutorial.event.AstralSafety.sound(serverLevel, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.7F, 1.35F);
    }
}
