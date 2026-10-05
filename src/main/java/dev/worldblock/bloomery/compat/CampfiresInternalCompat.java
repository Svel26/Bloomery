package dev.worldblock.bloomery.compat;

import dev.worldblock.campfires.campfire.CampfireData;
import dev.worldblock.campfires.campfire.CampfireProperties;
import dev.worldblock.campfires.campfire.CampfireTicker;
import dev.worldblock.campfires.config.CampfiresConfig;
import dev.worldblock.campfires.registry.ModAttachments;
import dev.worldblock.campfires.util.TemperatureHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CampfiresInternalCompat {
    public static int getFresh(BlockState state) {
        return state.hasProperty(CampfireProperties.FRESH) ? state.getValue(CampfireProperties.FRESH) : 4;
    }

    public static int getEmber(BlockState state) {
        return state.hasProperty(CampfireProperties.EMBER) ? state.getValue(CampfireProperties.EMBER) : 0;
    }

    public static void addLog(Level level, BlockPos pos, BlockState state, CampfireBlockEntity campfire, ItemStack stack, Player player) {
        CampfireTicker.addLog(level, pos, state, campfire, stack, player);
    }

    public static boolean isIgniting(CampfireBlockEntity campfire) {
        CampfireData data = campfire.getExistingDataOrNull(ModAttachments.CAMPFIRE_DATA);
        return data != null && data.igniteTimer > 0;
    }

    public static void startStickIgnition(Level level, BlockPos pos, Player player, ItemStack stack, CampfireBlockEntity campfire) {
        CampfireTicker.startStickIgnition(level, pos, player, stack, campfire);
    }

    public static float getLightChance(Level level, BlockPos pos) {
        return TemperatureHelper.lightChance(level, pos);
    }

    public static void prepareFuel(Level level, BlockPos pos, CampfireBlockEntity campfire) {
        CampfireTicker.prepareFuel(level, pos, campfire);
    }

    public static void douseCampfire(Level level, BlockPos pos, BlockState state, CampfireBlockEntity campfire) {
        CampfireData data = campfire.getData(ModAttachments.CAMPFIRE_DATA);
        CampfireTicker.douse(level, pos, state, campfire, data);
    }

    public static void appendJadeData(CompoundTag data, Level level, BlockPos campfirePos, BlockState campfireState, CampfireBlockEntity campfire) {
        CampfireData campfireData = campfire.getExistingDataOrNull(ModAttachments.CAMPFIRE_DATA);
        data.putInt("BurnFresh", getFresh(campfireState));
        data.putInt("BurnEmber", getEmber(campfireState));
        data.putBoolean("BurnLit", campfireState.getValue(CampfireBlock.LIT));
        data.putInt("BurnFuel", campfireData == null ? -1 : campfireData.fuel);
        data.putInt("BurnEmberTimer", campfireData == null ? 0 : campfireData.emberTimer);
        data.putInt("BurnIgnite", campfireData == null ? 0 : campfireData.igniteTimer);
        data.putInt("BurnIgniteTotal", campfireData == null ? 0 : campfireData.igniteTotal);
        data.putInt("BurnPerLog", (int) (CampfiresConfig.TICKS_PER_LOG.get()
                * TemperatureHelper.burnDurationMultiplier(level, campfirePos)));
    }
}
