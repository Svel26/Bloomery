package dev.worldblock.bloomery.registry;

import dev.worldblock.bloomery.Bloomery;
import dev.worldblock.bloomery.block.entity.BloomeryBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Bloomery.MOD_ID);

    public static final Supplier<BlockEntityType<BloomeryBlockEntity>> BLOOMERY =
            BLOCK_ENTITY_TYPES.register("bloomery", () ->
                    BlockEntityType.Builder.of(BloomeryBlockEntity::new,
                            ModBlocks.PACKED_MUD_BLOOMERY.get(),
                            ModBlocks.MUD_BRICK_BLOOMERY.get()
                    ).build(null));
}
