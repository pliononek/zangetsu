package com.zangetsu.init;

import com.zangetsu.ZangetsuMod;
import com.zangetsu.block.DomainBarrierBlock;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ZangetsuMod.MODID);

    public static final DeferredHolder<Block, DomainBarrierBlock> DOMAIN_BARRIER =
            BLOCKS.register("domain_barrier", DomainBarrierBlock::new);
}
