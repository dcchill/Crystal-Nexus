package net.crystalnexus.block;

import net.crystalnexus.block.entity.ConveyerBeltBaseBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.HashSet;
import java.util.Set;

public final class ConveyerBeltMode {
    private static final Set<BlockPos> SWITCHING = new HashSet<>();

    public enum Mode {
        NORMAL,
        INPUT,
        OUTPUT;

        public Mode next() {
            return switch (this) {
                case NORMAL -> INPUT;
                case INPUT -> OUTPUT;
                case OUTPUT -> NORMAL;
            };
        }

        static Mode forStorage(boolean hasFrontStorage, boolean hasBackStorage) {
            return hasFrontStorage ? INPUT : hasBackStorage ? OUTPUT : NORMAL;
        }
    }

    private ConveyerBeltMode() {}

    public static void updateAutomatic(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ConveyerBeltBlock)
                || !state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                || SWITCHING.contains(pos)
                || level.getBlockEntity(pos) instanceof ConveyerBeltBaseBlockEntity belt && belt.isManualMode()) {
            return;
        }

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        switchTo(level, pos, modeFor(
                hasItemStorage(level, pos.relative(facing), facing.getOpposite()),
                hasItemStorage(level, pos.relative(facing.getOpposite()), facing)
        ));
    }

    public static Mode cycle(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ConveyerBeltBaseBlockEntity belt) {
            belt.setManualMode(true);
        }
        Mode next = modeAt(level.getBlockState(pos)).next();
        switchTo(level, pos, next);
        return next;
    }

    static Mode modeFor(boolean hasFrontStorage, boolean hasBackStorage) {
        return Mode.forStorage(hasFrontStorage, hasBackStorage);
    }

    private static boolean hasItemStorage(Level level, BlockPos pos, Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity != null
                && !(blockEntity instanceof ConveyerBeltBaseBlockEntity)
                && level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side) != null;
    }

    private static Mode modeAt(BlockState state) {
        if (state.getBlock() instanceof ConveyerBeltInputBlock) return Mode.INPUT;
        if (state.getBlock() instanceof ConveyerBeltOutputBlock) return Mode.OUTPUT;
        return Mode.NORMAL;
    }

    public static ConveyerBeltTier tierAt(BlockState state) {
        return ConveyerBeltTierState.tierAt(state);
    }

    public static Block normalBlock(BlockState state) {
        return normalBlock(tierAt(state));
    }

    private static Block normalBlock(ConveyerBeltTier tier) {
        return ConveyerBeltTierState.normalBlock(tier);
    }

    private static void switchTo(Level level, BlockPos pos, Mode mode) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) || modeAt(state) == mode) return;

        ConveyerBeltTier tier = tierAt(state);

        CompoundTag data = null;
        if (level.getBlockEntity(pos) instanceof ConveyerBeltBaseBlockEntity belt) {
            data = belt.saveWithFullMetadata(level.registryAccess());
            belt.clearContent();
        }

        BlockState replacement = (switch (mode) {
            case NORMAL -> normalBlock(tier).defaultBlockState();
            case INPUT -> CrystalnexusModBlocks.CONVEYER_BELT_INPUT.get().defaultBlockState();
            case OUTPUT -> CrystalnexusModBlocks.CONVEYER_BELT_OUTPUT.get().defaultBlockState();
        }).setValue(BlockStateProperties.HORIZONTAL_FACING, state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        replacement = ConveyerBeltTierState.applyTier(replacement, tier);
        BlockPos key = pos.immutable();
        SWITCHING.add(key);
        try {
            level.setBlock(pos, replacement, Block.UPDATE_ALL);
        } finally {
            SWITCHING.remove(key);
        }

        if (data != null && level.getBlockEntity(pos) instanceof ConveyerBeltBaseBlockEntity belt) {
            belt.loadWithComponents(data, level.registryAccess());
            belt.setChanged();
        }
    }
}
