package com.example.tutorial.event;
import com.example.tutorial.config.ModSettings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
/** Persistent timestamps avoid an unbounded global player cache. */
public final class AstralSounds {
    public static void play(ServerPlayer p,String group,SoundEvent sound,float volume,float pitch) {
        if(!ModSettings.enabled("audio.enabled") || !p.isAlive()) return;
        long now=p.server.overworld().getGameTime(); String key="tutorial_mod:sound_"+group;
        var data=p.getPersistentData();
        if(data.contains(key) && now>=data.getLong(key) && now-data.getLong(key)<ModSettings.i("audio.minimumIntervalTicks")) return;
        data.putLong(key,now); p.playNotifySound(sound,SoundSource.PLAYERS,Math.min(1,volume),Math.max(.5F,Math.min(2,pitch)));
    }
    public static void threshold(ServerPlayer p,int before,int after) {
        if(before<100 && after>=100) play(p,"energy",SoundEvents.BEACON_POWER_SELECT,.65F,1.65F);
        else if(before<50 && after>=50) play(p,"energy",SoundEvents.EXPERIENCE_ORB_PICKUP,.4F,1.25F);
    }
    public static void shield(ServerPlayer p,float before,float after) {
        if(after<=.01F) {play(p,"shield_break",SoundEvents.GLASS_BREAK,.85F,.65F); p.getPersistentData().putLong("tutorial_mod:shield_dark_until",p.server.overworld().getGameTime()+60);}
        else if(before>StarArmorEventHandler.maximumShield()*.25F && after<=StarArmorEventHandler.maximumShield()*.25F) play(p,"shield",SoundEvents.NOTE_BLOCK_BASS.get(),.6F,.7F);
        else play(p,"shield",SoundEvents.AMETHYST_BLOCK_HIT,.35F,1.2F);
    }
    private AstralSounds() {}
}
