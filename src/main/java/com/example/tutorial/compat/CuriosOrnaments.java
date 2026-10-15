package com.example.tutorial.compat;
import com.example.tutorial.item.StellarOrnamentItem;
import com.example.tutorial.ornament.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import java.util.*;

/** Loaded only after ModList confirms Curios. Never referenced by item superclasses. */
public final class CuriosOrnaments {
    public static final String SLOT="stellar_ornament";
    public static void initialize(){
        for(var kind:OrnamentKind.values())CuriosApi.registerCurio(OrnamentItems.get(kind),new ICurioItem(){
            @Override public boolean canEquip(SlotContext ctx,ItemStack stack){
                if(ctx.entity()==null||ctx.cosmetic()||!SLOT.equals(ctx.identifier())||ctx.index()<0||ctx.index()>=OrnamentKind.MAX_EQUIPPED)return false;
                return CuriosApi.getCuriosInventory(ctx.entity()).map(handler->{
                    var slots=handler.getCurios().get(SLOT);if(slots==null)return false;
                    var items=slots.getStacks();
                    for(int i=0;i<items.getSlots();i++)if(i!=ctx.index()&&items.getStackInSlot(i).is(stack.getItem()))return false;
                    return true;
                }).orElse(false);
            }
            @Override public boolean canEquipFromUse(SlotContext ctx,ItemStack stack){return true;}
        });
    }
    public static Set<OrnamentKind> equipped(LivingEntity entity){
        return CuriosApi.getCuriosInventory(entity).map(handler->{
            Set<OrnamentKind> kinds=EnumSet.noneOf(OrnamentKind.class);
            var slots=handler.getCurios().get(SLOT);
            if(slots!=null){var items=slots.getStacks();
                for(int i=0;i<Math.min(OrnamentKind.MAX_EQUIPPED,items.getSlots());i++)
                    if(items.getStackInSlot(i).getItem() instanceof StellarOrnamentItem item)kinds.add(item.kind());
            }
            return kinds;
        }).orElseGet(()->EnumSet.noneOf(OrnamentKind.class));
    }
    private CuriosOrnaments(){}
}
