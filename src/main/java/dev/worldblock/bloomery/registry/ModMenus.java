package dev.worldblock.bloomery.registry;

import dev.worldblock.bloomery.Bloomery;
import dev.worldblock.bloomery.menu.BloomeryMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, Bloomery.MOD_ID);

    public static final Supplier<MenuType<BloomeryMenu>> BLOOMERY_MENU =
            MENU_TYPES.register("bloomery", () -> new MenuType<>(BloomeryMenu::new, FeatureFlags.DEFAULT_FLAGS));
}
