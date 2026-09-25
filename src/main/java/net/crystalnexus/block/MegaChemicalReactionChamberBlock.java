package net.crystalnexus.block;

import net.crystalnexus.block.entity.MegaChemicalReactionChamberBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.world.inventory.FluidChemicalReactionChamberGUIMenu;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class MegaChemicalReactionChamberBlock extends FluidChemicalReactionChamberBlock {
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof MegaChemicalReactionChamberBlockEntity chamber) {
            if (chamber.validateStructureNow()) {
                serverPlayer.openMenu(new MenuProvider() {
                    @Override public Component getDisplayName() { return Component.literal("Mega Chemical Reaction Chamber"); }
                    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player ignored) {
                        return new FluidChemicalReactionChamberGUIMenu(id, inventory,
                            new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos));
                    }
                }, pos);
            } else {
                serverPlayer.displayClientMessage(Component.literal(chamber.getStructureError()), false);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != CrystalnexusModBlockEntities.MEGA_CHEMICAL_REACTION_CHAMBER.get() ? null
            : (BlockEntityTicker<T>) (BlockEntityTicker<MegaChemicalReactionChamberBlockEntity>) MegaChemicalReactionChamberBlockEntity::tick;
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MegaChemicalReactionChamberBlockEntity(pos, state);
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (state.getBlock() != next.getBlock() && level.getBlockEntity(pos) instanceof MegaChemicalReactionChamberBlockEntity chamber)
            chamber.onControllerRemoved();
        super.onRemove(state, level, pos, next, moving);
    }
}
