package dev.worldblock.bloomery.registry;

import dev.worldblock.bloomery.Bloomery;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Bloomery.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            CREATIVE_MODE_TABS.register("bloomery_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.bloomery"))
                    .icon(() -> new ItemStack(ModItems.PACKED_MUD_BLOOMERY.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.PACKED_MUD_BLOOMERY.get());
                        output.accept(ModItems.MUD_BRICK_BLOOMERY.get());
                    })
                    .build());

    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.PACKED_MUD_BLOOMERY.get());
            event.accept(ModItems.MUD_BRICK_BLOOMERY.get());
        }
    }
}
