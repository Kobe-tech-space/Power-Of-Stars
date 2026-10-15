package com.example.tutorial.event;
import com.example.tutorial.TutorialMod;
import com.example.tutorial.entity.*;
import com.example.tutorial.item.ModItems;
import com.example.tutorial.recipe.AstralRecyclingRecipe;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
@GameTestHolder(TutorialMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FeaturesDedicatedGameTest {
    private static void ornaments(net.minecraft.server.level.ServerPlayer p,com.example.tutorial.ornament.OrnamentKind... kinds){
        if(net.minecraftforge.fml.ModList.get().isLoaded("curios"))OrnamentTestGear.equip(p,kinds);
        else p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,kinds.length==0?ItemStack.EMPTY:new ItemStack(com.example.tutorial.ornament.OrnamentItems.get(kinds[0])));
    }
    @GameTest(template="empty",templateNamespace="forge",timeoutTicks=60)
    public static void stellarOrnaments(GameTestHelper helper){
        var level=helper.getLevel();
        var p=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"OrnamentTest"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        p.setPos(net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(new net.minecraft.core.BlockPos(1,1,1))));
        helper.assertTrue(com.example.tutorial.ornament.OrnamentKind.values().length==10,"All ten ornaments registered");
        for(var kind:com.example.tutorial.ornament.OrnamentKind.values()){
            helper.assertTrue(level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID,kind.id)).isPresent(),"Recipe loaded: "+kind.id);
            helper.assertTrue(com.example.tutorial.ornament.OrnamentItems.get(kind).getDefaultInstance().getMaxStackSize()==1,"Unique non-stackable ornament");
        }
        ornaments(p,com.example.tutorial.ornament.OrnamentKind.DAWN_CROWN);
        com.example.tutorial.ornament.OrnamentEffects.update(p);com.example.tutorial.ornament.OrnamentEffects.update(p);
        helper.assertTrue(p.getMaxHealth()==22,"Crown adds exactly one heart without stacking");
        if(net.minecraftforge.fml.ModList.get().isLoaded("curios")){
            helper.assertTrue(!OrnamentTestGear.canEquip(p,com.example.tutorial.ornament.OrnamentKind.DAWN_CROWN,1),"Normal Curios equip rejects duplicate ornaments");
            helper.assertTrue(OrnamentTestGear.canEquip(p,com.example.tutorial.ornament.OrnamentKind.ORBIT_RING,1),"Different ornament can use a free slot");
            helper.assertTrue(!OrnamentTestGear.canEquip(p,com.example.tutorial.ornament.OrnamentKind.ORBIT_RING,3),"Extra slots cannot bypass the three-slot limit");
            ornaments(p,com.example.tutorial.ornament.OrnamentKind.DAWN_CROWN,com.example.tutorial.ornament.OrnamentKind.DAWN_CROWN);
            com.example.tutorial.ornament.OrnamentEffects.update(p);helper.assertTrue(p.getMaxHealth()==22,"Forced duplicate Curios stacks do not multiply effects");
        }
        ornaments(p);com.example.tutorial.ornament.OrnamentEffects.update(p);helper.assertTrue(p.getMaxHealth()==20,"Removing crown withdraws health");
        p.getInventory().add(new ItemStack(com.example.tutorial.ornament.OrnamentItems.get(com.example.tutorial.ornament.OrnamentKind.DAWN_CROWN)));
        com.example.tutorial.ornament.OrnamentEffects.update(p);helper.assertTrue(p.getMaxHealth()==20,"Inventory ornaments inactive");
        ornaments(p,com.example.tutorial.ornament.OrnamentKind.STARFALL_AMULET);
        int bonus=0;for(int i=0;i<10;i++)bonus+=com.example.tutorial.ornament.OrnamentEffects.extraEnergy(p,1);
        helper.assertTrue(bonus==1,"Amulet adds exactly 10%, fractional gains do not round into overpowered doubling");
        helper.assertTrue(StarEnergy.maximum(p)==0,"Amulet cannot bypass full-set energy requirement");
        ornaments(p,com.example.tutorial.ornament.OrnamentKind.ORBIT_RING);
        helper.assertTrue(Math.abs(com.example.tutorial.ornament.OrnamentEffects.damageBonus(p)-.05)<.0001,"Ring bonus capped at 5%");
        ornaments(p,com.example.tutorial.ornament.OrnamentKind.WING_BROOCH);
        var fall=new net.minecraftforge.event.entity.living.LivingFallEvent(p,10,1);
        com.example.tutorial.ornament.OrnamentEffects.fall(fall);helper.assertTrue(Math.abs(fall.getDamageMultiplier()-.8)<.0001,"Brooch reduces fall multiplier only");
        ornaments(p,com.example.tutorial.ornament.OrnamentKind.CONSTELLATION_BRACELET);com.example.tutorial.ornament.OrnamentEffects.update(p);
        helper.assertTrue(Math.abs(p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED)-4.2)<.0001,"Bracelet modest additive attack speed");
        ornaments(p,com.example.tutorial.ornament.OrnamentKind.ASTRAL_BADGE);com.example.tutorial.ornament.OrnamentEffects.update(p);
        helper.assertTrue(Math.abs(p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)-.05)<.0001,"Badge modest knockback resistance");
        helper.assertTrue(p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED)==4,"Switching ornaments removes previous bonus");
        ornaments(p,com.example.tutorial.ornament.OrnamentKind.CRESCENT_EARRING);p.tickCount=100;
        com.example.tutorial.ornament.OrnamentEffects.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
        helper.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION),"Earring grants night vision");
        ornaments(p,com.example.tutorial.ornament.OrnamentKind.TWILIGHT_TALISMAN);
        p.setHealth(10);p.getFoodData().setFoodLevel(20);p.tickCount=200;
        p.getPersistentData().putLong("tutorial_mod:ornament_last_combat",p.server.overworld().getGameTime()-100);
        com.example.tutorial.ornament.OrnamentEffects.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
        helper.assertTrue(p.getHealth()==11,"Talisman heals exactly 1 HP after combat");
        com.example.tutorial.ornament.OrnamentEffects.combat(new net.minecraftforge.event.entity.living.LivingDamageEvent(p,p.damageSources().generic(),1));
        com.example.tutorial.ornament.OrnamentEffects.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
        helper.assertTrue(p.getHealth()==11,"Talisman cannot heal during recent combat");
        ornaments(p,com.example.tutorial.ornament.OrnamentKind.METEOR_PENDANT);
        var repair=new ItemStack(net.minecraft.world.item.Items.IRON_SWORD);repair.setDamageValue(20);p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,repair);
        var repairEvent=new net.minecraftforge.event.entity.living.LivingDeathEvent(net.minecraft.world.entity.EntityType.ZOMBIE.create(level),p.damageSources().playerAttack(p));
        com.example.tutorial.ornament.OrnamentEffects.kill(repairEvent);
        int durability=repair.getDamageValue();com.example.tutorial.ornament.OrnamentEffects.kill(repairEvent);
        helper.assertTrue((durability==18||durability==20)&&repair.getDamageValue()==durability,"Meteor repair bounded to 2 durability and once per window");
        com.mojang.logging.LogUtils.getLogger().info("STELLAR_ORNAMENTS_PASS: ten recipes, equip rules, optional Curios={}, no duplicates, attribute removal, bounded energy, fall, speed, ward, night vision",net.minecraftforge.fml.ModList.get().isLoaded("curios"));
        helper.succeed();
    }
    @GameTest(template="empty",templateNamespace="forge",timeoutTicks=60)
    public static void movementWithoutCooldownAndShieldFeedback(GameTestHelper helper) {
        var level=helper.getLevel();
        var p=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"StarMoveTest"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        var origin=helper.absolutePos(new net.minecraft.core.BlockPos(1,20,1));
        for(int z=0;z<=40;z+=8)level.getChunk(origin.getX()>>4,(origin.getZ()+z)>>4);
        for(var pos:net.minecraft.core.BlockPos.betweenClosed(origin.offset(-1,0,-1),origin.offset(1,3,40)))
            level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        p.setPos(net.minecraft.world.phys.Vec3.atCenterOf(origin));
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new ItemStack(ModItems.COSMIC_HELMET.get()));
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,new ItemStack(ModItems.COSMIC_CHESTPLATE.get()));
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS,new ItemStack(ModItems.COSMIC_LEGGINGS.get()));
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,new ItemStack(ModItems.COSMIC_BOOTS.get()));
        StarEnergy.set(p,1000);p.setYRot(0);p.setXRot(60);
        p.getPersistentData().putLong("tutorial_mod:exploration_1",Long.MAX_VALUE);
        p.getPersistentData().putLong("tutorial_mod:exploration_2",Long.MAX_VALUE);
        var start=p.position();StarExploration.activate(p,1);
        helper.assertTrue(p.position().distanceTo(start)>3&&p.getY()==start.y&&StarEnergy.get(p)==980,"R ignores legacy cooldown and moves horizontally when looking down: start="+start+", end="+p.position()+", energy="+StarEnergy.get(p)+", set="+com.example.tutorial.item.StarArmorItem.fullSetTier(p));
        var first=p.position();StarExploration.activate(p,1);
        helper.assertTrue(p.position().equals(first)&&StarEnergy.get(p)==980,"Duplicate same-tick movement blocked without resource loss");
        p.getPersistentData().putLong("tutorial_mod:last_star_move_tick",level.getGameTime()-1);StarExploration.activate(p,1);
        helper.assertTrue(p.position().distanceTo(first)>3&&StarEnergy.get(p)==960,"R works again next tick, no skill cooldown");
        p.setXRot(0);p.getPersistentData().putLong("tutorial_mod:last_star_move_tick",level.getGameTime()-1);
        var before=p.position();StarExploration.activate(p,2);
        helper.assertTrue(p.position().distanceTo(before)>7&&StarEnergy.get(p)==920,"H ignores legacy cooldown");
        p.getPersistentData().putLong("tutorial_mod:last_star_move_tick",level.getGameTime()-1);before=p.position();StarExploration.activate(p,2);
        helper.assertTrue(p.position().distanceTo(before)>7&&StarEnergy.get(p)==880,"H works again next tick");
        var wall=p.blockPosition().south();level.setBlockAndUpdate(wall,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(wall.above(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(wall.above(2),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        p.getPersistentData().putLong("tutorial_mod:last_star_move_tick",level.getGameTime()-1);before=p.position();StarExploration.activate(p,2);
        helper.assertTrue(p.position().equals(before)&&StarEnergy.get(p)==880,"Blocked movement never teleports or spends energy");
        StarArmorEventHandler.setShield(p,StarArmorEventHandler.maximumShield());
        com.example.tutorial.network.ActivateStarSkillPacket.activate(p,com.example.tutorial.network.ActivateStarSkillPacket.Skill.ARMOR_SET);
        helper.assertTrue(StarEnergy.get(p)==880,"Full shield reforge does not spend energy");
        StarArmorEventHandler.setShield(p,0);
        com.example.tutorial.network.ActivateStarSkillPacket.activate(p,com.example.tutorial.network.ActivateStarSkillPacket.Skill.ARMOR_SET);
        helper.assertTrue(StarEnergy.get(p)==730&&StarArmorEventHandler.getStellarShield(p)==StarArmorEventHandler.maximumShield(),"G still restores shield for configured cost");
        com.mojang.logging.LogUtils.getLogger().info("STAR_MOVEMENT_PASS: R/H no cooldown, old timers ignored, horizontal dash, collision safety, exact costs, full-shield feedback");
        helper.succeed();
    }
    @GameTest(template="empty",templateNamespace="forge",timeoutTicks=60)
    public static void stellarTalentCombatAndRepair(GameTestHelper helper) {
        var level=helper.getLevel();
        var p=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"StellarProcs"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        p.setPos(net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(new net.minecraft.core.BlockPos(1,1,1))));
        var sword=new ItemStack(ModItems.ICE_SWORD.get());
        sword.enchant(com.example.tutorial.enchantment.ModEnchantments.STAR_GATHERING.get(),3);
        sword.enchant(com.example.tutorial.enchantment.ModEnchantments.STAR_LEECH.get(),3);
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,sword);
        var victim=net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        var canceled=new net.minecraftforge.event.entity.living.LivingDeathEvent(victim,p.damageSources().playerAttack(p));canceled.setCanceled(true);
        com.example.tutorial.talent.StellarTalents.death(canceled);
        helper.assertTrue(com.example.tutorial.talent.StellarTalents.experience(p)==0,"Canceled deaths never award XP");
        var death=new net.minecraftforge.event.entity.living.LivingDeathEvent(victim,p.damageSources().playerAttack(p));
        com.example.tutorial.talent.StellarTalents.death(death);com.example.tutorial.talent.StellarTalents.death(death);
        helper.assertTrue(com.example.tutorial.talent.StellarTalents.experience(p)==com.example.tutorial.config.ModSettings.i("talents.killExperience"),"Kills earn XP exactly once");
        com.example.tutorial.talent.StellarTalents.award(p,9300);
        for(int node:new int[]{0,0,1,1,2,8,8,9,9,10,10,11,11})com.example.tutorial.talent.StellarTalents.action(p,1,node);
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new ItemStack(ModItems.STAR_HELMET.get()));
        var chest=new ItemStack(ModItems.STAR_CHESTPLATE.get());chest.enchant(com.example.tutorial.enchantment.ModEnchantments.STAR_WARD.get(),3);chest.enchant(com.example.tutorial.enchantment.ModEnchantments.STAR_MENDING.get(),3);
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,chest);
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS,new ItemStack(ModItems.STAR_LEGGINGS.get()));
        var boots=new ItemStack(ModItems.STAR_BOOTS.get());boots.enchant(com.example.tutorial.enchantment.ModEnchantments.STAR_STRIDE.get(),3);p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,boots);
        com.example.tutorial.talent.StellarTalents.updateAttributes(p);
        helper.assertTrue(Math.abs(p.getAttributeValue(com.example.tutorial.attribute.ModAttributes.DAMAGE_IMMUNITY.get())-.03)<.0001,"Star Ward grants additive immunity");
        helper.assertTrue(p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)>p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getBaseValue(),"Star Stride and talent grant speed");
        StarEnergy.set(p,100);p.setHealth(10);
        var target=net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);
        var wave=new AstralSlashEntity(level,p,20,false,false);
        var hit=new net.minecraftforge.event.entity.living.LivingDamageEvent(target,com.example.tutorial.damage.ModDamageTypes.astralSlash(wave,p),20);
        com.example.tutorial.talent.StellarTalents.hit(hit);
        float healed=p.getHealth();int energy=StarEnergy.get(p);
        helper.assertTrue(healed>10&&healed<=12&&energy>100,"Leech and gathering trigger on wave snapshot");
        com.example.tutorial.talent.StellarTalents.hit(new net.minecraftforge.event.entity.living.LivingDamageEvent(target,com.example.tutorial.damage.ModDamageTypes.astralSlashMagic(wave,p),20));
        helper.assertTrue(p.getHealth()==healed&&StarEnergy.get(p)==energy,"Magic half cannot duplicate resource or heal procs");
        sword.setDamageValue(20);chest.setDamageValue(20);boots.setDamageValue(20);p.tickCount=200;
        com.example.tutorial.talent.StellarTalents.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
        helper.assertTrue(sword.getDamageValue()==15&&chest.getDamageValue()==15&&boots.getDamageValue()==15,"Forging and maintenance repair exact durability");
        sword.enchant(net.minecraft.world.item.enchantment.Enchantments.MENDING,1);sword.setDamageValue(20);p.tickCount=400;
        com.example.tutorial.talent.StellarTalents.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
        helper.assertTrue(sword.getDamageValue()==20,"Default Mending stacking protection");
        com.mojang.logging.LogUtils.getLogger().info("STELLAR_COMBAT_PASS: kill XP, canceled deaths, gathering, leech, mixed-hit cap, immunity, speed, repair and mending guard");
        helper.succeed();
    }
    @GameTest(template="empty",templateNamespace="forge",timeoutTicks=60)
    public static void stellarTalentAndEnchantments(GameTestHelper helper) {
        var level=helper.getLevel();
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TalentTest"));
        player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player);
        com.example.tutorial.talent.StellarTalents.award(player,9300);
        helper.assertTrue(com.example.tutorial.talent.TalentRules.earned(com.example.tutorial.talent.StellarTalents.experience(player))==24,"Talent points capped at 24");
        com.example.tutorial.talent.StellarTalents.action(player,1,5);
        helper.assertTrue(com.example.tutorial.talent.StellarTalents.rank(player,5)==0,"Prerequisite enforced by server");
        for(int i=0;i<2;i++)com.example.tutorial.talent.StellarTalents.action(player,1,4);
        for(int i=0;i<2;i++)com.example.tutorial.talent.StellarTalents.action(player,1,5);
        helper.assertTrue(player.getMaxHealth()==20,"Unequipped talents do not grant health");
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new ItemStack(ModItems.STAR_HELMET.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,new ItemStack(ModItems.STAR_CHESTPLATE.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS,new ItemStack(ModItems.STAR_LEGGINGS.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,new ItemStack(ModItems.STAR_BOOTS.get()));
        com.example.tutorial.talent.StellarTalents.updateAttributes(player);
        com.example.tutorial.talent.StellarTalents.updateAttributes(player);
        helper.assertTrue(player.getMaxHealth()==40,"Repeated attribute updates do not stack");
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,ItemStack.EMPTY);
        com.example.tutorial.talent.StellarTalents.updateAttributes(player);
        helper.assertTrue(player.getMaxHealth()==20,"Unequipping withdraws talent attributes");
        player.experienceLevel=5;player.getInventory().add(new ItemStack(ModItems.STAR_INGOT.get(),8));
        com.example.tutorial.talent.StellarTalents.action(player,2,-1);
        helper.assertTrue(player.experienceLevel==0&&player.getInventory().countItem(ModItems.STAR_INGOT.get())==0,"Reset pays exact cost");
        helper.assertTrue(com.example.tutorial.talent.TalentRules.spent(com.example.tutorial.talent.StellarTalents.ranks(player))==0
                &&com.example.tutorial.talent.StellarTalents.experience(player)==9300,"Reset refunds allocation without erasing XP");
        com.example.tutorial.talent.StellarTalents.action(player,2,-1);
        helper.assertTrue(player.experienceLevel==0,"Repeated reset does not charge or mint points");
        var sword=new ItemStack(ModItems.ICE_SWORD.get());
        var edge=com.example.tutorial.enchantment.ModEnchantments.STAR_EDGE.get();
        helper.assertTrue(edge.canEnchant(sword)&&!edge.canEnchant(new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD)),"Sword enchantment is exclusive");
        helper.assertTrue(!com.example.tutorial.enchantment.ModEnchantments.STAR_WARD.get().canEnchant(new ItemStack(ModItems.STAR_BOOTS.get())),"Armor enchantment enforces slot");
        sword.enchant(edge,3);player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,sword);
        helper.assertTrue(Math.abs(com.example.tutorial.enchantment.ModEnchantments.getDamageFactor(sword)-1.24)<.0001,"Star Edge uses additive damage");
        var wave=new AstralSlashEntity(level,player,20,false,false);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,ItemStack.EMPTY);
        helper.assertTrue(wave.stellarWeaponSnapshot().getEnchantmentLevel(edge)==3,"Wave retains launch-time enchantment snapshot");
        var copy=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TalentClone"));
        com.example.tutorial.talent.StellarTalents.clone(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(copy,player,true));
        helper.assertTrue(com.example.tutorial.talent.StellarTalents.experience(copy)==9300,"Talent XP survives death clone");
        com.mojang.logging.LogUtils.getLogger().info("STELLAR_TALENTS_PASS: prerequisites, cap, cost, reset, attributes, snapshot, exclusive enchants, death persistence");
        helper.succeed();
    }
    @GameTest(template="empty",templateNamespace="forge",timeoutTicks=40)
    public static void serverAssetsAndProjectile(GameTestHelper helper) {
        var level=helper.getLevel();
        var recipe=level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID,"recycle_star_chestplate")).orElseThrow();
        helper.assertTrue(recipe instanceof AstralRecyclingRecipe,"Dedicated server loaded custom blasting recipe");
        helper.assertTrue(((AstralRecyclingRecipe)recipe).count(new ItemStack(ModItems.STAR_CHESTPLATE.get()))==2,"Dedicated recycling output");
        var wave=new AstralSlashEntity(ModEntities.ASTRAL_SLASH.get(),level);wave.tick();
        helper.assertTrue(wave.isRemoved(),"Ownerless waves clear safely without client classes");
        var owner=net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        var invalid=new AstralSlashEntity(level,owner,Float.NaN,false,false);
        helper.assertTrue(invalid.saveWithoutId(new net.minecraft.nbt.CompoundTag()).getFloat("Damage")==0,"NaN projectile damage rejected");
        var enormous=new AstralSlashEntity(level,owner,Float.POSITIVE_INFINITY,false,false);
        helper.assertTrue(enormous.saveWithoutId(new net.minecraft.nbt.CompoundTag()).getFloat("Damage")==1_000_000_000F,"Infinite damage bounded");
        helper.assertTrue(ItemStack.EMPTY.getCount()==0,"Empty weapon snapshots do not mutate shared empty stack");
        helper.assertTrue(level.getServer().getAdvancements().getAdvancement(ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID,"astral/multi_kill"))!=null,"Dedicated advancement assets loaded");
        com.mojang.logging.LogUtils.getLogger().info("FEATURE_DEDICATED_PASS: dedicated class loading, recipes, projectile cleanup, progression, numerical safety, empty snapshots");
        helper.succeed();
    }
    public static final class MixedWave extends AstralSlashEntity {
        public MixedWave(net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.LivingEntity owner) {
            super(level,owner,20,false,false);
        }
        public void hit(net.minecraft.world.entity.LivingEntity target) {onHitEntity(new net.minecraft.world.phys.EntityHitResult(target));}
    }
    @GameTest(template="empty",templateNamespace="forge",timeoutTicks=40)
    public static void mixedPhysicalAndMagic(GameTestHelper helper) {
        var level=helper.getLevel();
        var owner=net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        var target=net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        target.getPersistentData().putBoolean("tutorial_mod:elite_rolled",true);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(20);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS).setBaseValue(0);
        target.setHealth(200);target.setNoAi(true);
        var wave=new MixedWave(level,owner);
        var physical=com.example.tutorial.damage.ModDamageTypes.astralSlash(wave,owner);
        var magic=com.example.tutorial.damage.ModDamageTypes.astralSlashMagic(wave,owner);
        var magicTag=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath("forge","is_magic"));
        var physicalTag=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath("forge","is_physical"));
        helper.assertTrue(magic.is(magicTag) && !physical.is(magicTag),"Magic classification differs from physical");
        helper.assertTrue(physical.is(physicalTag) && !magic.is(physicalTag),"Physical classification differs from magic");
        helper.assertTrue(magic.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR) && !physical.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR),"Only magical half bypasses armor");
        wave.hit(target);
        helper.assertTrue(Math.abs(target.getHealth()-186)<.01,"20 raw mixed damage: 10 physical -> 4, 10 magic -> 10, total 14");
        com.mojang.logging.LogUtils.getLogger().info("MIXED_DAMAGE_PASS: armor 20, 20 mixed -> 14 actual, distinct forge tags, both halves bypass own iframe safely");
        helper.succeed();
    }
}
