package net.crystalnexus.block.entity;

import net.crystalnexus.block.ChemicalReactionChamberBlock;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.multiblock.MultiblockPortTarget;
import net.crystalnexus.multiblock.StructureNbtValidator;
import net.crystalnexus.procedures.FluidChemicalReactionChamberOnTickUpdateProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class MegaChemicalReactionChamberBlockEntity extends FluidChemicalReactionChamberBlockEntity implements MultiblockPortTarget {
    public static final int TANK_CAPACITY = FluidChemicalReactionChamberBlockEntity.TANK_CAPACITY * 4;
    private static final int VALIDATION_INTERVAL = 20;
    private static final ResourceLocation STRUCTURE = ResourceLocation.fromNamespaceAndPath("crystalnexus", "mega_chem_reactor");
    private final List<BlockPos> itemInputs = new ArrayList<>();
    private final List<BlockPos> itemOutputs = new ArrayList<>();
    private final List<BlockPos> fluidInputs = new ArrayList<>();
    private final List<BlockPos> fluidOutputs = new ArrayList<>();
    private final List<BlockPos> energyInputs = new ArrayList<>();
    private boolean formed;
    private String structureError = "Mega Chemical Reaction Chamber structure is incomplete.";
    private int validationDelay;

    public MegaChemicalReactionChamberBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.MEGA_CHEMICAL_REACTION_CHAMBER.get(), pos, state, TANK_CAPACITY);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MegaChemicalReactionChamberBlockEntity chamber) {
        if (level instanceof ServerLevel serverLevel) chamber.serverTick(serverLevel);
    }

    private void serverTick(ServerLevel level) {
        if (!formed || validationDelay-- <= 0) {
            validateStructure(level);
            validationDelay = VALIDATION_INTERVAL;
        }
        if (!formed) {
            getPersistentData().putDouble("progress", 0);
            setActive(level, false);
            return;
        }
        pullItemInputs();
        flushItemOutput();
        FluidChemicalReactionChamberOnTickUpdateProcedure.execute(level, worldPosition);
        flushItemOutput();
    }

    public boolean validateStructureNow() {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        validateStructure(serverLevel);
        validationDelay = VALIDATION_INTERVAL;
        return formed;
    }

    public boolean isFormed() { return formed; }
    public String getStructureError() { return structureError; }

    private void validateStructure(ServerLevel level) {
        Optional<StructureNbtValidator.Match> match = StructureNbtValidator.validate(level, STRUCTURE, worldPosition,
            getBlockState().getValue(ChemicalReactionChamberBlock.FACING), CrystalnexusModBlocks.MEGA_CHEMICAL_REACTION_CHAMBER.get(),
            ChemicalReactionChamberBlock.FACING, Map.of(CrystalnexusModBlocks.TEMPERED_AZURINE_CASING.get(), Set.of(
                CrystalnexusModBlocks.MULTIBLOCK_ITEM_INPUT.get(), CrystalnexusModBlocks.MULTIBLOCK_ITEM_OUTPUT.get(),
                CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get(), CrystalnexusModBlocks.MULTIBLOCK_FLUID_OUTPUT.get(),
                CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get())),
            Set.of(CrystalnexusModBlocks.MEGA_CHEMICAL_REACTION_CHAMBER.get()), true, false);
        List<BlockPos> substitutions = match.map(StructureNbtValidator.Match::substitutionPositions).orElse(List.of());
        itemInputs.clear();
        itemInputs.addAll(positions(level, substitutions, CrystalnexusModBlocks.MULTIBLOCK_ITEM_INPUT.get(), MultiblockItemInputBlockEntity.class));
        itemOutputs.clear();
        itemOutputs.addAll(positions(level, substitutions, CrystalnexusModBlocks.MULTIBLOCK_ITEM_OUTPUT.get(), MultiblockItemOutputBlockEntity.class));
        updateFluidInputs(level, positions(level, substitutions, CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get(), MachineFluidInputBlockEntity.class));
        updateFluidOutputs(level, positions(level, substitutions, CrystalnexusModBlocks.MULTIBLOCK_FLUID_OUTPUT.get(), MultiblockFluidOutputBlockEntity.class));
        updateEnergyInputs(level, positions(level, substitutions, CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get(), MachineEnergyInputBlockEntity.class));
        boolean hasInput = !itemInputs.isEmpty() || !fluidInputs.isEmpty();
        boolean hasOutput = !itemOutputs.isEmpty() || !fluidOutputs.isEmpty();
        boolean hasEnergyInput = !energyInputs.isEmpty();
        boolean next = match.isPresent() && hasInput && hasOutput && hasEnergyInput;
        if (next) structureError = "";
        else if (match.isEmpty()) structureError = "Mega Chemical Reaction Chamber structure is incomplete or has misplaced blocks.";
        else {
            List<String> missing = new ArrayList<>();
            if (!hasInput) missing.add("at least one item or fluid input");
            if (!hasOutput) missing.add("at least one item or fluid output");
            if (!hasEnergyInput) missing.add("an energy input");
            structureError = "Mega Chemical Reaction Chamber needs " + String.join(", ", missing) + ".";
        }
        if (formed != next) {
            formed = next;
            for (BlockPos pos : itemInputs) level.invalidateCapabilities(pos);
            for (BlockPos pos : itemOutputs) level.invalidateCapabilities(pos);
            for (BlockPos pos : fluidInputs) level.invalidateCapabilities(pos);
            for (BlockPos pos : fluidOutputs) level.invalidateCapabilities(pos);
            for (BlockPos pos : energyInputs) level.invalidateCapabilities(pos);
            sync();
        } else formed = next;
    }

    private static <T> List<BlockPos> positions(ServerLevel level, List<BlockPos> substitutions, Block block, Class<T> entityType) {
        return substitutions.stream().filter(pos -> level.getBlockState(pos).is(block) && entityType.isInstance(level.getBlockEntity(pos))).toList();
    }

    private void updateFluidInputs(ServerLevel level, List<BlockPos> next) {
        for (BlockPos old : List.copyOf(fluidInputs)) if (!next.contains(old)
            && level.getBlockEntity(old) instanceof MachineFluidInputBlockEntity input) input.unbindController(worldPosition);
        fluidInputs.clear();
        for (BlockPos pos : next) if (level.getBlockEntity(pos) instanceof MachineFluidInputBlockEntity input) {
            input.bindController(worldPosition); fluidInputs.add(pos);
        }
    }

    private void updateFluidOutputs(ServerLevel level, List<BlockPos> next) {
        for (BlockPos old : List.copyOf(fluidOutputs)) if (!next.contains(old)
            && level.getBlockEntity(old) instanceof MultiblockFluidOutputBlockEntity output) output.unbindController(worldPosition);
        fluidOutputs.clear();
        for (BlockPos pos : next) if (level.getBlockEntity(pos) instanceof MultiblockFluidOutputBlockEntity output) {
            output.bindController(worldPosition); fluidOutputs.add(pos);
        }
    }

    private void updateEnergyInputs(ServerLevel level, List<BlockPos> next) {
        for (BlockPos old : List.copyOf(energyInputs)) if (!next.contains(old)
            && level.getBlockEntity(old) instanceof MachineEnergyInputBlockEntity input) input.unbindController(worldPosition);
        energyInputs.clear();
        for (BlockPos pos : next) if (level.getBlockEntity(pos) instanceof MachineEnergyInputBlockEntity input) {
            input.bindController(worldPosition); energyInputs.add(pos);
        }
    }

    private void pullItemInputs() {
        for (BlockPos pos : itemInputs) {
            if (!(level.getBlockEntity(pos) instanceof MultiblockItemInputBlockEntity input)) continue;
            for (int sourceSlot = 0; sourceSlot < input.getContainerSize(); sourceSlot++) {
                ItemStack source = input.getItem(sourceSlot);
                for (int targetSlot = 0; targetSlot < 2 && !source.isEmpty(); targetSlot++) {
                    ItemStack target = getItem(targetSlot);
                    if (!target.isEmpty() && !ItemStack.isSameItemSameComponents(target, source)) continue;
                    int moved = Math.min(source.getCount(), target.isEmpty() ? source.getMaxStackSize() : target.getMaxStackSize() - target.getCount());
                    if (moved <= 0) continue;
                    if (target.isEmpty()) setItem(targetSlot, source.copyWithCount(moved)); else target.grow(moved);
                    source.shrink(moved);
                }
            }
            input.setChanged();
        }
    }

    private void flushItemOutput() {
        ItemStack output = getItem(2);
        for (BlockPos pos : itemOutputs) {
            if (output.isEmpty()) break;
            if (level.getBlockEntity(pos) instanceof MultiblockItemOutputBlockEntity port && port.insert(output, true)) {
                port.insert(output, false);
                setItem(2, ItemStack.EMPTY);
            }
        }
    }

    private void setActive(ServerLevel level, boolean active) {
        BlockState state = level.getBlockState(worldPosition);
        int value = active ? 2 : 1;
        if (state.hasProperty(ChemicalReactionChamberBlock.BLOCKSTATE) && state.getValue(ChemicalReactionChamberBlock.BLOCKSTATE) != value)
            level.setBlock(worldPosition, state.setValue(ChemicalReactionChamberBlock.BLOCKSTATE, value), 3);
    }

    @Override public boolean acceptsMultiblockPort(BlockPos pos) {
        return formed && (fluidInputs.contains(pos) || fluidOutputs.contains(pos) || energyInputs.contains(pos));
    }
    @Override public IFluidHandler multiblockFluidInput() { return fluidInput; }
    @Override public IFluidHandler multiblockFluidOutput() { return fluidOutput; }
    @Override public IEnergyStorage multiblockEnergyInput() { return getEnergyStorage(); }
    @Override public int getSpeedMultiplier() { return 4; }

    public void onControllerRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            updateFluidInputs(serverLevel, List.of());
            updateFluidOutputs(serverLevel, List.of());
            updateEnergyInputs(serverLevel, List.of());
        }
        itemInputs.clear(); itemOutputs.clear(); formed = false;
    }

    private final IFluidHandler fluidInput = new IFluidHandler() {
        @Override public int getTanks() { return 2; }
        @Override public FluidStack getFluidInTank(int tank) { return getTank(tank).getFluid(); }
        @Override public int getTankCapacity(int tank) { return getTank(tank).getCapacity(); }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return true; }
        @Override public int fill(FluidStack fluid, FluidAction action) {
            for (int tank = 0; tank < 2; tank++) if (FluidStack.isSameFluidSameComponents(getTank(tank).getFluid(), fluid)) return getTank(tank).fill(fluid, action);
            for (int tank = 0; tank < 2; tank++) if (getTank(tank).isEmpty()) return getTank(tank).fill(fluid, action);
            return 0;
        }
        @Override public FluidStack drain(FluidStack fluid, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int amount, FluidAction action) { return FluidStack.EMPTY; }
    };
    private final IFluidHandler fluidOutput = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return tank == 0 ? getTank(2).getFluid() : FluidStack.EMPTY; }
        @Override public int getTankCapacity(int tank) { return tank == 0 ? getTank(2).getCapacity() : 0; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return false; }
        @Override public int fill(FluidStack fluid, FluidAction action) { return 0; }
        @Override public FluidStack drain(FluidStack fluid, FluidAction action) { return getTank(2).drain(fluid, action); }
        @Override public FluidStack drain(int amount, FluidAction action) { return getTank(2).drain(amount, action); }
    };

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }
}
