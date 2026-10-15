package com.example.tutorial.entity;

import com.example.tutorial.TutorialMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities
{
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TutorialMod.MOD_ID);

    public static final RegistryObject<EntityType<AstralSlashEntity>> ASTRAL_SLASH =
            ENTITIES.register("astral_slash", () -> EntityType.Builder
                    .<AstralSlashEntity>of(AstralSlashEntity::new, MobCategory.MISC)
                    .sized(1.65F, 1.65F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build(TutorialMod.MOD_ID + ":astral_slash"));

    private ModEntities()
    {
    }

    public static void register(IEventBus eventBus)
    {
        ENTITIES.register(eventBus);
    }
}
