package com.zangetsu.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public class DomainBarrierBlock extends Block {
    public DomainBarrierBlock() {
        super(BlockBehaviour.Properties.of()
                .destroyTime(-1.0f)
                .explosionResistance(3600000.0f)
                .sound(SoundType.AMETHYST)
                .lightLevel(state -> 6)
                .noLootTable()
                .isValidSpawn((state, getter, pos, type) -> false));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < 0.25f) {
            Vector3f crimson = new Vector3f(0.95f, 0.05f, 0.15f);
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(new DustParticleOptions(crimson, 1.2f), x, y, z, 0, 0, 0);
        }
    }
}
