package net.crystalnexus.block.entity;

import net.crystalnexus.block.PipeStraightBlock;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModBlocks;
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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PipeStraightBlockEntity extends BlockEntity {
    public static final int MAX_TRANSFER = 5_000;
    public static final int COPPER_MAX_TRANSFER = MAX_TRANSFER / 4;
    public static final int TRANSFER_TICKS_PER_PIPE = 1;
    private static final int DISPLAY_TICKS = TRANSFER_TICKS_PER_PIPE + 2;

    private int inputSides;
    private int outputSides;
    private int automaticInputSides;
    private int automaticOutputSides;
    private FluidStack displayFluid = FluidStack.EMPTY;
    private int displayTicks;
    private FluidStack legacyFluid = FluidStack.EMPTY;
    private PendingTransfer pendingTransfer;
    private List<BlockPos> latchedPath = List.of();
    private BlockPos latchedSourcePos;
    private Direction latchedSourceSide;
    private FluidStack latchedFluid = FluidStack.EMPTY;
    private int latchedGraceTicks;

    public PipeStraightBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.PIPE_STRAIGHT.get(), pos, state);
    }

    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        return new NetworkFluidHandler(side);
    }

    public FluidStack getDisplayFluid() {
        return displayFluid;
    }

    private int maxTransfer() {
        return getBlockState().is(CrystalnexusModBlocks.COPPER_FLUID_PIPE.get())
            ? COPPER_MAX_TRANSFER : MAX_TRANSFER;
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
        maintainLatchedPath();
        tickDisplay();
        if (pendingTransfer != null) {
            advanceTransfer();
            return;
        }
        if (!legacyFluid.isEmpty()) moveLegacyFluid();
        for (Direction direction : Direction.values()) {
            if (!connected(direction)) {
                automaticInputSides &= ~(1 << direction.ordinal());
                automaticOutputSides &= ~(1 << direction.ordinal());
                continue;
            }
            if (!canPull(direction)) continue;
            BlockPos endpointPos = worldPosition.relative(direction);
            if (level.getBlockEntity(endpointPos) instanceof PipeStraightBlockEntity) continue;
            IFluidHandler source = handlerAt(endpointPos, direction.getOpposite());
            if (source == null) continue;

            FluidStack offered = source.drain(maxTransfer(), IFluidHandler.FluidAction.SIMULATE);
            if (offered.isEmpty()) continue;
            if (beginTransfer(endpointPos, direction, offered)) return;
        }
    }

    private boolean beginTransfer(BlockPos sourcePos, Direction sourceSide, FluidStack offered) {
        for (Endpoint endpoint : endpoints(sourceSide)) {
            if (endpoint.pos.equals(sourcePos) || !endpoint.pipe.canPush(endpoint.side)) continue;
            FluidStack candidate = offered.copy();
            candidate.setAmount(Math.min(Math.min(offered.getAmount(), maxTransfer()), endpoint.limit));
            int accepted = endpoint.handler.fill(candidate, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) continue;
            candidate.setAmount(accepted);
            pendingTransfer = new PendingTransfer(sourcePos, sourceSide, endpoint.pos, endpoint.side,
                candidate, endpoint.path, 0);
            show(candidate);
            return true;
        }
        return false;
    }

    private void advanceTransfer() {
        PendingTransfer transfer = pendingTransfer;
        int progress = transfer.progress + 1;
        int visiblePipes = Math.min(transfer.path.size(),
            1 + progress / TRANSFER_TICKS_PER_PIPE);
        showPath(transfer.path.subList(0, visiblePipes), transfer.fluid);
        if (progress < transfer.path.size() * TRANSFER_TICKS_PER_PIPE) {
            pendingTransfer = transfer.withProgress(progress);
            return;
        }

        PipeStraightBlockEntity destinationPipe = level.getBlockEntity(transfer.path.getLast())
            instanceof PipeStraightBlockEntity pipe ? pipe : null;
        IFluidHandler destination = destinationPipe == null ? null
            : destinationPipe.handlerAt(transfer.destinationPos, transfer.destinationSide.getOpposite());
        if (transfer.sourcePos == null) {
            if (destination == null) {
                pendingTransfer = transfer.withProgress(progress);
                return;
            }
            int accepted = destination.fill(transfer.fluid, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) {
                pendingTransfer = transfer.withProgress(progress);
                return;
            }
            FluidStack delivered = transfer.fluid.copy();
            delivered.setAmount(accepted);
            int inserted = destination.fill(delivered, IFluidHandler.FluidAction.EXECUTE);
            if (inserted < transfer.fluid.getAmount()) {
                FluidStack remaining = transfer.fluid.copy();
                remaining.shrink(inserted);
                pendingTransfer = transfer.withFluid(remaining).withProgress(progress);
                return;
            }
            destinationPipe.setAutomaticOutput(transfer.destinationSide);
            latchPath(transfer, delivered);
        } else {
            IFluidHandler source = handlerAt(transfer.sourcePos, transfer.sourceSide.getOpposite());
            if (source != null && destination != null) {
                FluidStack offered = source.drain(transfer.fluid, IFluidHandler.FluidAction.SIMULATE);
                int accepted = offered.isEmpty() ? 0
                    : destination.fill(offered, IFluidHandler.FluidAction.SIMULATE);
                if (accepted > 0) {
                    FluidStack drained = source.drain(Math.min(accepted, transfer.fluid.getAmount()),
                        IFluidHandler.FluidAction.EXECUTE);
                    if (!drained.isEmpty() && destination.fill(drained, IFluidHandler.FluidAction.EXECUTE) > 0) {
                        if (!isInputSide(transfer.sourceSide)) setAutomaticInput(transfer.sourceSide);
                        destinationPipe.setAutomaticOutput(transfer.destinationSide);
                        latchPath(transfer, drained);
                    }
                }
            }
        }
        pendingTransfer = null;
    }

    private void moveLegacyFluid() {
        int remaining = Math.min(legacyFluid.getAmount(), maxTransfer());
        for (Endpoint endpoint : endpoints(null)) {
            if (!endpoint.pipe.canPush(endpoint.side)) continue;
            FluidStack candidate = legacyFluid.copy();
            candidate.setAmount(Math.min(remaining, endpoint.limit));
            int moved = endpoint.handler.fill(candidate, IFluidHandler.FluidAction.EXECUTE);
            if (moved <= 0) continue;
            legacyFluid.shrink(moved);
            remaining -= moved;
            endpoint.pipe.setAutomaticOutput(endpoint.side);
            showPath(endpoint.path, candidate);
            if (remaining == 0 || legacyFluid.isEmpty()) break;
        }
        setChanged();
    }

    private int fillNetwork(FluidStack resource, IFluidHandler.FluidAction action, @Nullable Direction ingress) {
        if (resource.isEmpty() || pendingTransfer != null || ingress != null && !canPull(ingress)) return 0;
        for (Endpoint endpoint : endpoints(ingress)) {
            if (!endpoint.pipe.canPush(endpoint.side)) continue;
            FluidStack candidate = resource.copy();
            candidate.setAmount(Math.min(Math.min(resource.getAmount(), maxTransfer()), endpoint.limit));
            int accepted = endpoint.handler.fill(candidate, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) continue;
            candidate.setAmount(accepted);
            if (action.execute()) {
                pendingTransfer = new PendingTransfer(null, ingress, endpoint.pos, endpoint.side,
                    candidate.copy(), endpoint.path, 0);
                latchedGraceTicks = 2;
                if (ingress != null && !isInputSide(ingress)) setAutomaticInput(ingress);
                show(candidate);
            }
            return accepted;
        }
        return 0;
    }

    private List<Endpoint> endpoints(@Nullable Direction excludedSide) {
        List<Endpoint> result = new ArrayList<>();
        if (level == null) return result;
        ArrayDeque<NetworkNode> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(new NetworkNode(worldPosition, maxTransfer(), List.of(worldPosition)));
        visited.add(worldPosition);

        while (!queue.isEmpty()) {
            NetworkNode node = queue.removeFirst();
            if (!(level.getBlockEntity(node.pos) instanceof PipeStraightBlockEntity pipe)) continue;
            for (Direction direction : Direction.values()) {
                if (!pipe.connected(direction)) continue;
                BlockPos neighborPos = node.pos.relative(direction);
                if (level.getBlockEntity(neighborPos) instanceof PipeStraightBlockEntity neighbor) {
                    if (visited.add(neighborPos)) {
                        List<BlockPos> path = new ArrayList<>(node.path);
                        path.add(neighborPos);
                        queue.addLast(new NetworkNode(neighborPos,
                            Math.min(node.limit, neighbor.maxTransfer()), List.copyOf(path)));
                    }
                    continue;
                }
                if (node.pos.equals(worldPosition) && direction == excludedSide) continue;
                IFluidHandler handler = pipe.handlerAt(neighborPos, direction.getOpposite());
                if (handler != null) result.add(new Endpoint(neighborPos, direction, pipe, handler,
                    node.limit, node.path));
            }
        }
        return result;
    }

    private boolean connected(Direction direction) {
        BlockState state = getBlockState();
        return state.hasProperty(PipeStraightBlock.property(direction))
            && state.getValue(PipeStraightBlock.property(direction));
    }

    private boolean canPull(Direction direction) {
        int side = 1 << direction.ordinal();
        return (inputSides & side) != 0
            || (outputSides & side) == 0 && (automaticOutputSides & side) == 0;
    }

    private boolean canPush(Direction direction) {
        int side = 1 << direction.ordinal();
        return (outputSides & side) != 0
            || (inputSides & side) == 0 && (automaticInputSides & side) == 0;
    }

    private void setAutomaticInput(Direction direction) {
        int side = 1 << direction.ordinal();
        automaticInputSides |= side;
        automaticOutputSides &= ~side;
        sync();
    }

    private void setAutomaticOutput(Direction direction) {
        int side = 1 << direction.ordinal();
        if ((inputSides & side) != 0 || (outputSides & side) != 0) return;
        automaticOutputSides |= side;
        automaticInputSides &= ~side;
        sync();
    }

    private void show(FluidStack fluid) {
        displayFluid = fluid.copyWithAmount(1);
        if (displayTicks >= 0) displayTicks = DISPLAY_TICKS;
        sync();
    }

    private void showPath(List<BlockPos> path, FluidStack fluid) {
        for (BlockPos pos : path) {
            if (level.getBlockEntity(pos) instanceof PipeStraightBlockEntity pipe) pipe.show(fluid);
        }
    }

    private void latchPath(PendingTransfer transfer, FluidStack fluid) {
        releaseLatchedPath();
        latchedPath = transfer.path;
        latchedSourcePos = transfer.sourcePos;
        latchedSourceSide = transfer.sourceSide;
        latchedFluid = fluid.copyWithAmount(1);
        latchedGraceTicks = 2;
        showPathPersistent(latchedPath, latchedFluid);
    }

    private void maintainLatchedPath() {
        if (latchedPath.isEmpty()) return;
        if (latchedSourcePos == null) {
            if (pendingTransfer == null && latchedGraceTicks-- <= 0) {
                releaseLatchedPath();
                return;
            }
        } else {
            IFluidHandler source = handlerAt(latchedSourcePos, latchedSourceSide.getOpposite());
            FluidStack available = source == null ? FluidStack.EMPTY
                : source.drain(1, IFluidHandler.FluidAction.SIMULATE);
            if (available.isEmpty() || !FluidStack.isSameFluidSameComponents(available, latchedFluid)) {
                releaseLatchedPath();
                return;
            }
        }
        showPathPersistent(latchedPath, latchedFluid);
    }

    private void showPathPersistent(List<BlockPos> path, FluidStack fluid) {
        for (BlockPos pos : path) {
            if (level.getBlockEntity(pos) instanceof PipeStraightBlockEntity pipe) {
                pipe.displayFluid = fluid.copyWithAmount(1);
                pipe.displayTicks = -1;
                pipe.sync();
            }
        }
    }

    private void releaseLatchedPath() {
        for (BlockPos pos : latchedPath) {
            if (level.getBlockEntity(pos) instanceof PipeStraightBlockEntity pipe && pipe.displayTicks < 0) {
                pipe.displayTicks = DISPLAY_TICKS;
            }
        }
        latchedPath = List.of();
        latchedSourcePos = null;
        latchedSourceSide = null;
        latchedFluid = FluidStack.EMPTY;
        latchedGraceTicks = 0;
    }

    private void tickDisplay() {
        if (displayFluid.isEmpty()) return;
        if (displayTicks < 0) return;
        if (displayTicks > 0) displayTicks--;
        if (displayTicks > 0) return;
        displayFluid = FluidStack.EMPTY;
        sync();
    }

    private IFluidHandler handlerAt(BlockPos pos, Direction preferredSide) {
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, preferredSide);
        if (handler != null) return handler;
        handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        if (handler != null) return handler;
        for (Direction side : Direction.values()) {
            handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
            if (handler != null) return handler;
        }
        return null;
    }

    private void sync() {
        setChanged();
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 2);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inputSides = tag.getInt("configuredInputSides");
        outputSides = tag.getInt("configuredOutputSides");
        automaticInputSides = tag.contains("automaticInputSides")
            ? tag.getInt("automaticInputSides") : tag.getInt("inputSides");
        automaticOutputSides = tag.contains("automaticOutputSides")
            ? tag.getInt("automaticOutputSides") : tag.getInt("outputSides");
        if (tag.get("displayFluid") instanceof CompoundTag fluidTag) {
            displayFluid = FluidStack.parseOptional(registries, fluidTag);
            displayTicks = tag.getInt("displayTicks");
        } else {
            displayFluid = FluidStack.EMPTY;
            displayTicks = 0;
        }
        if (tag.get("legacyFluid") instanceof CompoundTag fluidTag) {
            legacyFluid = FluidStack.parseOptional(registries, fluidTag);
        } else if (tag.get("fluidTank") instanceof CompoundTag tankTag) {
            FluidTank oldTank = new FluidTank(15_000);
            oldTank.readFromNBT(registries, tankTag);
            legacyFluid = oldTank.getFluid().copy();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("configuredInputSides", inputSides);
        tag.putInt("configuredOutputSides", outputSides);
        tag.putInt("automaticInputSides", automaticInputSides);
        tag.putInt("automaticOutputSides", automaticOutputSides);
        if (!legacyFluid.isEmpty()) tag.put("legacyFluid", legacyFluid.save(registries));
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveWithFullMetadata(registries);
        if (!displayFluid.isEmpty()) {
            tag.put("displayFluid", displayFluid.save(registries));
            tag.putInt("displayTicks", displayTicks);
        }
        return tag;
    }

    private record NetworkNode(BlockPos pos, int limit, List<BlockPos> path) {}

    private record Endpoint(BlockPos pos, Direction side, PipeStraightBlockEntity pipe,
                            IFluidHandler handler, int limit, List<BlockPos> path) {}

    private record PendingTransfer(BlockPos sourcePos, Direction sourceSide, BlockPos destinationPos,
                                   Direction destinationSide, FluidStack fluid, List<BlockPos> path,
                                   int progress) {
        private PendingTransfer withProgress(int progress) {
            return new PendingTransfer(sourcePos, sourceSide, destinationPos, destinationSide,
                fluid, path, progress);
        }

        private PendingTransfer withFluid(FluidStack fluid) {
            return new PendingTransfer(sourcePos, sourceSide, destinationPos, destinationSide,
                fluid, path, progress);
        }
    }

    private final class NetworkFluidHandler implements IFluidHandler {
        private final Direction side;

        private NetworkFluidHandler(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return fillNetwork(resource, action, side);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
