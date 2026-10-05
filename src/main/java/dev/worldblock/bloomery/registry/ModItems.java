package dev.worldblock.bloomery.registry;

import dev.worldblock.bloomery.Bloomery;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Bloomery.MOD_ID);

    public static final DeferredItem<BlockItem> PACKED_MUD_BLOOMERY =
            ITEMS.registerSimpleBlockItem(ModBlocks.PACKED_MUD_BLOOMERY);

    public static final DeferredItem<BlockItem> MUD_BRICK_BLOOMERY =
            ITEMS.registerSimpleBlockItem(ModBlocks.MUD_BRICK_BLOOMERY);
}
