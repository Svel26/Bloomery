package dev.worldblock.bloomery.event;

import dev.worldblock.bloomery.block.BloomeryBlock;
import dev.worldblock.bloomery.compat.CampfiresIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class BloomeryEvents {
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BloomeryBlock)) {
            return;
        }

        if (CampfiresIntegration.handleInteraction(level, pos, state, event.getEntity(), event.getHand(), event.getItemStack())) {
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            event.setCanceled(true);
        }
    }
}
