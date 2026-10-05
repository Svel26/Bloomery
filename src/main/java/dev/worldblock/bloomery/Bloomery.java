package dev.worldblock.bloomery;

import dev.worldblock.bloomery.client.BloomeryScreen;
import dev.worldblock.bloomery.config.BloomeryConfig;
import dev.worldblock.bloomery.recipe.BloomeryRecipeManager;
import dev.worldblock.bloomery.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Bloomery.MOD_ID)
public class Bloomery {
    public static final String MOD_ID = "bloomery";
    public static final Logger LOGGER = LoggerFactory.getLogger("Bloomery");

    public Bloomery(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, BloomeryConfig.SPEC);

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modBus);
        ModMenus.MENU_TYPES.register(modBus);
        ModRecipes.RECIPE_TYPES.register(modBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modBus);

        modBus.addListener(ModCreativeTabs::addCreative);
        modBus.addListener(this::registerCapabilities);

        if (FMLEnvironment.dist.isClient()) {
            modBus.addListener(this::registerScreens);
        }

        NeoForge.EVENT_BUS.addListener(this::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(this::onDatapackSync);
        NeoForge.EVENT_BUS.addListener(dev.worldblock.bloomery.event.BloomeryEvents::onRightClickBlock);

        LOGGER.info("Bloomery mod initialized!");
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.BLOOMERY.get(),
                (bloomery, side) -> bloomery.getItemHandler(side)
        );
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.BLOOMERY_MENU.get(), BloomeryScreen::new);
    }

    private void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new BloomeryRecipeManager(event.getServerResources(), event.getRegistryAccess()));
    }

    private void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            BloomeryRecipeManager.filterAndInjectRecipes(
                    event.getPlayerList().getServer().getRecipeManager(),
                    event.getPlayerList().getServer().registryAccess()
            );
        }
    }
}
