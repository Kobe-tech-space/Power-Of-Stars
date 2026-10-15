package com.example.tutorial.attribute;

import com.example.tutorial.TutorialMod;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModAttributes
{
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, TutorialMod.MOD_ID);

    public static final RegistryObject<Attribute> DAMAGE_IMMUNITY = ATTRIBUTES.register("damage_immunity",
            () -> new RangedAttribute("attribute.name.tutorial_mod.damage_immunity",
                    0.0D, 0.0D, 0.95D).setSyncable(true));

    private ModAttributes() {}
    public static final RegistryObject<Attribute> DAMAGE_BONUS = ATTRIBUTES.register("damage_bonus",
            () -> new RangedAttribute("attribute.name.tutorial_mod.damage_bonus", 0, 0, 100).setSyncable(true));
    public static final RegistryObject<Attribute> DOMAIN = ATTRIBUTES.register("domain",
            () -> new RangedAttribute("attribute.name.tutorial_mod.domain", 0, 0, 2).setSyncable(true));

    public static void register(IEventBus eventBus)
    {
        ATTRIBUTES.register(eventBus);
    }
}
