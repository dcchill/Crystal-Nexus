package net.crystalnexus.block.entity;

import net.crystalnexus.util.MachineSync;

import net.crystalnexus.block.PlasmaGeneratorControllerBlock;
import net.crystalnexus.block.HeatingCoreBlock;
import net.crystalnexus.block.PlasmaBlock;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.energy.GeneratorEnergyStorage;
import net.crystalnexus.energy.PlasmaGrid;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.core.Direction;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.crystalnexus.multiblock.StructureNbtValidator;
import net.crystalnexus.multiblock.MultiblockPortTarget;
import net.crystalnexus.world.inventory.PlasmaGeneratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class PlasmaGeneratorControllerBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider, MultiblockPortTarget, WorldlyContainer {
    public final MachineSync machineSync = new MachineSync(this);
    public static final int TANK_CAPACITY = 10_000;
    private final NonNullList<ItemStack> components = NonNullList.withSize(PlasmaGrid.SIZE, ItemStack.EMPTY);
    private final double[] heat = new double[PlasmaGrid.SIZE];
    private static final int VALIDATION_INTERVAL = 20;
    public static final double PLASMA_SPEED = 0.75D;
    public static final int PLASMA_SIZE = 16;
    private static final double PLASMA_PATH_RADIUS = 8.0D;
    private static final double PLASMA_TRAIL_SPACING = 0.16D;
    private static final ResourceLocation STRUCTURE = ResourceLocation.fromNamespaceAndPath("crystalnexus", "plasma_gen");

    private final FluidTank argonTank = new FluidTank(TANK_CAPACITY,
        stack -> stack.is(CrystalnexusModFluids.ARGON.get())) {
        @Override protected void onContentsChanged() { machineSync.changed(); }
    };
	private final GeneratorEnergyStorage energy = new GeneratorEnergyStorage(
		100_000_000, Integer.MAX_VALUE, machineSync::changed);
    private final List<BlockPos> fluidInputs = new ArrayList<>();
    private final List<BlockPos> energyOutputs = new ArrayList<>();
    private final List<BlockPos> heatingCores = new ArrayList<>();
    @Nullable private Vec3 formationCenter;
    private final List<BlockPos> plasmaPositions = new ArrayList<>();
    private boolean formed;
    private boolean operating;
    private int outputPerTick;
    private double argonRemainder;
    private String status = "Incomplete Structure";
    private String structureStatus = "Incomplete Structure";
    private int validationDelay;

    public PlasmaGeneratorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.PLASMA_GENERATOR_CONTROLLER.get(), pos, state);
    }

    public FluidTank getArgonTank() { return argonTank; }
    public boolean isFormed() { return formed; }
    public boolean isOperating() { return formed && operating; }
    public int getOutputPerTick() { return outputPerTick; }
    public String getStatus() { return status; }
    @Nullable public Vec3 getFormationCenter() { return formationCenter; }

    public boolean validateStructureNow() {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        validateStructure(serverLevel);
        validationDelay = VALIDATION_INTERVAL;
        return formed;
    }

    public void serverTick() {
        machineSync.tick();
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (validationDelay-- <= 0) {
            validateStructure(serverLevel);
            validationDelay = VALIDATION_INTERVAL;
        }
        energyOutputs.forEach(pos -> {
            if (serverLevel.getBlockEntity(pos) instanceof MachineEnergyOutputBlockEntity output) output.pushEnergy();
        });

        PlasmaGrid.Result grid = getGrid();
        int argonToDrain = (int) Math.floor(grid.argonPerTick() + argonRemainder);
        boolean injecting = formed && grid.argonPerTick() > 0
            && argonTank.getFluidAmount() >= argonToDrain && availableOutputCapacity(1) > 0;
        double[] previousHeat = heat.clone();
        boolean rupture = PlasmaGrid.advanceHeat(heat, grid, injecting);
        if (!java.util.Arrays.equals(previousHeat, heat)) machineSync.changed();
        if (rupture) { plasmaArcFailure(serverLevel, List.copyOf(heatingCores)); return; }
        if (!formed) { updateOperating(false, 0, status.equals("Plasma Arc Failure") ? status : structureStatus); return; }
        if (grid.argonPerTick() == 0) { updateOperating(false, 0, "No Plasma Injectors"); return; }
        if (argonTank.getFluidAmount() < argonToDrain) { updateOperating(false, 0, "Waiting for Argon"); return; }
        if (!injecting) { updateOperating(false, 0, "Energy Output Full"); return; }

        argonRemainder = grid.argonPerTick() + argonRemainder - argonToDrain;
        machineSync.changed();
        argonTank.drain(argonToDrain, IFluidHandler.FluidAction.EXECUTE);
        int generated = distributeEnergy(grid.outputPerTick());
        updateOperating(true, generated, generated > 0 ? "Generating" : "No Plasma Extraction");
    }

    private void validateStructure(ServerLevel level) {
        StructureNbtValidator.ValidationResult validation = StructureNbtValidator.validateDetailed(level, STRUCTURE, worldPosition,
            getBlockState().getValue(PlasmaGeneratorControllerBlock.FACING),
            CrystalnexusModBlocks.PLASMA_GENERATOR_CONTROLLER.get(), PlasmaGeneratorControllerBlock.FACING,
            Map.of(CrystalnexusModBlocks.TITANIUM_CARBIDE_BLOCK.get(), Set.of(
                CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get(), CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get())),
            Set.of(CrystalnexusModBlocks.HEATING_CORE.get()), true, false, Map.of(
                CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get(), 1,
                CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get(), 2), true);
        Optional<StructureNbtValidator.Match> match = validation.match();
        structureStatus = validation.message();
        List<BlockPos> substitutions = match.map(StructureNbtValidator.Match::substitutionPositions).orElse(List.of());
        List<BlockPos> previousHeatingCores = List.copyOf(heatingCores);
        List<BlockPos> nextInputs = substitutions.stream()
            .filter(pos -> level.getBlockState(pos).is(CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get())).toList();
        List<BlockPos> nextOutputs = substitutions.stream()
            .filter(pos -> level.getBlockState(pos).is(CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get())).toList();
        boolean nextFormed = match.isPresent() && !nextInputs.isEmpty()
            && nextInputs.size() + nextOutputs.size() == substitutions.size();
        heatingCores.clear();
        match.ifPresent(found -> heatingCores.addAll(found.positionsFor(CrystalnexusModBlocks.HEATING_CORE.get())));
        if (formed && operating && !nextFormed) {
            plasmaArcFailure(level, previousHeatingCores);
            return;
        }

        for (BlockPos old : List.copyOf(fluidInputs)) if (!nextInputs.contains(old)
            && level.getBlockEntity(old) instanceof MachineFluidInputBlockEntity input) input.unbindController(worldPosition);
        fluidInputs.clear();
        for (BlockPos pos : nextInputs) if (level.getBlockEntity(pos) instanceof MachineFluidInputBlockEntity input) {
            input.bindController(worldPosition);
            fluidInputs.add(pos);
        }
        for (BlockPos old : List.copyOf(energyOutputs)) if (!nextOutputs.contains(old)
            && level.getBlockEntity(old) instanceof MachineEnergyOutputBlockEntity output) output.unbindController(worldPosition);
        energyOutputs.clear();
        for (BlockPos pos : nextOutputs) if (level.getBlockEntity(pos) instanceof MachineEnergyOutputBlockEntity output) {
            output.bindController(worldPosition);
            energyOutputs.add(pos);
        }

        if (!nextFormed) setHeatingCoresActive(previousHeatingCores, false);
        Vec3 nextCenter = nextFormed ? match.orElseThrow().center() : null;
        if (formed != nextFormed || !Objects.equals(formationCenter, nextCenter)) {
            formed = nextFormed;
            formationCenter = nextCenter;
            sync();
        } else formed = nextFormed;
    }

	@Override public IFluidHandler multiblockFluidInput() { return argonTank; }

    private int availableOutputCapacity(int requested) {
		return energy.generateEnergy(requested, true);
    }

    private int distributeEnergy(int requested) {
		return energy.generateEnergy(requested, false);
    }

	@Override public IEnergyStorage multiblockEnergyOutput() { return energy; }

    private void updateOperating(boolean nextOperating, int nextOutput, String nextStatus) {
        setHeatingCoresActive(heatingCores, nextOperating);
        if (level instanceof ServerLevel serverLevel) updatePlasmaBlocks(serverLevel, nextOperating);
        if (operating == nextOperating && outputPerTick == nextOutput && status.equals(nextStatus)) return;
        operating = nextOperating;
        outputPerTick = nextOutput;
        status = nextStatus;
        sync();
    }

    private void updatePlasmaBlocks(ServerLevel level, boolean active) {
        if (!active || formationCenter == null) {
            clearPlasmaBlocks(level);
            return;
        }
        double headAngle = level.getGameTime() * PLASMA_SPEED;
        List<BlockPos> nextPositions = new ArrayList<>(PLASMA_SIZE);
        for (int index = 0; index < PLASMA_SIZE; index++) {
            double angle = headAngle - index * PLASMA_TRAIL_SPACING;
            BlockPos next = BlockPos.containing(
                formationCenter.x + Math.cos(angle) * PLASMA_PATH_RADIUS,
                formationCenter.y,
                formationCenter.z + Math.sin(angle) * PLASMA_PATH_RADIUS);
            if (nextPositions.contains(next)) continue;
            BlockState state = level.getBlockState(next);
            if (plasmaPositions.contains(next) && state.is(CrystalnexusModBlocks.PLASMA_BLOCK.get())) {
                nextPositions.add(next);
            } else if (state.isAir()
                    && level.setBlockAndUpdate(next, CrystalnexusModBlocks.PLASMA_BLOCK.get().defaultBlockState().setValue(PlasmaBlock.CONTAINED, true))) {
                nextPositions.add(next);
            }
        }
        for (BlockPos previous : plasmaPositions) {
            if (!nextPositions.contains(previous)
                    && level.getBlockState(previous).is(CrystalnexusModBlocks.PLASMA_BLOCK.get()))
                level.removeBlock(previous, false);
        }
        if (!plasmaPositions.equals(nextPositions)) {
            plasmaPositions.clear();
            plasmaPositions.addAll(nextPositions);
            setChanged();
        }
    }

    private void clearPlasmaBlocks(ServerLevel level) {
        if (plasmaPositions.isEmpty()) return;
        for (BlockPos pos : plasmaPositions) {
            if (level.getBlockState(pos).is(CrystalnexusModBlocks.PLASMA_BLOCK.get()))
                level.removeBlock(pos, false);
        }
        plasmaPositions.clear();
        setChanged();
    }

    private void releasePlasmaBlocks(ServerLevel level) {
        for (BlockPos pos : List.copyOf(plasmaPositions)) PlasmaBlock.release(level, pos);
        plasmaPositions.clear();
        setChanged();
    }

    private void setHeatingCoresActive(List<BlockPos> cores, boolean active) {
        if (level == null) return;
        for (BlockPos pos : cores) {
            BlockState state = level.getBlockState(pos);
            if (state.is(CrystalnexusModBlocks.HEATING_CORE.get()) && state.getValue(HeatingCoreBlock.LIT) != active)
                level.setBlock(pos, state.setValue(HeatingCoreBlock.LIT, active), 3);
        }
    }

    private void plasmaArcFailure(ServerLevel level, List<BlockPos> cores) {
        Vec3 center = formationCenter == null ? Vec3.atCenterOf(worldPosition) : formationCenter;
        releasePlasmaBlocks(level);
        setHeatingCoresActive(cores, false);
        cores.stream().filter(pos -> level.getBlockState(pos).is(CrystalnexusModBlocks.HEATING_CORE.get()))
            .forEach(pos -> level.destroyBlock(pos, false));
        argonTank.drain(argonTank.getFluidAmount(), IFluidHandler.FluidAction.EXECUTE);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 160, 3.0, 2.0, 3.0, 0.35);
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 8, 1.0, 1.0, 1.0, 0);
        level.playSound(null, BlockPos.containing(center), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 5.0F, 1.4F);
        level.explode(null, center.x, center.y, center.z, 4.5F, Level.ExplosionInteraction.NONE);
        shutDown();
        java.util.Arrays.fill(heat, 0);
        status = "Plasma Arc Failure";
        sync();
    }

    public void onControllerRemoved() {
        if (level != null && !level.isClientSide) {
            Containers.dropContents(level, worldPosition, this);
            clearContent();
        }
        if (operating && level instanceof ServerLevel serverLevel) {
            plasmaArcFailure(serverLevel, List.copyOf(heatingCores));
            return;
        }
        shutDown();
    }

    private void shutDown() {
        setHeatingCoresActive(heatingCores, false);
        if (level != null) {
            if (level instanceof ServerLevel serverLevel) clearPlasmaBlocks(serverLevel);
            for (BlockPos pos : fluidInputs) if (level.getBlockEntity(pos) instanceof MachineFluidInputBlockEntity input)
                input.unbindController(worldPosition);
            for (BlockPos pos : energyOutputs) if (level.getBlockEntity(pos) instanceof MachineEnergyOutputBlockEntity output)
                output.unbindController(worldPosition);
        }
        fluidInputs.clear();
        energyOutputs.clear();
        heatingCores.clear();
        formed = false;
        operating = false;
        outputPerTick = 0;
        formationCenter = null;
    }

    public static int componentType(ItemStack stack) {
        if (stack.is(CrystalnexusModItems.PLASMA_INJECTOR.get())) return PlasmaGrid.INJECTOR;
        if (stack.is(CrystalnexusModItems.HIGH_FLOW_PLASMA_INJECTOR.get())) return PlasmaGrid.HIGH_FLOW_INJECTOR;
        if (stack.is(CrystalnexusModItems.FERROSTEEL_HEATSINK.get())) return PlasmaGrid.HEATSINK;
        if (stack.is(CrystalnexusModItems.INDUCTION_COIL.get())) return PlasmaGrid.COIL;
        return PlasmaGrid.EMPTY;
    }

    public PlasmaGrid.Result getGrid() {
        int[] grid = new int[PlasmaGrid.SIZE];
        for (int slot = 0; slot < grid.length; slot++) grid[slot] = componentType(components.get(slot));
        return PlasmaGrid.calculate(grid);
    }
    public double getHeat(int slot) { return heat[slot]; }
    public int getHottestSlot() {
        int hottest = 0;
        for (int slot = 1; slot < heat.length; slot++) if (heat[slot] > heat[hottest]) hottest = slot;
        return hottest;
    }
    public double getHottestHeat() { return heat[getHottestSlot()]; }
    @Override public int getContainerSize() { return PlasmaGrid.SIZE; }
    @Override public int getMaxStackSize() { return 1; }
    @Override public int[] getSlotsForFace(Direction side) { return new int[0]; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override public boolean isEmpty() { return components.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return components.get(slot); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return componentType(stack) != PlasmaGrid.EMPTY; }
    @Override public void setItem(int slot, ItemStack stack) {
        if (!stack.isEmpty() && !canPlaceItem(slot, stack)) return;
        components.set(slot, stack.copyWithCount(Math.min(1, stack.getCount())));
        machineSync.changed();
    }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(components, slot, amount);
        if (!removed.isEmpty()) machineSync.changed();
        return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(components, slot); }
    @Override public void clearContent() { components.clear(); machineSync.changed(); }
    @Override public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
            && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
    }

    @Override public Component getDisplayName() { return Component.translatable("block.crystalnexus.plasma_generator_controller"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PlasmaGeneratorMenu(id, inventory, this);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        components.clear();
        ContainerHelper.loadAllItems(tag, components, registries);
        for (int slot = 0; slot < PlasmaGrid.SIZE; slot++) {
            ItemStack stack = components.get(slot);
            if (!stack.isEmpty() && !canPlaceItem(slot, stack)) components.set(slot, ItemStack.EMPTY);
            else stack.setCount(Math.min(1, stack.getCount()));
            double value = tag.getDouble("heat" + slot);
            heat[slot] = Double.isFinite(value) ? Math.max(0, Math.min(PlasmaGrid.HEAT_LIMIT, value)) : 0;
        }
        if (tag.get("argon") instanceof CompoundTag fluid) argonTank.readFromNBT(registries, fluid);
		if (tag.get("energy") instanceof IntTag stored) energy.deserializeNBT(registries, stored);
        formed = tag.getBoolean("formed");
        operating = tag.getBoolean("operating");
        outputPerTick = tag.getInt("outputPerTick");
        argonRemainder = tag.getDouble("argonRemainder") == 0.5 ? 0.5 : 0;
        status = tag.contains("status", Tag.TAG_STRING) ? tag.getString("status") : "Incomplete Structure";
        formationCenter = tag.contains("formationX", Tag.TAG_DOUBLE)
            ? new Vec3(tag.getDouble("formationX"), tag.getDouble("formationY"), tag.getDouble("formationZ")) : null;
        plasmaPositions.clear();
        if (tag.contains("plasmaPositions", Tag.TAG_LONG_ARRAY)) {
            for (long packedPos : tag.getLongArray("plasmaPositions")) plasmaPositions.add(BlockPos.of(packedPos));
        } else if (tag.contains("plasmaX", Tag.TAG_INT)) {
            plasmaPositions.add(new BlockPos(tag.getInt("plasmaX"), tag.getInt("plasmaY"), tag.getInt("plasmaZ")));
        }
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, components, registries);
        for (int slot = 0; slot < heat.length; slot++) tag.putDouble("heat" + slot, heat[slot]);
        tag.put("argon", argonTank.writeToNBT(registries, new CompoundTag()));
		tag.put("energy", energy.serializeNBT(registries));
        tag.putBoolean("formed", formed);
        tag.putBoolean("operating", operating);
        tag.putInt("outputPerTick", outputPerTick);
        tag.putDouble("argonRemainder", argonRemainder);
        tag.putString("status", status);
        if (formationCenter != null) {
            tag.putDouble("formationX", formationCenter.x);
            tag.putDouble("formationY", formationCenter.y);
            tag.putDouble("formationZ", formationCenter.z);
        }
        tag.putLongArray("plasmaPositions", plasmaPositions.stream().mapToLong(BlockPos::asLong).toArray());
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithFullMetadata(registries); }
}
