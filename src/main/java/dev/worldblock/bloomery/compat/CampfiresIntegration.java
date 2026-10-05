package dev.worldblock.bloomery.compat;

import dev.worldblock.bloomery.block.BloomeryBlock;
import dev.worldblock.bloomery.block.entity.BloomeryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

public class CampfiresIntegration {
    public static boolean isCampfiresLoaded() {
        return ModList.get().isLoaded("campfires");
    }

    public static BlockPos getCampfirePos(BlockPos bloomeryPos, BlockState bloomeryState) {
        Direction facing = bloomeryState.getValue(BloomeryBlock.FACING);
        return bloomeryPos.relative(facing.getOpposite());
    }

    public static boolean handleInteraction(Level level, BlockPos pos, BlockState state, Player player, InteractionHand hand, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        BlockPos campfirePos = getCampfirePos(pos, state);
        BlockState campfireState = level.getBlockState(campfirePos);
        if (!(campfireState.getBlock() instanceof CampfireBlock)) {
            return false;
        }

        if (!(level.getBlockEntity(campfirePos) instanceof CampfireBlockEntity campfire)) {
            return false;
        }

        boolean lit = campfireState.getValue(CampfireBlock.LIT);
        boolean waterlogged = campfireState.getValue(CampfireBlock.WATERLOGGED);
        EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;

        // 1. Shovel - Put out campfire
        if (stack.getItem() instanceof ShovelItem || stack.is(ItemTags.SHOVELS)) {
            if (!lit) {
                return false;
            }
            if (!level.isClientSide) {
                if (isCampfiresLoaded()) {
                    CampfiresInternalCompat.douseCampfire(level, campfirePos, campfireState, campfire);
                } else {
                    CampfireBlock.dowse(player, level, campfirePos, campfireState);
                }

                if (!player.getAbilities().instabuild) {
                    stack.hurtAndBreak(1, player, slot);
                }

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            campfirePos.getX() + 0.5, campfirePos.getY() + 0.5, campfirePos.getZ() + 0.5,
                            8, 0.2, 0.1, 0.2, 0.02);
                }

                if (level.getBlockEntity(pos) instanceof BloomeryBlockEntity bloomery) {
                    bloomery.setHeating(false);
                }
                level.setBlock(pos, state.setValue(BloomeryBlock.LIT, false), Block.UPDATE_ALL);
            }
            return true;
        }

        // 2. Logs - Add log to campfire (campfires mod mechanic)
        if (stack.is(ItemTags.LOGS)) {
            if (isCampfiresLoaded()) {
                int fresh = CampfiresInternalCompat.getFresh(campfireState);
                if (fresh < 4) {
                    if (!level.isClientSide) {
                        CampfiresInternalCompat.addLog(level, campfirePos, campfireState, campfire, stack, player);
                    }
                    return true;
                }
            }
            return false;
        }

        // 3. Stick - Stick light mechanic (campfires mod mechanic)
        if (stack.is(Items.STICK)) {
            if (!lit && !waterlogged && isCampfiresLoaded()) {
                int fresh = CampfiresInternalCompat.getFresh(campfireState);
                if (fresh > 0) {
                    if (!level.isClientSide) {
                        CampfiresInternalCompat.startStickIgnition(level, campfirePos, player, stack, campfire);
                    }
                    return true;
                }
            }
            return false;
        }

        // 4. Flint and Steel - Light campfire
        if (stack.is(Items.FLINT_AND_STEEL) || stack.getItem() instanceof FlintAndSteelItem) {
            if (!lit && !waterlogged) {
                if (isCampfiresLoaded()) {
                    int fresh = CampfiresInternalCompat.getFresh(campfireState);
                    if (fresh <= 0) {
                        return false;
                    }

                    if (!level.isClientSide) {
                        float chance = CampfiresInternalCompat.getLightChance(level, campfirePos);
                        if (level.random.nextFloat() < chance) {
                            CampfiresInternalCompat.prepareFuel(level, campfirePos, campfire);
                            level.setBlock(campfirePos, campfireState.setValue(CampfireBlock.LIT, true), 3);
                            level.playSound(null, campfirePos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.4F + 0.8F);
                            level.playSound(null, campfirePos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.8F, 1.0F);
                            if (level instanceof ServerLevel serverLevel) {
                                serverLevel.sendParticles(ParticleTypes.FLAME,
                                        campfirePos.getX() + 0.5, campfirePos.getY() + 0.4, campfirePos.getZ() + 0.5,
                                        10, 0.25, 0.15, 0.25, 0.02);
                            }
                            if (!player.getAbilities().instabuild) {
                                stack.hurtAndBreak(1, player, slot);
                            }
                            campfire.setChanged();
                            level.scheduleTick(campfirePos, campfireState.getBlock(), 1);

                            if (level.getBlockEntity(pos) instanceof BloomeryBlockEntity bloomery) {
                                if (bloomery.checkMultiblock()) {
                                    bloomery.setHeating(true);
                                    level.setBlock(pos, state.setValue(BloomeryBlock.LIT, true), Block.UPDATE_ALL);
                                }
                            }
                        } else {
                            if (!player.getAbilities().instabuild) {
                                stack.hurtAndBreak(1, player, slot);
                            }
                            level.playSound(null, campfirePos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7F, 0.9F + level.random.nextFloat() * 0.2F);
                            if (level instanceof ServerLevel serverLevel) {
                                serverLevel.sendParticles(ParticleTypes.SMOKE,
                                        campfirePos.getX() + 0.5, campfirePos.getY() + 0.4, campfirePos.getZ() + 0.5,
                                        8, 0.25, 0.1, 0.25, 0.01);
                            }
                        }
                    }
                    return true;
                } else {
                    // Vanilla campfire lighting fallback
                    if (!level.isClientSide) {
                        level.setBlock(campfirePos, campfireState.setValue(CampfireBlock.LIT, true), 3);
                        level.playSound(null, campfirePos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.4F + 0.8F);
                        if (!player.getAbilities().instabuild) {
                            stack.hurtAndBreak(1, player, slot);
                        }
                        if (level.getBlockEntity(pos) instanceof BloomeryBlockEntity bloomery) {
                            if (bloomery.checkMultiblock()) {
                                bloomery.setHeating(true);
                                level.setBlock(pos, state.setValue(BloomeryBlock.LIT, true), Block.UPDATE_ALL);
                            }
                        }
                    }
                    return true;
                }
            }
        }

        return false;
    }
}
