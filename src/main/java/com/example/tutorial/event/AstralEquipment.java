package com.example.tutorial.event;
import com.example.tutorial.TutorialMod;
import com.example.tutorial.config.ModSettings;
import com.example.tutorial.item.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=TutorialMod.MOD_ID)
public final class AstralEquipment {
    public static final String LEVEL="tutorial_mod:reinforcement";
    public static boolean isEquipment(ItemStack stack) {return stack.is(ModItems.ICE_SWORD.get()) || stack.getItem() instanceof StarArmorItem;}
    public static int level(ItemStack stack) {return isEquipment(stack) && stack.hasTag()?Math.max(0,Math.min(ModSettings.i("reinforcement.maxLevel"),stack.getTag().getInt(LEVEL))):0;}
    public static int maxDamage(ItemStack stack,int base) {return base+base*level(stack)/10;}
    public static int skillCost(ServerPlayer p,int base) {
        int levels=level(p.getMainHandItem()); for(ItemStack s:p.getArmorSlots()) levels+=level(s);
        return Math.max(0,(int)Math.ceil(base*(1-Math.min(.20,levels*.008))));
    }
    public static void wearArmor(ServerPlayer p) {
        if(p.isCreative()) return;
        for(var slot:new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET}) {
            ItemStack s=p.getItemBySlot(slot); if(s.getItem() instanceof StarArmorItem) s.hurtAndBreak(ModSettings.i("skills.armorWear"),p,q->q.broadcastBreakEvent(slot));
        }
    }
    public static boolean reinforce(ServerPlayer p,ItemStack stack) {
        if(!ModSettings.enabled("reinforcement.enabled") || !isEquipment(stack)) return false;
        int next=level(stack)+1;
        if(next>ModSettings.i("reinforcement.maxLevel")) {p.displayClientMessage(Component.literal("已经达到强化上限"),true);return false;}
        int ingots=1<<next, diamonds=next*2, xp=next*5, singularities=next>=4?1:0;
        if(!p.isCreative() && (p.getInventory().countItem(ModItems.STAR_INGOT.get())<ingots || p.getInventory().countItem(Items.DIAMOND)<diamonds
            || p.getInventory().countItem(ModItems.STAR_SINGULARITY.get())<singularities || p.experienceLevel<xp)) {
            p.displayClientMessage(Component.literal("强化 "+next+"：需要星锭×"+ingots+"、钻石×"+diamonds+"、"+xp+"级经验"+(singularities>0?"、星辰奇点×1":"")),true);
            AstralSounds.play(p,"failure",net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.get(),.4F,.6F); return false;
        }
        if(!p.isCreative()) {take(p,ModItems.STAR_INGOT.get(),ingots);take(p,Items.DIAMOND,diamonds);take(p,ModItems.STAR_SINGULARITY.get(),singularities);p.giveExperienceLevels(-xp);}
        stack.getOrCreateTag().putInt(LEVEL,next); p.containerMenu.broadcastChanges();
        p.displayClientMessage(Component.literal("强化成功 · +"+next+" · 耐久上限 +"+(next*10)+"%"),true);
        AstralSounds.play(p,"reforge",net.minecraft.sounds.SoundEvents.ANVIL_USE,.65F,1.2F);return true;
    }
    private static void take(ServerPlayer p,Item item,int count) {
        for(int i=0;i<p.getInventory().getContainerSize() && count>0;i++) {var s=p.getInventory().getItem(i);if(s.is(item)) {int n=Math.min(count,s.getCount());s.shrink(n);count-=n;}}
    }
    @SubscribeEvent public static void smithing(PlayerInteractEvent.RightClickBlock e) {
        if(e.getHand()!=InteractionHand.MAIN_HAND || !e.getEntity().isShiftKeyDown() || !e.getLevel().getBlockState(e.getPos()).is(Blocks.SMITHING_TABLE) || !isEquipment(e.getItemStack())) return;
        e.setCanceled(true); e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        if(e.getEntity() instanceof ServerPlayer p && AstralSafety.valid(p)) reinforce(p,e.getItemStack());
    }
    private static boolean canRepair(ItemStack s) {return isEquipment(s) && s.isDamaged() && (ModSettings.enabled("repair.stackWithMending") || s.getEnchantmentLevel(Enchantments.MENDING)==0);}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || !ModSettings.enabled("repair.enabled") || !AstralSafety.valid(p)
            || p.tickCount%ModSettings.i("repair.fullEnergyIntervalTicks")!=0 || StarEnergy.maximum(p)==0 || StarEnergy.get(p)<StarEnergy.maximum(p)) return;
        ItemStack s=p.getMainHandItem();
        if(s.is(ModItems.ICE_SWORD.get()) && canRepair(s)) {s.setDamageValue(Math.max(0,s.getDamageValue()-ModSettings.i("repair.amount")));p.containerMenu.broadcastChanges();}
    }
    @SubscribeEvent public static void eliteRepair(LivingDeathEvent e) {
        if(e.isCanceled() || !(e.getSource().getEntity() instanceof ServerPlayer p) || !ModSettings.enabled("repair.enabled") || !e.getEntity().getPersistentData().getBoolean("tutorial_mod:astral_elite") || p.getRandom().nextDouble()>=ModSettings.d("repair.eliteChance")) return;
        var s=p.getMainHandItem(); if(canRepair(s)) {s.setDamageValue(Math.max(0,s.getDamageValue()-100));p.containerMenu.broadcastChanges();}
    }
    private AstralEquipment() {}
}
