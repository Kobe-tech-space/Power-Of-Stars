package com.example.tutorial.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.util.RandomSource;

public final class StarOreBlock extends DropExperienceBlock
{
    public StarOreBlock(BlockBehaviour.Properties properties) { super(properties, UniformInt.of(3, 7)); }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random)
    {
        if (!level.isNight() || random.nextInt(6) != 0) return;
        Direction side = Direction.getRandom(random);
        level.addParticle(ParticleTypes.END_ROD,
                pos.getX() + 0.5D + side.getStepX() * 0.56D,
                pos.getY() + 0.5D + side.getStepY() * 0.56D,
                pos.getZ() + 0.5D + side.getStepZ() * 0.56D, 0.0D, 0.008D, 0.0D);
    }
}
