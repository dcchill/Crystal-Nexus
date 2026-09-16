package net.crystalnexus.block.entity;

import net.crystalnexus.energy.GeneratorEnergyStorage;
import net.crystalnexus.block.EngineeredHeartBlock;
import net.crystalnexus.multiblock.EngineeredHeartStructure;
import net.crystalnexus.multiblock.MultiblockPortTarget;
import net.crystalnexus.util.HeartCycle;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.world.inventory.EngineeredHeartMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public final class EngineeredHeartBlockEntity extends BlockEntity implements MenuProvider, MultiblockPortTarget {
    public static final int BLOOD_TANK_CAPACITY = 4_000;
    public static final int ENERGY_CAPACITY = 10_240_000;
    public static final int BLOOD_PER_BEAT = 200;
    public static final int FE_PER_BEAT = 1_024_000;
    private final HeartCycle cycle = new HeartCycle();
    private boolean formed;
    private Direction boundFacing;
    private BlockPos inputPos, outputPos, heartPos;
    private String status = "Incomplete Structure";

    private final FluidTank bloodTank = new FluidTank(BLOOD_TANK_CAPACITY,
        stack -> stack.is(CrystalnexusModFluids.BLOOD.get())) {
        @Override protected void onContentsChanged() { sync(); }
    };
    private final GeneratorEnergyStorage energy = new GeneratorEnergyStorage(ENERGY_CAPACITY, ENERGY_CAPACITY, this::sync);

    public EngineeredHeartBlockEntity(BlockPos pos, BlockState state) { super(CrystalnexusModBlockEntities.ENGINEERED_HEART.get(), pos, state); }
    public FluidTank getBloodTank() { return bloodTank; }
    public IEnergyStorage getEnergyStorage() { return energy; }
    public boolean isFormed() { return formed; }
    public String getStatus() { return status; }
    private Direction facing() { return getBlockState().getValue(EngineeredHeartBlock.FACING); }
    private boolean portsAvailable() {
        return !isRemoved() && formed && level != null && EngineeredHeartStructure.matches(level, worldPosition, facing());
    }
    @Override public IFluidHandler multiblockFluidInput() { return portsAvailable() ? bloodTank : null; }
    @Override public IEnergyStorage multiblockEnergyOutput() { return portsAvailable() ? energy : null; }
    @Override public boolean acceptsMultiblockPort(BlockPos pos) { return formed && (pos.equals(inputPos) || pos.equals(outputPos)); }

    public void unbindStructure() {
        formed = false;
        cycle.reset();
        if (level != null) {
            if (inputPos != null && level.hasChunkAt(inputPos) && level.getBlockEntity(inputPos) instanceof MachineFluidInputBlockEntity input) input.unbindController(worldPosition);
            if (outputPos != null && level.hasChunkAt(outputPos) && level.getBlockEntity(outputPos) instanceof MachineEnergyOutputBlockEntity output) output.unbindController(worldPosition);
            if (heartPos != null && level.hasChunkAt(heartPos) && level.getBlockEntity(heartPos) instanceof HeartBlockEntity heart) heart.unbindController(worldPosition);
        }
        inputPos = outputPos = heartPos = null;
        boundFacing = null;
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;
        boolean valid = EngineeredHeartStructure.matches(level, worldPosition, facing());
        if (!valid || boundFacing != facing()) unbindStructure();
        if (!valid) { updateStatus("Incomplete Structure"); return; }
        heartPos = EngineeredHeartStructure.center(worldPosition, facing());
        inputPos = heartPos.offset(EngineeredHeartStructure.rotate(new BlockPos(-2, -2, 0), facing()));
        outputPos = heartPos.offset(EngineeredHeartStructure.rotate(new BlockPos(2, -2, 0), facing()));
        if (!(level.getBlockEntity(inputPos) instanceof MachineFluidInputBlockEntity input)
            || !(level.getBlockEntity(outputPos) instanceof MachineEnergyOutputBlockEntity output)
            || !(level.getBlockEntity(heartPos) instanceof HeartBlockEntity heart)) {
            unbindStructure();
            updateStatus("Incomplete Structure");
            return;
        }
        formed = true;
        boundFacing = facing();
        input.bindController(worldPosition);
        output.bindController(worldPosition);
        heart.bindController(worldPosition, facing());
        output.pushEnergy();
        boolean bloodReady = bloodTank.getFluidAmount() >= BLOOD_PER_BEAT;
        boolean energyReady = energy.generateEnergy(FE_PER_BEAT, true) == FE_PER_BEAT;
        updateStatus(!bloodReady ? "Insufficient Blood" : !energyReady ? "Energy Full" : "Beating");
        if (cycle.tick(bloodReady && energyReady)) {
            bloodTank.drain(BLOOD_PER_BEAT, IFluidHandler.FluidAction.EXECUTE);
            energy.generateEnergy(FE_PER_BEAT, false);
            heart.beat();
            output.pushEnergy();
        }
    }

    private void updateStatus(String next) {
        boolean lit = "Beating".equals(next);
        BlockState state = getBlockState();
        if (level != null && state.getValue(EngineeredHeartBlock.LIT) != lit)
            level.setBlock(worldPosition, state.setValue(EngineeredHeartBlock.LIT, lit), 3);
        if (!status.equals(next)) { status = next; sync(); }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EngineeredHeartBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    @Override public Component getDisplayName() { return Component.translatable("block.crystalnexus.engineered_heart"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new EngineeredHeartMenu(id, inventory, this); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.get("blood") instanceof CompoundTag blood) bloodTank.readFromNBT(registries, blood);
        if (tag.get("energy") instanceof IntTag stored) energy.deserializeNBT(registries, stored);
        // Formation and the cycle are revalidated after a world load; never replay offline beats.
        formed = tag.getBoolean("formed");
        status = tag.contains("status") ? tag.getString("status") : "Incomplete Structure";
        cycle.reset();
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("blood", bloodTank.writeToNBT(registries, new CompoundTag()));
        tag.put("energy", energy.serializeNBT(registries));
    }
    private void sync() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveWithFullMetadata(registries);
        tag.putBoolean("formed", formed);
        tag.putString("status", status);
        return tag;
    }
}
