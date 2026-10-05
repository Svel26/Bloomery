package dev.worldblock.bloomery.block.entity;

import dev.worldblock.bloomery.Bloomery;
import dev.worldblock.bloomery.block.BloomeryBlock;
import dev.worldblock.bloomery.config.BloomeryConfig;
import dev.worldblock.bloomery.menu.BloomeryMenu;
import dev.worldblock.bloomery.recipe.BloomeryRecipe;
import dev.worldblock.bloomery.recipe.BloomeryRecipeManager;
import dev.worldblock.bloomery.registry.ModBlockEntities;
import dev.worldblock.bloomery.registry.ModBlocks;
import dev.worldblock.bloomery.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BloomeryBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, StackedContentsCompatible {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int DATA_COOKING_PROGRESS = 0;
    public static final int DATA_COOKING_TOTAL_TIME = 1;
    public static final int DATA_IS_HEATING = 2;
    public static final int DATA_IS_MUD_BRICK = 3;

    private static final int[] SLOTS_UP = new int[]{SLOT_INPUT};
    private static final int[] SLOTS_DOWN = new int[]{SLOT_OUTPUT};
    private static final int[] SLOTS_HORIZONTAL = new int[]{SLOT_INPUT};

    protected NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private int cookingProgress;
    private int cookingTotalTime;
    private boolean isHeating;
    private float experience;

    private final RecipeManager.CachedCheck<SingleRecipeInput, BloomeryRecipe> quickCheck;
    protected final ContainerData dataAccess;

    private final IItemHandler[] handlers;

    public BloomeryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLOOMERY.get(), pos, state);
        this.quickCheck = RecipeManager.createCheck(ModRecipes.BLOOMING_TYPE.get());

        Direction[] directions = Direction.values();
        this.handlers = new IItemHandler[directions.length];
        for (int i = 0; i < directions.length; i++) {
            this.handlers[i] = new SidedInvWrapper(this, directions[i]);
        }

        this.dataAccess = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_COOKING_PROGRESS -> BloomeryBlockEntity.this.cookingProgress;
                    case DATA_COOKING_TOTAL_TIME -> BloomeryBlockEntity.this.cookingTotalTime;
                    case DATA_IS_HEATING -> BloomeryBlockEntity.this.isHeating ? 1 : 0;
                    case DATA_IS_MUD_BRICK -> BloomeryBlockEntity.this.isMudBrick() ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case DATA_COOKING_PROGRESS -> BloomeryBlockEntity.this.cookingProgress = value;
                    case DATA_COOKING_TOTAL_TIME -> BloomeryBlockEntity.this.cookingTotalTime = value;
                    case DATA_IS_HEATING -> BloomeryBlockEntity.this.isHeating = value != 0;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    public boolean isMudBrick() {
        return this.getBlockState().getBlock() == ModBlocks.MUD_BRICK_BLOOMERY.get();
    }

    public boolean isHeating() {
        return this.isHeating;
    }

    public void setHeating(boolean heating) {
        this.isHeating = heating;
        this.setChanged();
    }

    private boolean isMatchingWall(BlockState state) {
        if (this.isMudBrick()) {
            return state.is(Blocks.MUD_BRICKS);
        } else {
            return state.is(Blocks.PACKED_MUD);
        }
    }

    public boolean checkMultiblock() {
        if (this.level == null) return false;
        Direction facing = this.getBlockState().getValue(BloomeryBlock.FACING);
        BlockPos center = this.worldPosition.relative(facing.getOpposite());

        // 1. Lit campfire must be directly behind the bloomery block at center
        BlockState campfireState = this.level.getBlockState(center);
        if (!(campfireState.getBlock() instanceof CampfireBlock && campfireState.getValue(CampfireBlock.LIT))) {
            return false;
        }

        // 2. Three walls enclosing the campfire on Layer 0 (left, right, back)
        Direction back = facing.getOpposite();
        Direction left = facing.getCounterClockWise();
        Direction right = facing.getClockWise();

        if (!isMatchingWall(this.level.getBlockState(center.relative(left)))) return false;
        if (!isMatchingWall(this.level.getBlockState(center.relative(right)))) return false;
        if (!isMatchingWall(this.level.getBlockState(center.relative(back)))) return false;

        // 3. Four chimney walls on Layer 1 (above the bloomery, left, right, and back)
        if (!isMatchingWall(this.level.getBlockState(this.worldPosition.above()))) return false;
        if (!isMatchingWall(this.level.getBlockState(center.relative(left).above()))) return false;
        if (!isMatchingWall(this.level.getBlockState(center.relative(right).above()))) return false;
        if (!isMatchingWall(this.level.getBlockState(center.relative(back).above()))) return false;

        return true;
    }

    public void logMultiblockStatus() {
        if (this.level == null) return;
        Direction facing = this.getBlockState().getValue(BloomeryBlock.FACING);
        BlockPos center = this.worldPosition.relative(facing.getOpposite());

        BlockState campfireState = this.level.getBlockState(center);
        if (!(campfireState.getBlock() instanceof CampfireBlock && campfireState.getValue(CampfireBlock.LIT))) {
            Bloomery.LOGGER.info("Bloomery at {}: Multiblock incomplete -> No lit campfire found directly behind bloomery at {} (found: {})",
                    this.worldPosition, center, campfireState.getBlock());
            return;
        }

        Direction back = facing.getOpposite();
        Direction left = facing.getCounterClockWise();
        Direction right = facing.getClockWise();

        BlockPos leftWall = center.relative(left);
        if (!isMatchingWall(this.level.getBlockState(leftWall))) {
            Bloomery.LOGGER.info("Bloomery at {}: Multiblock incomplete -> Missing wall on left at {} (found: {})",
                    this.worldPosition, leftWall, this.level.getBlockState(leftWall).getBlock());
            return;
        }

        BlockPos rightWall = center.relative(right);
        if (!isMatchingWall(this.level.getBlockState(rightWall))) {
            Bloomery.LOGGER.info("Bloomery at {}: Multiblock incomplete -> Missing wall on right at {} (found: {})",
                    this.worldPosition, rightWall, this.level.getBlockState(rightWall).getBlock());
            return;
        }

        BlockPos backWall = center.relative(back);
        if (!isMatchingWall(this.level.getBlockState(backWall))) {
            Bloomery.LOGGER.info("Bloomery at {}: Multiblock incomplete -> Missing wall behind at {} (found: {})",
                    this.worldPosition, backWall, this.level.getBlockState(backWall).getBlock());
            return;
        }

        BlockPos frontChimney = this.worldPosition.above();
        if (!isMatchingWall(this.level.getBlockState(frontChimney))) {
            Bloomery.LOGGER.info("Bloomery at {}: Multiblock incomplete -> Missing chimney block directly above bloomery at {} (found: {})",
                    this.worldPosition, frontChimney, this.level.getBlockState(frontChimney).getBlock());
            return;
        }

        BlockPos leftChimney = leftWall.above();
        if (!isMatchingWall(this.level.getBlockState(leftChimney))) {
            Bloomery.LOGGER.info("Bloomery at {}: Multiblock incomplete -> Missing left chimney block at {} (found: {})",
                    this.worldPosition, leftChimney, this.level.getBlockState(leftChimney).getBlock());
            return;
        }

        BlockPos rightChimney = rightWall.above();
        if (!isMatchingWall(this.level.getBlockState(rightChimney))) {
            Bloomery.LOGGER.info("Bloomery at {}: Multiblock incomplete -> Missing right chimney block at {} (found: {})",
                    this.worldPosition, rightChimney, this.level.getBlockState(rightChimney).getBlock());
            return;
        }

        BlockPos backChimney = backWall.above();
        if (!isMatchingWall(this.level.getBlockState(backChimney))) {
            Bloomery.LOGGER.info("Bloomery at {}: Multiblock incomplete -> Missing back chimney block at {} (found: {})",
                    this.worldPosition, backChimney, this.level.getBlockState(backChimney).getBlock());
            return;
        }

        Bloomery.LOGGER.info("Bloomery at {}: Multiblock is complete and valid!", this.worldPosition);
    }

    public @Nullable BloomeryRecipeManager.BloomingResult getBloomingResult(ItemStack input) {
        if (input.isEmpty() || this.level == null) return null;

        // 1. Try datapack RecipeManager check
        Optional<RecipeHolder<BloomeryRecipe>> opt = this.quickCheck.getRecipeFor(new SingleRecipeInput(input), this.level);
        if (opt.isPresent()) {
            BloomeryRecipe r = opt.get().value();
            ItemStack res = r.assemble(new SingleRecipeInput(input), this.level.registryAccess());
            return new BloomeryRecipeManager.BloomingResult(res, r.getCookingTime(), r.getExperience());
        }

        // 2. Direct fallback resolver (covers captured smelting, tags, names)
        return BloomeryRecipeManager.findRecipe(input, this.level);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BloomeryBlockEntity entity) {
        boolean wasHeating = entity.isHeating;

        if (level.getGameTime() % 10 == 0 || !entity.isHeating) {
            entity.isHeating = entity.checkMultiblock();
        }

        if (wasHeating != entity.isHeating) {
            level.setBlock(pos, state.setValue(BloomeryBlock.LIT, entity.isHeating), Block.UPDATE_ALL);
            Bloomery.LOGGER.info("Bloomery at {} heating state changed to {} (facing={})",
                    pos, entity.isHeating, state.getValue(BloomeryBlock.FACING));
            entity.setChanged();
        }

        ItemStack inputStack = entity.items.get(SLOT_INPUT);
        boolean hasInput = !inputStack.isEmpty();

        if (hasInput) {
            BloomeryRecipeManager.BloomingResult recipe = entity.getBloomingResult(inputStack);
            int maxStack = entity.getMaxStackSize();

            if (entity.isHeating && canBurn(recipe, entity.items, maxStack)) {
                if (entity.cookingTotalTime <= 0) {
                    if (entity.isMudBrick()) {
                        int base = (recipe != null && recipe.cookTime() > 0) ? (recipe.cookTime() / 2) : BloomeryConfig.INSTANCE.mudBrickCookingTime.get();
                        entity.cookingTotalTime = Math.max(1, base);
                    } else {
                        int base = (recipe != null && recipe.cookTime() > 0) ? recipe.cookTime() : BloomeryConfig.INSTANCE.packedMudCookingTime.get();
                        entity.cookingTotalTime = Math.max(1, base);
                    }
                    Bloomery.LOGGER.info("Bloomery at {} started smelting {} -> {} (target: {} ticks)",
                            pos, inputStack.getItem(), recipe.result().getItem(), entity.cookingTotalTime);
                }

                entity.cookingProgress++;
                entity.setChanged();

                if (entity.cookingProgress >= entity.cookingTotalTime) {
                    entity.cookingProgress = 0;
                    entity.cookingTotalTime = 0;
                    if (burn(recipe, entity.items, maxStack)) {
                        entity.experience += recipe.experience();
                        Bloomery.LOGGER.info("Bloomery at {} finished smelting into {}", pos, recipe.result());
                    }
                    entity.setChanged();
                }
            } else {
                if (!entity.isHeating && entity.cookingProgress > 0) {
                    entity.cookingProgress = Math.max(0, entity.cookingProgress - 2);
                    entity.setChanged();
                }
            }
        } else {
            if (entity.cookingProgress > 0) {
                entity.cookingProgress = 0;
                entity.cookingTotalTime = 0;
                entity.setChanged();
            }
        }
    }

    private static boolean canBurn(@Nullable BloomeryRecipeManager.BloomingResult recipe, NonNullList<ItemStack> items, int maxStack) {
        if (items.get(SLOT_INPUT).isEmpty() || recipe == null) {
            return false;
        }
        ItemStack result = recipe.result();
        if (result.isEmpty()) {
            return false;
        }
        ItemStack output = items.get(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(output, result)) {
            return false;
        }
        int combined = output.getCount() + result.getCount();
        return combined <= maxStack && combined <= output.getMaxStackSize();
    }

    private static boolean burn(@Nullable BloomeryRecipeManager.BloomingResult recipe, NonNullList<ItemStack> items, int maxStack) {
        if (recipe == null || !canBurn(recipe, items, maxStack)) {
            return false;
        }
        ItemStack input = items.get(SLOT_INPUT);
        ItemStack result = recipe.result();
        ItemStack output = items.get(SLOT_OUTPUT);

        if (output.isEmpty()) {
            items.set(SLOT_OUTPUT, result.copy());
        } else if (ItemStack.isSameItemSameComponents(output, result)) {
            output.grow(result.getCount());
        }
        input.shrink(1);
        return true;
    }

    public void awardUsedRecipesAndPopExperience(ServerPlayer player) {
        if (this.experience > 0 && this.level instanceof ServerLevel serverLevel) {
            int xp = (int) this.experience;
            float rem = this.experience - xp;
            if (rem > 0 && Math.random() < rem) {
                xp++;
            }
            this.experience = 0;
            if (xp > 0) {
                ExperienceOrb.award(serverLevel, Vec3.atCenterOf(this.worldPosition), xp);
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.cookingProgress = tag.getInt("CookingProgress");
        this.cookingTotalTime = tag.getInt("CookingTotalTime");
        this.experience = tag.getFloat("Experience");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CookingProgress", this.cookingProgress);
        tag.putInt("CookingTotalTime", this.cookingTotalTime);
        tag.putFloat("Experience", this.experience);
        ContainerHelper.saveAllItems(tag, this.items, registries);
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public int getMaxStackSize() {
        return BloomeryConfig.INSTANCE.maxStackSize.get();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(this.isMudBrick() ? "container.bloomery.mud_brick" : "container.bloomery.packed_mud");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return new BloomeryMenu(containerId, playerInventory, this, this.dataAccess);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return SLOTS_DOWN;
        } else if (side == Direction.UP) {
            return SLOTS_UP;
        } else {
            return SLOTS_HORIZONTAL;
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_INPUT) {
            ItemStack current = this.items.get(SLOT_INPUT);
            return current.getCount() < this.getMaxStackSize() && (current.isEmpty() || ItemStack.isSameItemSameComponents(current, stack));
        }
        return false;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return this.canPlaceItem(slot, stack) && side != Direction.DOWN;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT && side == Direction.DOWN;
    }

    @Override
    public void fillStackedContents(StackedContents contents) {
        for (ItemStack itemstack : this.items) {
            contents.accountStack(itemstack);
        }
    }

    public @Nullable IItemHandler getItemHandler(@Nullable Direction side) {
        if (side == null) {
            return this.handlers[0];
        }
        return this.handlers[side.ordinal()];
    }
}
