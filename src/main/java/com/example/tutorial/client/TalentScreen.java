package com.example.tutorial.client;

import com.example.tutorial.item.ModItems;
import com.example.tutorial.network.*;
import com.example.tutorial.talent.TalentRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Compact code-rendered constellation tree. No new texture dependencies. */
public final class TalentScreen extends Screen {
    private static TalentSnapshotPacket state = new TalentSnapshotPacket(false,0,new int[12],true,5,8);
    private static final int[] COLORS = {0xB295FF, 0xE7C789, 0x80D5E9};
    private int left, top, panelWidth, panelHeight, selected, pendingTicks, confirmTicks;
    private Button learn, reset;
    public TalentScreen() { super(Component.translatable("talent.tutorial_mod.title")); }
    public static void receive(TalentSnapshotPacket packet) {
        state = new TalentSnapshotPacket(packet.open(),Math.max(0,packet.experience()),TalentRules.sanitize(packet.ranks()),
                packet.enabled(),packet.resetLevels(),packet.resetIngots());
        Minecraft mc = Minecraft.getInstance();
        if (packet.open() && mc.player != null && mc.screen == null) mc.setScreen(new TalentScreen());
        if (mc.screen instanceof TalentScreen s) { s.pendingTicks=0; s.refresh(); }
    }
    private int columnWidth() { return (panelWidth - 24) / 3; }
    private int rowSpace() { return Math.max(22, (panelHeight - 140) / 4); }
    private int nodeX(int node) { return left + 12 + node / 4 * columnWidth(); }
    private int nodeY(int node) { return top + 64 + node % 4 * rowSpace(); }
    @Override protected void init() {
        panelWidth=Math.min(480,width-12);panelHeight=Math.min(310,height-12);
        left=(width-panelWidth)/2;top=(height-panelHeight)/2;
        learn=addRenderableWidget(Button.builder(Component.translatable("talent.tutorial_mod.learn"), b -> {
            if(pendingTicks>0) return;
            pendingTicks=100;refresh();ModNetwork.sendToServer(new TalentActionPacket(1,selected));
        }).bounds(left+12,top+panelHeight-28,92,20).build());
        reset=addRenderableWidget(Button.builder(Component.translatable("talent.tutorial_mod.reset"), b -> {
            if(pendingTicks>0)return;
            if(confirmTicks==0){confirmTicks=100;refresh();return;}
            confirmTicks=0;pendingTicks=100;refresh();ModNetwork.sendToServer(new TalentActionPacket(2,-1));
        }).bounds(left+panelWidth-128,top+panelHeight-28,116,20).build());
        addRenderableWidget(Button.builder(Component.literal("×"),b -> onClose()).bounds(left+panelWidth-28,top+10,16,16).build());
        refresh();
    }
    private void refresh() {
        if(learn==null)return;
        learn.active=state.enabled() && pendingTicks==0 && TalentRules.canLearn(state.experience(),state.ranks(),selected);
        reset.active=state.enabled() && pendingTicks==0 && TalentRules.spent(state.ranks())>0;
        reset.setMessage(Component.translatable(confirmTicks>0?"talent.tutorial_mod.confirm":"talent.tutorial_mod.reset"));
    }
    @Override public void tick() {
        if(pendingTicks>0 && --pendingTicks==0) {ModNetwork.sendToServer(new TalentActionPacket(0,-1));refresh();}
        if(confirmTicks>0 && --confirmTicks==0)refresh();
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        renderBackground(g);
        g.fill(left,top,left+panelWidth,top+panelHeight,0xF20D1020);
        g.fill(left,top,left+panelWidth,top+2,0xFFB295FF);
        g.drawString(font,title,left+12,top+13,0xEEE9FF,false);
        int level=TalentRules.earned(state.experience()), points=level-TalentRules.spent(state.ranks());
        g.drawString(font,Component.translatable(state.enabled()?"talent.tutorial_mod.points":"talent.tutorial_mod.disabled",points),left+12,top+30,0xD3C8EB,false);
        int bar=panelWidth-24, progress=TalentRules.progress(state.experience());
        g.fill(left+12,top+44,left+12+bar,top+47,0xFF252B40);
        g.fill(left+12,top+44,left+12+(level==24?bar:bar*progress/TalentRules.threshold(level)),top+47,0xFF9F87EA);
        for(int branch=0;branch<3;branch++) {
            int x=nodeX(branch*4), cw=columnWidth();
            g.drawString(font,Component.translatable("talent.tutorial_mod.branch."+branch),x+3,top+53,COLORS[branch],false);
            for(int row=0;row<4;row++) {
                int node=branch*4+row,y=nodeY(node),rank=state.ranks()[node];
                boolean unlocked=row==0 || state.ranks()[node-1]>=2;
                if(row>0)g.fill(x+10,y-rowSpace()+20,x+12,y,unlocked?0xFF80759F:0xFF30364A);
                int border=node==selected?COLORS[branch]:rank>0?0x706D88:0x30394F;
                g.fill(x,y,x+cw-6,y+22,0xFF000000|border);
                g.fill(x+1,y+1,x+cw-7,y+21,unlocked?0xFF1C2035:0xFF121626);
                ItemStack icon=new ItemStack(branch==0?ModItems.ICE_SWORD.get():branch==1?ModItems.STAR_CHESTPLATE.get():ModItems.STAR_INGOT.get());
                g.renderItem(icon,x+3,y+3);
                Component name=Component.translatable("talent.tutorial_mod.node."+node);
                g.drawString(font,font.plainSubstrByWidth(name.getString(),cw-50),x+23,y+7,unlocked?0xE6E1F4:0x777A90,false);
                g.drawString(font,rank+"/3",x+cw-29,y+7,COLORS[branch],false);
            }
        }
        int detailY=top+panelHeight-70;
        g.drawString(font,Component.translatable("talent.tutorial_mod.node."+selected),left+12,detailY,COLORS[selected/4],false);
        g.drawWordWrap(font,Component.translatable("talent.tutorial_mod.desc."+selected),left+12,detailY+13,panelWidth-24,0xBFC4DA);
        super.render(g,mx,my,partial);
        for(int node=0;node<12;node++) if(mx>=nodeX(node)&&mx<nodeX(node)+columnWidth()-6&&my>=nodeY(node)&&my<nodeY(node)+22) {
            var lines=new java.util.ArrayList<net.minecraft.util.FormattedCharSequence>();
            lines.add(Component.translatable("talent.tutorial_mod.node."+node).getVisualOrderText());
            lines.addAll(font.split(Component.translatable("talent.tutorial_mod.desc."+node),220));
            lines.addAll(font.split(Component.translatable("talent.tutorial_mod.prerequisite"),220));
            g.renderTooltip(font,lines,mx,my);
        }
        if(reset.isHovered())g.renderTooltip(font,Component.translatable("talent.tutorial_mod.cost",state.resetLevels(),state.resetIngots()),mx,my);
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        if(button==0)for(int node=0;node<12;node++)if(x>=nodeX(node)&&x<nodeX(node)+columnWidth()-6&&y>=nodeY(node)&&y<nodeY(node)+22) {
            selected=node;confirmTicks=0;refresh();return true;
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean isPauseScreen(){return false;}
}
