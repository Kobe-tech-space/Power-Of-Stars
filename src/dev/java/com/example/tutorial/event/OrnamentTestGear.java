package com.example.tutorial.event;
import com.example.tutorial.ornament.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
public final class OrnamentTestGear {
    public static boolean canEquip(ServerPlayer p,OrnamentKind kind,int index){
        var stack=new ItemStack(OrnamentItems.get(kind));
        return CuriosApi.getCurio(stack).map(curio->curio.canEquip(new top.theillusivec4.curios.api.SlotContext(
                com.example.tutorial.compat.CuriosOrnaments.SLOT,p,index,false,true))).orElse(false);
    }
    public static void equip(ServerPlayer p,OrnamentKind... kinds){
        var handler=CuriosApi.getCuriosInventory(p).orElseThrow(()->new IllegalStateException("Curios player capability missing"));
        var slots=handler.getCurios().get(com.example.tutorial.compat.CuriosOrnaments.SLOT);
        if(slots==null||slots.getSlots()!=3)throw new IllegalStateException("Dedicated 3-slot setup missing");
        var items=slots.getStacks();
        for(int i=0;i<items.getSlots();i++)items.setStackInSlot(i,i<kinds.length?new ItemStack(OrnamentItems.get(kinds[i])):ItemStack.EMPTY);
    }
}
