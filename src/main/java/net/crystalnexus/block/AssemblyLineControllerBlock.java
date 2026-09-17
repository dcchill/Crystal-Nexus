package net.crystalnexus.block;

import net.crystalnexus.block.entity.AssemblyLineControllerBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class AssemblyLineControllerBlock extends AssemblyLineCasingBlock implements EntityBlock {
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AssemblyLineControllerBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != CrystalnexusModBlockEntities.ASSEMBLY_LINE_CONTROLLER.get() ? null
            : (l, p, s, be) -> AssemblyLineControllerBlockEntity.tick(l, p, s, (AssemblyLineControllerBlockEntity) be);
    }
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof AssemblyLineControllerBlockEntity be) server.openMenu(be, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (next.getBlock() != state.getBlock() && level.getBlockEntity(pos) instanceof AssemblyLineControllerBlockEntity be) be.dropContents();
        super.onRemove(state, level, pos, next, moving);
    }
}
