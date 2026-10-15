package com.example.tutorial.event;
import com.example.tutorial.TutorialMod;
import com.example.tutorial.config.ModSettings;
import com.example.tutorial.item.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.commands.Commands;
@Mod.EventBusSubscriber(modid=TutorialMod.MOD_ID)
public final class AstralProgress {
    public static void award(ServerPlayer p,String name) {
        var advancement=p.server.getAdvancements().getAdvancement(ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID,"astral/"+name));
        if(advancement==null) return; var progress=p.getAdvancements().getOrStartProgress(advancement);
        if(!name.equals("root") && !name.equals("guide_gift")) award(p,"root");
        if(!progress.isDone()) for(String criterion:progress.getRemainingCriteria()) p.getAdvancements().award(advancement,criterion);
    }
    private static void equipment(ServerPlayer p,ItemStack stack) {
        if(stack.is(ModItems.STAR_INGOT.get())) {
            award(p,"ingot");
            if(!p.getPersistentData().getBoolean("tutorial_mod:recipes_unlocked")) {
                p.getPersistentData().putBoolean("tutorial_mod:recipes_unlocked",true);
                p.awardRecipesByKey(new ResourceLocation[]{ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID,"ice_sword"),ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID,"astral_guide")});
                p.sendSystemMessage(Component.literal("星锭配方已解锁 · ").append(Component.literal("[查看星环帮助]").withStyle(s->s.withColor(0xB58ADD).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/astral help")))));
            }
        }
        if(stack.is(ModItems.STAR_ORE.get())) award(p,"ore");
        if(!stack.is(ModItems.ICE_SWORD.get())) return;
        award(p,"sword");
        if(AstralGreatswordItem.isRanged(stack)) award(p,"ranged");
        if(WeaponMastery.level(stack)>=WeaponMastery.MAX_LEVEL) award(p,"mastery");
        if(ModSettings.enabled("guide.autoGift")) {
            var root=p.server.getAdvancements().getAdvancement(ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID,"astral/guide_gift"));
            if(root!=null && !p.getAdvancements().getOrStartProgress(root).isDone()) {
                award(p,"guide_gift");
                if(p.getInventory().countItem(ModItems.ASTRAL_GUIDE.get())==0) p.getInventory().placeItemBackInInventory(new ItemStack(ModItems.ASTRAL_GUIDE.get()));
                p.sendSystemMessage(Component.literal("已获得星环装备指南。右键阅读；输入 /astral help 查看功能。"));
            }
        }
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || p.tickCount%40!=0 || !AstralSafety.valid(p)) return;
        for(ItemStack s:p.getInventory().items) equipment(p,s);
        if(StarArmorItem.hasFullSet(p)) award(p,"full_set");
        if(StarEnergy.get(p)>=100) award(p,"energy_100");
        if(StarArmorEventHandler.getStellarShield(p)>=StarArmorEventHandler.maximumShield()) award(p,"shield_full");
    }
    @SubscribeEvent public static void craft(PlayerEvent.ItemCraftedEvent e) {if(e.getEntity() instanceof ServerPlayer p) equipment(p,e.getCrafting());}
    @SubscribeEvent public static void smelt(PlayerEvent.ItemSmeltedEvent e) {if(e.getEntity() instanceof ServerPlayer p) equipment(p,e.getSmelting());}
    @SubscribeEvent public static void ore(net.minecraftforge.event.level.BlockEvent.BreakEvent e) {if(e.getPlayer() instanceof ServerPlayer p && e.getState().is(com.example.tutorial.block.ModBlocks.STAR_ORE.get())) award(p,"ore");}
    @SubscribeEvent public static void clone(PlayerEvent.Clone e) {
        var original=e.getOriginal().getPersistentData();
        for(String key:new String[]{"tutorial_mod:recipes_unlocked","tutorial_mod:shield_absorb_fraction","tutorial_mod:night_energy_fraction"}) {var tag=original.get(key);if(tag!=null) e.getEntity().getPersistentData().put(key,tag.copy());}
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("astral")
            .then(Commands.literal("help").executes(c->{c.getSource().sendSuccess(()->Component.literal("L悬赏 · K自定义HUD · V重剑技能 · G重铸 · J领域 · N感知 · R冲刺 · H瞬移。潜行持装备右键锻造台：固定成本强化。高炉：未附魔装备回收。按键可重新绑定。"),false);return 1;}))
            .then(Commands.literal("stats").executes(c->{var p=c.getSource().getPlayerOrException();for(String key:AstralStatistics.KEYS) p.sendSystemMessage(Component.translatable("stat.tutorial_mod."+key).append(": "+AstralStatistics.value(p,key)));return 1;})));
    }
    private AstralProgress() {}
}
