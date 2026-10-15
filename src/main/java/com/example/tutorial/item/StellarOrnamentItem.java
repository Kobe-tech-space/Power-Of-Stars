package com.example.tutorial.item;
import com.example.tutorial.ornament.OrnamentKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import java.util.List;
public final class StellarOrnamentItem extends Item {
    private final OrnamentKind kind;
    public StellarOrnamentItem(OrnamentKind kind) { super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));this.kind=kind; }
    public OrnamentKind kind(){return kind;}
    @Override public void appendHoverText(ItemStack stack,@Nullable Level level,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("ornament.tutorial_mod."+kind.id+".desc").withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("ornament.tutorial_mod.equip").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("ornament.tutorial_mod.limit").withStyle(ChatFormatting.DARK_GRAY));
    }
}
