package dev.worldblock.bloomery.registry;

import dev.worldblock.bloomery.Bloomery;
import dev.worldblock.bloomery.block.BloomeryBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(Bloomery.MOD_ID);

    public static final DeferredBlock<BloomeryBlock> PACKED_MUD_BLOOMERY =
            BLOCKS.registerBlock("packed_mud_bloomery", properties -> new BloomeryBlock(false, properties),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.PACKED_MUD).lightLevel(state -> state.getValue(BloomeryBlock.LIT) ? 13 : 0));

    public static final DeferredBlock<BloomeryBlock> MUD_BRICK_BLOOMERY =
            BLOCKS.registerBlock("mud_brick_bloomery", properties -> new BloomeryBlock(true, properties),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.MUD_BRICKS).lightLevel(state -> state.getValue(BloomeryBlock.LIT) ? 13 : 0));
}
