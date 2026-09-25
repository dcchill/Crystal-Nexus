package net.crystalnexus.block.entity;

import net.crystalnexus.block.GasolineGeneratorControllerBlock;
import net.crystalnexus.energy.GeneratorEnergyStorage;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.multiblock.MultiblockPortTarget;
import net.crystalnexus.world.inventory.GasolineGeneratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class GasolineGeneratorControllerBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider, MultiblockPortTarget {
    public static final int MIN_SHAFTS = 3;
    public static final int MAX_SHAFTS = 16;
    public static final int GASOLINE_FE_PER_TICK_PER_SHAFT = 2_048;
    public static final int FUEL_PER_SHAFT = 250;
    public static final int FUEL_CYCLE_TICKS = 350;
    public static final int FUEL_CAPACITY = 16_000;
    public static final int ENERGY_CAPACITY = 10_000_000;
    private static final int VALIDATION_INTERVAL = 20;
    private static final TagKey<Fluid> GASOLINE = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "gasoline"));
    private static final TagKey<Fluid> OVERFUEL = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "overfuel"));

    private final FluidTank fuelTank = new FluidTank(FUEL_CAPACITY, stack -> stack.is(GASOLINE) || stack.is(OVERFUEL)) {
        @Override protected void onContentsChanged() { sync(); }
    };
    private final GeneratorEnergyStorage energy = new GeneratorEnergyStorage(ENERGY_CAPACITY,
        Integer.MAX_VALUE, this::sync);
    private final List<BlockPos> fluidInputs = new ArrayList<>();
    private final List<BlockPos> energyOutputs = new ArrayList<>();
    private boolean formed;
    private boolean operating;
    private int shaftCount;
    private int outputPerTick;
    private int fuelPerCycle;
    private int progress;
    private int validationDelay;
    private String status = "Incomplete Structure";

    public GasolineGeneratorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.DIESEL_GENERATOR_CONTROLLER.get(), pos, state);
    }

    public FluidTank getFuelTank() { return fuelTank; }
    public boolean isFormed() { return formed; }
    public boolean isOperating() { return formed && operating; }
    public int getShaftCount() { return shaftCount; }
    public int getOutputPerTick() { return outputPerTick; }
    public int getFuelPerCycle() { return fuelPerCycle; }
    public int getEnergyStored() { return energy.getEnergyStored(); }
    public String getStatus() { return status; }
    public String getFuelName() { return fuelTank.isEmpty() ? "None" : BuiltInRegistries.FLUID.getKey(fuelTank.getFluid().getFluid()).getPath(); }

    public boolean validateStructureNow() {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        validateStructure(serverLevel);
        validationDelay = VALIDATION_INTERVAL;
        return formed;
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (validationDelay-- <= 0) {
            validateStructure(serverLevel);
            validationDelay = VALIDATION_INTERVAL;
        }
        energyOutputs.forEach(pos -> {
            if (serverLevel.getBlockEntity(pos) instanceof MachineEnergyOutputBlockEntity output) output.pushEnergy();
        });
        if (!formed) { updateOperating(false, 0, status); return; }
        if (fuelTank.getFluidAmount() < fuelPerCycle) { updateOperating(false, 0, "Waiting for Fuel"); return; }

        int requested = GASOLINE_FE_PER_TICK_PER_SHAFT * shaftCount * (fuelTank.getFluid().is(OVERFUEL) ? 4 : 1);
        if (energy.generateEnergy(requested, true) <= 0) { updateOperating(false, 0, "Energy Output Full"); return; }
        int generated = energy.generateEnergy(requested, false);
        progress++;
        if (progress >= FUEL_CYCLE_TICKS) {
            fuelTank.drain(fuelPerCycle, IFluidHandler.FluidAction.EXECUTE);
            progress = 0;
        }
        updateOperating(generated > 0, generated, generated > 0 ? "Generating" : "Energy Output Full");
    }

    private void validateStructure(ServerLevel serverLevel) {
        Direction front = getBlockState().getValue(GasolineGeneratorControllerBlock.FACING);
        Direction rear = front.getOpposite();
        int foundShafts = countDriveshafts(serverLevel, rear);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            int shafts = countDriveshafts(serverLevel, direction);
            if (shafts > foundShafts) {
                rear = direction;
                foundShafts = shafts;
            }
        }
        if (foundShafts < MIN_SHAFTS || foundShafts > MAX_SHAFTS) { invalidate("Gasoline Generator requires " + MIN_SHAFTS + "-" + MAX_SHAFTS + " driveshafts"); return; }

        List<BlockPos> nextInputs = new ArrayList<>();
        List<BlockPos> nextOutputs = new ArrayList<>();
        for (int depth = 0; depth <= foundShafts + 1; depth++) {
            BlockPos sliceCenter = worldPosition.relative(rear, depth);
            for (int y = -1; y <= 1; y++) for (int side = -1; side <= 1; side++) {
                BlockPos pos = sliceCenter.offset(rear.getClockWise().getStepX() * side, y, rear.getClockWise().getStepZ() * side);
                boolean center = y == 0 && side == 0;
                BlockState actual = serverLevel.getBlockState(pos);
                if (depth == 0 && center) {
                    if (!actual.is(CrystalnexusModBlocks.DIESEL_GENERATOR_CONTROLLER.get())
                            || actual.getValue(GasolineGeneratorControllerBlock.FACING) != front) { invalidate("Controller is misaligned"); return; }
                } else if (center && depth <= foundShafts) {
                    if (!actual.is(CrystalnexusModBlocks.DIESEL_GENERATOR_DRIVESHAFT.get())) { invalidate("Expected Gasoline Generator Driveshaft at " + pos.toShortString()); return; }
                } else if (actual.is(CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get())) {
                    nextInputs.add(pos.immutable());
                } else if (actual.is(CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get())) {
                    nextOutputs.add(pos.immutable());
                } else if (!actual.is(CrystalnexusModBlocks.INSULATED_TITANIUM_CASING.get())) {
                    invalidate("Expected Insulated Azurine Casing at " + pos.toShortString()); return;
                }
            }
        }
        if (nextInputs.size() > 4) { invalidate("Supports at most 4 machine fluid inputs"); return; }
        if (nextOutputs.size() > 2) { invalidate("Supports at most 2 machine energy outputs"); return; }

        unbindPorts();
        for (BlockPos pos : nextInputs) if (serverLevel.getBlockEntity(pos) instanceof MachineFluidInputBlockEntity input) {
            input.bindController(worldPosition);
            fluidInputs.add(pos);
        }
        for (BlockPos pos : nextOutputs) if (serverLevel.getBlockEntity(pos) instanceof MachineEnergyOutputBlockEntity output) {
            output.bindController(worldPosition);
            energyOutputs.add(pos);
        }
        formed = true;
        shaftCount = foundShafts;
        fuelPerCycle = FUEL_PER_SHAFT * foundShafts;
        if (!operating) status = "Ready";
        sync();
    }

    private int countDriveshafts(ServerLevel serverLevel, Direction direction) {
        int count = 0;
        while (count <= MAX_SHAFTS && serverLevel.getBlockState(worldPosition.relative(direction, count + 1))
                .is(CrystalnexusModBlocks.DIESEL_GENERATOR_DRIVESHAFT.get())) count++;
        return count;
    }

    private void invalidate(String message) {
        unbindPorts();
        formed = false;
        operating = false;
        shaftCount = 0;
        outputPerTick = 0;
        fuelPerCycle = 0;
        progress = 0;
        status = message;
        sync();
    }

    private void unbindPorts() {
        if (level != null) {
            for (BlockPos pos : fluidInputs) if (level.getBlockEntity(pos) instanceof MachineFluidInputBlockEntity input) input.unbindController(worldPosition);
            for (BlockPos pos : energyOutputs) if (level.getBlockEntity(pos) instanceof MachineEnergyOutputBlockEntity output) output.unbindController(worldPosition);
        }
        fluidInputs.clear();
        energyOutputs.clear();
    }

    private void updateOperating(boolean nextOperating, int nextOutput, String nextStatus) {
        if (operating == nextOperating && outputPerTick == nextOutput && status.equals(nextStatus)) return;
        operating = nextOperating;
        outputPerTick = nextOutput;
        status = nextStatus;
        sync();
    }

    @Override public boolean acceptsMultiblockPort(BlockPos pos) { return fluidInputs.contains(pos) || energyOutputs.contains(pos); }
    @Override public IFluidHandler multiblockFluidInput() { return fuelTank; }
    @Override public IEnergyStorage multiblockEnergyOutput() { return energy; }
    public void onControllerRemoved() { unbindPorts(); formed = false; operating = false; }
    @Override public Component getDisplayName() { return Component.translatable("block.crystalnexus.diesel_generator_controller"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new GasolineGeneratorMenu(id, inventory, this); }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.get("fuel") instanceof CompoundTag fuel) fuelTank.readFromNBT(registries, fuel);
        if (tag.get("energy") instanceof IntTag stored) energy.deserializeNBT(registries, stored);
        progress = tag.getInt("progress");
        formed = tag.getBoolean("formed");
        operating = tag.getBoolean("operating");
        shaftCount = tag.getInt("shaftCount");
        outputPerTick = tag.getInt("outputPerTick");
        fuelPerCycle = tag.getInt("fuelPerCycle");
        status = tag.contains("status", Tag.TAG_STRING) ? tag.getString("status") : "Incomplete Structure";
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("fuel", fuelTank.writeToNBT(registries, new CompoundTag()));
        tag.put("energy", energy.serializeNBT(registries));
        tag.putInt("progress", progress);
        tag.putBoolean("formed", formed);
        tag.putBoolean("operating", operating);
        tag.putInt("shaftCount", shaftCount);
        tag.putInt("outputPerTick", outputPerTick);
        tag.putInt("fuelPerCycle", fuelPerCycle);
        tag.putString("status", status);
    }
    private void sync() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithFullMetadata(registries); }
}
