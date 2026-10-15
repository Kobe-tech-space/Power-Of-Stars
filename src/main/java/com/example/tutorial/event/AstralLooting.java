package com.example.tutorial.event;
import com.example.tutorial.TutorialMod;
import com.example.tutorial.entity.AstralSlashEntity;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=TutorialMod.MOD_ID)
public final class AstralLooting {
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void looting(LootingLevelEvent e) {
        if(e.getDamageSource()!=null && e.getDamageSource().getDirectEntity() instanceof AstralSlashEntity wave) e.setLootingLevel(wave.lootingLevel());
    }
}
