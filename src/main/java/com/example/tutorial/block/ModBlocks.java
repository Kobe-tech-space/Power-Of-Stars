package com.example.tutorial.block;

import com.example.tutorial.TutorialMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks
{
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, TutorialMod.MOD_ID);

    public static final RegistryObject<Block> STAR_ORE = BLOCKS.register("star_ore",
            () -> new StarOreBlock(BlockBehaviour.Properties.copy(Blocks.IRON_ORE)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 3)));

    public static final RegistryObject<Block> STAR_INGOT_BLOCK = BLOCKS.register("star_ingot_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).lightLevel(state -> 5)));

    private ModBlocks()
    {
    }

    public static void register(IEventBus eventBus)
    {
        BLOCKS.register(eventBus);
    }
}
