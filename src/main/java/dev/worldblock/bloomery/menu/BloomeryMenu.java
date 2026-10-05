package dev.worldblock.bloomery.menu;

import dev.worldblock.bloomery.config.BloomeryConfig;
import dev.worldblock.bloomery.recipe.BloomeryRecipeManager;
import dev.worldblock.bloomery.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BloomeryMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public BloomeryMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(2) {
            @Override
            public int getMaxStackSize() {
                return BloomeryConfig.INSTANCE.maxStackSize.get();
            }
        }, new SimpleContainerData(4));
    }

    public BloomeryMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenus.BLOOMERY_MENU.get(), containerId);
        checkContainerSize(container, 2);
        checkContainerDataCount(data, 4);
        this.container = container;
        this.data = data;

        // Bloomery slots (capped at container.getMaxStackSize(), default 16)
        this.addSlot(new Slot(container, 0, 56, 17) {
            @Override
            public int getMaxStackSize() {
                return container.getMaxStackSize();
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return Math.min(container.getMaxStackSize(), stack.getMaxStackSize());
            }
        });

        this.addSlot(new BloomeryResultSlot(playerInventory.player, container, 1, 116, 35) {
            @Override
            public int getMaxStackSize() {
                return container.getMaxStackSize();
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return Math.min(container.getMaxStackSize(), stack.getMaxStackSize());
            }
        });

        // Player Inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    public boolean isHeating() {
        return this.data.get(2) != 0;
    }

    public boolean isMudBrick() {
        return this.data.get(3) != 0;
    }

    public float getBurnProgress() {
        int progress = this.data.get(0);
        int total = this.data.get(1);
        return total != 0 && progress != 0 ? net.minecraft.util.Mth.clamp((float) progress / (float) total, 0.0F, 1.0F) : 0.0F;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    private boolean isBloomable(ItemStack stack, Player player) {
        return BloomeryRecipeManager.findRecipe(stack, player.level()) != null;
    }

    private boolean moveInputStack(ItemStack stack) {
        Slot inputSlot = this.slots.get(0);
        ItemStack current = inputSlot.getItem();
        int max = inputSlot.getMaxStackSize(stack);

        if (current.isEmpty()) {
            int toMove = Math.min(stack.getCount(), max);
            if (toMove > 0) {
                inputSlot.setByPlayer(stack.split(toMove));
                inputSlot.setChanged();
                return true;
            }
        } else if (ItemStack.isSameItemSameComponents(current, stack)) {
            int space = max - current.getCount();
            if (space > 0) {
                int toMove = Math.min(stack.getCount(), space);
                stack.shrink(toMove);
                current.grow(toMove);
                inputSlot.setChanged();
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            if (index == 1) { // Output slot
                if (!this.moveItemStackTo(stackInSlot, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, itemstack);
            } else if (index != 0) { // Player inventory / hotbar
                if (this.isBloomable(stackInSlot, player) && this.moveInputStack(stackInSlot)) {
                    // successfully moved into input slot
                } else if (index < 29) {
                    if (!this.moveItemStackTo(stackInSlot, 29, 38, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 38 && !this.moveItemStackTo(stackInSlot, 2, 29, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stackInSlot, 2, 38, false)) { // Input slot -> Inventory
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
        }

        return itemstack;
    }
}

