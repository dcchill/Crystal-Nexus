package net.crystalnexus.block.entity;

import net.crystalnexus.block.PipeStraightBlock;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;


public class PipeStraightBlockEntity extends BlockEntity {
    public static final int CAPACITY = 15_000; // mB
    public static final int MAX_TRANSFER = 5_000; // mB per tick
    public static final int TRANSFER_DELAY_TICKS = 0;

    private int inputSides;
    private int outputSides;
    private int automaticInputSides;
    private int automaticOutputSides;
    private int transferCooldown;

    private final FluidTank fluidTank = new FluidTank(CAPACITY) {
        @Override
        public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
            boolean wasEmpty = isEmpty();
            int filled = super.fill(resource, action);
            if (wasEmpty && filled > 0 && action == IFluidHandler.FluidAction.EXECUTE) delayTransfer();
            return filled;
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null) {
                BlockState state = level.getBlockState(worldPosition);
                level.sendBlockUpdated(worldPosition, state, state, 2);
            }
        }
    };

    public PipeStraightBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.PIPE_STRAIGHT.get(), pos, state);
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    public boolean isInputSide(Direction direction) {
        return (inputSides & 1 << direction.ordinal()) != 0;
    }

    public boolean isOutputSide(Direction direction) {
        return (outputSides & 1 << direction.ordinal()) != 0;
    }

    public int cycleSideMode(Direction direction) {
        int side = 1 << direction.ordinal();
        int mode;
        if ((inputSides & side) != 0) {
            inputSides &= ~side;
            outputSides |= side;
            mode = 2;
        } else if ((outputSides & side) != 0) {
            outputSides &= ~side;
            mode = 0;
        } else {
            inputSides |= side;
            mode = 1;
        }
        automaticInputSides &= ~side;
        automaticOutputSides &= ~side;
        sync();
        return mode;
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;
        if (transferCooldown > 0) {
            transferCooldown--;
            return;
        }
        BlockState state = getBlockState();

        for (Direction direction : Direction.values()) {
            int side = 1 << direction.ordinal();
            if (!state.getValue(PipeStraightBlock.property(direction))) {
                automaticInputSides &= ~side;
                automaticOutputSides &= ~side;
                continue;
            }
            BlockPos neighborPos = worldPosition.relative(direction);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (neighbor instanceof PipeStraightBlockEntity) continue;
            if (neighbor instanceof TemporalExploiterBlockEntity) {
                automaticInputSides &= ~side;
                automaticOutputSides |= side;
                continue;
            }
            boolean defaultMode = (inputSides & side) == 0 && (outputSides & side) == 0;
            if (((inputSides & side) != 0 || defaultMode && (automaticOutputSides & side) == 0)
                && pullFrom(neighborPos, direction.getOpposite()) > 0) {
                if (defaultMode) automaticInputSides |= side;
                break;
            }
        }

        for (Direction direction : Direction.values()) {
            if (!state.getValue(PipeStraightBlock.property(direction))) continue;
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor instanceof PipeStraightBlockEntity other
                && other.transferCooldown == 0
                && worldPosition.asLong() < other.worldPosition.asLong()) {
                if (balanceWith(other)) return;
            }
        }

        for (Direction direction : Direction.values()) {
            int side = 1 << direction.ordinal();
            if (!state.getValue(PipeStraightBlock.property(direction))) continue;
            BlockPos neighborPos = worldPosition.relative(direction);
            if (level.getBlockEntity(neighborPos) instanceof PipeStraightBlockEntity) continue;
            boolean defaultMode = (inputSides & side) == 0 && (outputSides & side) == 0;
            if (((outputSides & side) != 0 || defaultMode && (automaticInputSides & side) == 0)
                && pushTo(neighborPos, direction.getOpposite()) > 0 && defaultMode) {
                automaticOutputSides |= side;
            }
        }
    }

    private void sync() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 2);
        }
    }

    private int pullFrom(BlockPos pos, Direction preferredSide) {
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, preferredSide);
        int moved = handler == null ? 0 : pullFrom(handler);
        if (moved > 0) return moved;
        handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        moved = handler == null ? 0 : pullFrom(handler);
        if (moved > 0) return moved;
        for (Direction side : Direction.values()) {
            handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
            moved = handler == null ? 0 : pullFrom(handler);
            if (moved > 0) return moved;
        }
        return 0;
    }

    private int pushTo(BlockPos pos, Direction preferredSide) {
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, preferredSide);
        int moved = handler == null ? 0 : pushTo(handler);
        if (moved > 0) return moved;
        handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        moved = handler == null ? 0 : pushTo(handler);
        if (moved > 0) return moved;
        for (Direction side : Direction.values()) {
            handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
            moved = handler == null ? 0 : pushTo(handler);
            if (moved > 0) return moved;
        }
        return 0;
    }

    private int pullFrom(IFluidHandler source) {
        FluidStack offered = source.drain(MAX_TRANSFER, IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty()) return 0;
        int accepted = fluidTank.fill(offered, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return 0;
        FluidStack drained = source.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        int moved = drained.isEmpty() ? 0 : fluidTank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (moved > 0) delayTransfer();
        return moved;
    }

    private int pushTo(IFluidHandler destination) {
        FluidStack offered = fluidTank.drain(MAX_TRANSFER, IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty()) return 0;
        int accepted = destination.fill(offered, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return 0;
        FluidStack drained = fluidTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        int moved = drained.isEmpty() ? 0 : destination.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (moved > 0) delayTransfer();
        return moved;
    }

    private boolean balanceWith(PipeStraightBlockEntity other) {
        int mine = fluidTank.getFluidAmount();
        int theirs = other.fluidTank.getFluidAmount();
        if (Math.abs(mine - theirs) < 2) return false;

        FluidTank source = mine > theirs ? fluidTank : other.fluidTank;
        FluidTank destination = mine > theirs ? other.fluidTank : fluidTank;
        FluidStack offered = source.drain(
            Math.min(MAX_TRANSFER, Math.abs(mine - theirs) / 2), IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty()) return false;
        int accepted = destination.fill(offered, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return false;
        FluidStack drained = source.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) return false;
        destination.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        delayTransfer();
        other.delayTransfer();
        return true;
    }

    private void delayTransfer() {
        transferCooldown = TRANSFER_DELAY_TICKS;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.get("fluidTank") instanceof CompoundTag tankTag) fluidTank.readFromNBT(registries, tankTag);
        inputSides = tag.getInt("configuredInputSides");
        outputSides = tag.getInt("configuredOutputSides");
        automaticInputSides = tag.contains("automaticInputSides")
            ? tag.getInt("automaticInputSides") : tag.getInt("inputSides");
        automaticOutputSides = tag.contains("automaticOutputSides")
            ? tag.getInt("automaticOutputSides") : tag.getInt("outputSides");
        transferCooldown = tag.getInt("transferCooldown");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("fluidTank", fluidTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("configuredInputSides", inputSides);
        tag.putInt("configuredOutputSides", outputSides);
        tag.putInt("automaticInputSides", automaticInputSides);
        tag.putInt("automaticOutputSides", automaticOutputSides);
        tag.putInt("transferCooldown", transferCooldown);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithFullMetadata(registries);
    }
}
