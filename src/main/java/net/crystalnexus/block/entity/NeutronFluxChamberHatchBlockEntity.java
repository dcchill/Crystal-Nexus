package net.crystalnexus.block.entity;

import java.util.stream.IntStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.multiblock.MultiblockPortTarget;
import net.crystalnexus.multiblock.StructureNbtValidator;
import net.crystalnexus.block.NeutronFluxChamberHatchBlock;
import net.crystalnexus.item.DemonCoreItem;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.world.inventory.NeutronFluxChamberHatchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public final class NeutronFluxChamberHatchBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer, MultiblockPortTarget {
	private static final ResourceLocation STRUCTURE = ResourceLocation.fromNamespaceAndPath("crystalnexus", "neutron_flux_chamber");
	private static final int VALIDATION_INTERVAL = 20;
	public static final int FLUID_CAPACITY = 1_000;
	public static final int FLUX_PER_TICK = 1;
	public static final int ENERGY_PER_FLUX_MB = 2_048;
	private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
	private final List<BlockPos> energyInputs = new ArrayList<>();
	private final List<BlockPos> fluidOutputs = new ArrayList<>();
	private final FluidTank fluidOutput = new FluidTank(FLUID_CAPACITY) {
		@Override protected void onContentsChanged() { sync(); }
	};
	private boolean formed;
	private String structureError = "Neutron Flux Chamber structure is incomplete.";
	private int validationDelay;
	private final EnergyStorage energyStorage = new EnergyStorage(
		8_192, Integer.MAX_VALUE, Integer.MAX_VALUE, 0) {
		@Override public int receiveEnergy(int amount, boolean simulate) {
			int received = super.receiveEnergy(amount, simulate);
			if (received > 0 && !simulate) sync();
			return received;
		}
		@Override public int extractEnergy(int amount, boolean simulate) {
			int extracted = super.extractEnergy(amount, simulate);
			if (extracted > 0 && !simulate) sync();
			return extracted;
		}
		private void sync() {
			setChanged();
			if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
		}
	};

	public NeutronFluxChamberHatchBlockEntity(BlockPos pos, BlockState state) {
		super(CrystalnexusModBlockEntities.NEUTRON_FLUX_CHAMBER_HATCH.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, NeutronFluxChamberHatchBlockEntity chamber) {
		if (!(level instanceof ServerLevel serverLevel)) return;
		if (chamber.validationDelay-- <= 0) {
			chamber.validateStructure(serverLevel);
			chamber.validationDelay = VALIDATION_INTERVAL;
		}
		chamber.generateNeutronFlux();
	}

	private void generateNeutronFlux() {
		if (!formed || !DemonCoreItem.isClosed(items.get(0))) return;
		FluidStack flux = new FluidStack(CrystalnexusModFluids.NEUTRON_FLUX.get(), FLUX_PER_TICK);
		if (fluidOutput.fill(flux, IFluidHandler.FluidAction.SIMULATE) != FLUX_PER_TICK
				|| energyStorage.extractEnergy(ENERGY_PER_FLUX_MB, true) != ENERGY_PER_FLUX_MB) return;
		if (energyStorage.extractEnergy(ENERGY_PER_FLUX_MB, false) == ENERGY_PER_FLUX_MB)
			fluidOutput.fill(flux, IFluidHandler.FluidAction.EXECUTE);
	}

	public boolean validateStructureNow() {
		if (!(level instanceof ServerLevel serverLevel)) return false;
		validateStructure(serverLevel);
		validationDelay = VALIDATION_INTERVAL;
		return formed;
	}

	public boolean isFormed() { return formed; }
	public String getStructureError() { return structureError; }
	public FluidTank getFluidOutputTank() { return fluidOutput; }

	private void validateStructure(ServerLevel serverLevel) {
		List<BlockPos> previousEnergy = List.copyOf(energyInputs);
		List<BlockPos> previousFluid = List.copyOf(fluidOutputs);
		DirectionProperty facing = NeutronFluxChamberHatchBlock.FACING;
		StructureNbtValidator.ValidationResult result = StructureNbtValidator.validateDetailed(serverLevel, STRUCTURE,
			worldPosition, getBlockState().getValue(facing), CrystalnexusModBlocks.NEUTRON_FLUX_CHAMBER_HATCH.get(), facing,
			Map.of(CrystalnexusModBlocks.CARBON_BLOCK.get(), Set.of(
				CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get(), CrystalnexusModBlocks.MULTIBLOCK_FLUID_OUTPUT.get())),
			Set.of(CrystalnexusModBlocks.NEUTRON_FLUX_CHAMBER_HATCH.get()), false, false, Map.of(), true, true);
		List<BlockPos> substitutions = result.match().map(StructureNbtValidator.Match::substitutionPositions).orElse(List.of());
		List<BlockPos> nextEnergy = substitutions.stream()
			.filter(port -> serverLevel.getBlockState(port).is(CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get())
				&& serverLevel.getBlockEntity(port) instanceof MachineEnergyInputBlockEntity)
			.map(BlockPos::immutable).toList();
		List<BlockPos> nextFluid = substitutions.stream()
			.filter(port -> serverLevel.getBlockState(port).is(CrystalnexusModBlocks.MULTIBLOCK_FLUID_OUTPUT.get())
				&& serverLevel.getBlockEntity(port) instanceof MultiblockFluidOutputBlockEntity)
			.map(BlockPos::immutable).toList();

		for (BlockPos old : List.copyOf(energyInputs)) if (!nextEnergy.contains(old)
			&& serverLevel.getBlockEntity(old) instanceof MachineEnergyInputBlockEntity input) input.unbindController(worldPosition);
		for (BlockPos old : List.copyOf(fluidOutputs)) if (!nextFluid.contains(old)
			&& serverLevel.getBlockEntity(old) instanceof MultiblockFluidOutputBlockEntity output) output.unbindController(worldPosition);
		energyInputs.clear();
		fluidOutputs.clear();
		for (BlockPos port : nextEnergy) if (serverLevel.getBlockEntity(port) instanceof MachineEnergyInputBlockEntity input) {
			input.bindController(worldPosition);
			energyInputs.add(port);
		}
		for (BlockPos port : nextFluid) if (serverLevel.getBlockEntity(port) instanceof MultiblockFluidOutputBlockEntity output) {
			output.bindController(worldPosition);
			fluidOutputs.add(port);
		}

		boolean nextFormed = result.match().isPresent() && !energyInputs.isEmpty() && !fluidOutputs.isEmpty()
			&& energyInputs.size() + fluidOutputs.size() == substitutions.size();
		if (result.match().isEmpty()) structureError = result.message();
		else if (energyInputs.isEmpty() || fluidOutputs.isEmpty()) {
			List<String> missing = new ArrayList<>();
			if (energyInputs.isEmpty()) missing.add("at least one energy input");
			if (fluidOutputs.isEmpty()) missing.add("at least one fluid output");
			structureError = "Neutron Flux Chamber needs " + String.join(" and ", missing) + ".";
		} else if (energyInputs.size() + fluidOutputs.size() != substitutions.size()) {
			structureError = "Every replaced carbon block must be an energy input or fluid output.";
		} else structureError = "";

		boolean formationChanged = formed != nextFormed;
		boolean portsChanged = !previousEnergy.equals(energyInputs) || !previousFluid.equals(fluidOutputs);
		formed = nextFormed;
		if (formationChanged || portsChanged) {
			for (BlockPos port : previousEnergy) serverLevel.invalidateCapabilities(port);
			for (BlockPos port : previousFluid) serverLevel.invalidateCapabilities(port);
			for (BlockPos port : nextEnergy) serverLevel.invalidateCapabilities(port);
			for (BlockPos port : nextFluid) serverLevel.invalidateCapabilities(port);
			for (BlockPos port : substitutions) serverLevel.invalidateCapabilities(port);
			sync();
		}
	}

	@Override public EnergyStorage multiblockEnergyInput() { return energyStorage; }
	@Override public IFluidHandler multiblockFluidOutput() { return fluidOutput; }
	@Override public boolean acceptsMultiblockPort(BlockPos pos) { return formed && (energyInputs.contains(pos) || fluidOutputs.contains(pos)); }

	public void onControllerRemoved() {
		if (level != null) {
			for (BlockPos port : energyInputs) if (level.getBlockEntity(port) instanceof MachineEnergyInputBlockEntity input) input.unbindController(worldPosition);
			for (BlockPos port : fluidOutputs) if (level.getBlockEntity(port) instanceof MultiblockFluidOutputBlockEntity output) output.unbindController(worldPosition);
		}
		energyInputs.clear();
		fluidOutputs.clear();
		formed = false;
	}

	@Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
		super.loadAdditional(tag, provider);
		if (!tryLoadLootTable(tag)) items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, items, provider);
		if (tag.get("energyStorage") instanceof IntTag energy) energyStorage.deserializeNBT(provider, energy);
		if (tag.get("fluidOutput") instanceof CompoundTag fluid) fluidOutput.readFromNBT(provider, fluid);
		formed = tag.getBoolean("formed");
	}

	@Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
		super.saveAdditional(tag, provider);
		if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, items, provider);
		tag.put("energyStorage", energyStorage.serializeNBT(provider));
		tag.put("fluidOutput", fluidOutput.writeToNBT(provider, new CompoundTag()));
		tag.putBoolean("formed", formed);
	}

	@Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
	@Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) { return saveWithFullMetadata(provider); }
	@Override public int getContainerSize() { return items.size(); }
	@Override public boolean isEmpty() { return items.get(0).isEmpty(); }
	@Override public Component getDefaultName() { return Component.translatable("block.crystalnexus.neutron_flux_chamber_hatch"); }
	@Override public Component getDisplayName() { return getDefaultName(); }
	@Override public AbstractContainerMenu createMenu(int id, Inventory inventory) { return new NeutronFluxChamberHatchMenu(id, inventory, this); }
	@Override protected NonNullList<ItemStack> getItems() { return items; }
	@Override protected void setItems(NonNullList<ItemStack> stacks) { items = stacks; }
	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && DemonCoreItem.isClosed(stack); }
	@Override public int[] getSlotsForFace(Direction side) { return IntStream.range(0, getContainerSize()).toArray(); }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return true; }
	public EnergyStorage getEnergyStorage() { return energyStorage; }
	private void sync() {
		setChanged();
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
	}
}