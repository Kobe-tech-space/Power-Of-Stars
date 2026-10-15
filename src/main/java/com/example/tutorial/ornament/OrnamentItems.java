package com.example.tutorial.ornament;
import com.example.tutorial.item.ModItems;
import com.example.tutorial.item.StellarOrnamentItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import java.util.*;
public final class OrnamentItems {
    private static final Map<OrnamentKind,RegistryObject<Item>> ITEMS=new EnumMap<>(OrnamentKind.class);
    public static void register(){
        if(!ITEMS.isEmpty())return;
        for(var kind:OrnamentKind.values())ITEMS.put(kind,ModItems.ITEMS.register(kind.id,()->new StellarOrnamentItem(kind)));
    }
    public static Item get(OrnamentKind kind){return ITEMS.get(kind).get();}
    private OrnamentItems(){}
}
