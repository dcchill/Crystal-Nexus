package net.crystalnexus.block;

import io.netty.buffer.Unpooled;
import net.crystalnexus.block.entity.TitaniumElectrolysisCellBlockEntity;
import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.processing.TieredMachineBlock;
import net.crystalnexus.world.inventory.TitaniumElectrolysisCellMenu;
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

public final class TitaniumElectrolysisCellBlock extends ChemicalReactionChamberBlock implements TieredMachineBlock {
    private final MachineTier machineTier;

    public TitaniumElectrolysisCellBlock() { this(MachineTier.TITANIUM); }
    public TitaniumElectrolysisCellBlock(MachineTier machineTier) { this.machineTier = machineTier; }
    @Override public MachineTier machineTier() { return machineTier; }

    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof TitaniumElectrolysisCellBlockEntity cell) cell.serverTick();
        level.scheduleTick(pos, this, 1);
    }

    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) serverPlayer.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() {
                return Component.translatable(machineTier == MachineTier.CHLOROPHYTE
                    ? "block.crystalnexus.chlorophyte_electrolysis_cell"
                    : "block.crystalnexus.titanium_electrolysis_cell");
            }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player ignored) {
                return new TitaniumElectrolysisCellMenu(id, inventory,
                    new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos));
            }
        }, pos);
        return InteractionResult.SUCCESS;
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TitaniumElectrolysisCellBlockEntity(pos, state);
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (state.getBlock() != next.getBlock()) {
            if (level.getBlockEntity(pos) instanceof TitaniumElectrolysisCellBlockEntity cell)
                Containers.dropContents(level, pos, cell);
            super.onRemove(state, level, pos, next, moving);
        }
    }
}
