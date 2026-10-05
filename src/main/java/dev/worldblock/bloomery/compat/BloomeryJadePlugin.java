package dev.worldblock.bloomery.compat;

import dev.worldblock.bloomery.Bloomery;
import dev.worldblock.bloomery.block.BloomeryBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin(Bloomery.MOD_ID)
public class BloomeryJadePlugin implements IWailaPlugin {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Bloomery.MOD_ID, "bloomery_campfire");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(BloomeryServerData.INSTANCE, BloomeryBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(BloomeryClientData.INSTANCE, BloomeryBlock.class);
    }

    private static final class BloomeryServerData implements IServerDataProvider<BlockAccessor> {
        private static final BloomeryServerData INSTANCE = new BloomeryServerData();

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            BlockPos bloomeryPos = accessor.getPosition();
            BlockState bloomeryState = accessor.getBlockState();
            if (!(bloomeryState.getBlock() instanceof BloomeryBlock)) {
                return;
            }

            BlockPos campfirePos = CampfiresIntegration.getCampfirePos(bloomeryPos, bloomeryState);
            Level level = accessor.getLevel();
            BlockState campfireState = level.getBlockState(campfirePos);
            if (!(campfireState.getBlock() instanceof CampfireBlock)) {
                return;
            }
            if (!(level.getBlockEntity(campfirePos) instanceof CampfireBlockEntity campfire)) {
                return;
            }

            if (ModList.get().isLoaded("campfires")) {
                CampfiresInternalCompat.appendJadeData(data, level, campfirePos, campfireState, campfire);
            } else {
                data.putInt("BurnFresh", 4);
                data.putInt("BurnEmber", 0);
                data.putBoolean("BurnLit", campfireState.getValue(CampfireBlock.LIT));
                data.putInt("BurnFuel", -1);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private static final class BloomeryClientData implements IBlockComponentProvider {
        private static final BloomeryClientData INSTANCE = new BloomeryClientData();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains("BurnFresh")) {
                return;
            }
            int fresh = data.getInt("BurnFresh");
            int ember = data.getInt("BurnEmber");
            if (data.getBoolean("BurnLit")) {
                int remaining = Math.max(0, data.getInt("BurnFuel"))
                        + Math.max(0, fresh - 1) * data.getInt("BurnPerLog");
                tooltip.add(Component.translatable("jade.campfires.burn_time", formatTicks(remaining)));
                tooltip.add(Component.translatable("jade.campfires.logs", fresh));
            } else if (data.getInt("BurnIgnite") > 0) {
                int total = Math.max(1, data.getInt("BurnIgniteTotal"));
                int percent = (int) ((1.0F - (float) data.getInt("BurnIgnite") / (float) total) * 100.0F);
                tooltip.add(Component.translatable("jade.campfires.igniting", percent));
            } else if (ember > 0) {
                tooltip.add(Component.translatable("jade.campfires.embers", formatTicks(data.getInt("BurnEmberTimer"))));
            } else if (fresh > 0) {
                tooltip.add(Component.translatable("jade.campfires.unlit", fresh));
            } else {
                tooltip.add(Component.translatable("jade.campfires.burnt"));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        private static String formatTicks(int ticks) {
            int totalSeconds = Math.max(0, ticks) / 20;
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            return minutes > 0 ? minutes + "m " + seconds + "s" : seconds + "s";
        }
    }
}
