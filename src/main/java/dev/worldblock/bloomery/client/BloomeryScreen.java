package dev.worldblock.bloomery.client;

import dev.worldblock.bloomery.Bloomery;
import dev.worldblock.bloomery.menu.BloomeryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class BloomeryScreen extends AbstractContainerScreen<BloomeryMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Bloomery.MOD_ID, "textures/gui/bloomery.png");
    private static final ResourceLocation BURN_PROGRESS_SPRITE =
            ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");

    public BloomeryScreen(BloomeryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // When heating, overlay the bright lit campfire icon over the dark/black base icon
        if (this.menu.isHeating()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 42, 176, 32, 16, 16);
        }

        // Render arrow burn progress using vanilla furnace sprite
        int arrowWidth = Mth.ceil(this.menu.getBurnProgress() * 24.0F);
        if (arrowWidth > 0) {
            guiGraphics.blitSprite(BURN_PROGRESS_SPRITE, 24, 16, 0, 0, this.leftPos + 79, this.topPos + 34, arrowWidth, 16);
        }
    }
}

