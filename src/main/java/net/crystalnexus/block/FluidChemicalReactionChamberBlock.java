package net.crystalnexus.block;

import io.netty.buffer.Unpooled;
import net.crystalnexus.block.entity.FluidChemicalReactionChamberBlockEntity;
import net.crystalnexus.procedures.FluidChemicalReactionChamberOnTickUpdateProcedure;
import net.crystalnexus.world.inventory.FluidChemicalReactionChamberGUIMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class FluidChemicalReactionChamberBlock extends ChemicalReactionChamberBlock {
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        FluidChemicalReactionChamberOnTickUpdateProcedure.execute(level, pos);
        level.scheduleTick(pos, this, 1);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new MenuProvider() {
                @Override public Component getDisplayName() { return Component.literal("Fluid Chemical Reaction Chamber"); }
                @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player ignored) {
                    return new FluidChemicalReactionChamberGUIMenu(id, inventory,
                        new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos));
                }
            }, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidChemicalReactionChamberBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof FluidChemicalReactionChamberBlockEntity chamber) {
                Containers.dropContents(level, pos, chamber);
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, moving);
        }
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof FluidChemicalReactionChamberBlockEntity chamber
            ? AbstractContainerMenu.getRedstoneSignalFromContainer(chamber) : 0;
    }
}
