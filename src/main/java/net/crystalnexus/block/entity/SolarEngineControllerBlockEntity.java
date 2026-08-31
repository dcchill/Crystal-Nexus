package net.crystalnexus.block.entity;

import net.crystalnexus.block.SolarEngineControllerBlock;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.item.StarDurability;
import net.crystalnexus.energy.GeneratorEnergyStorage;
import net.crystalnexus.multiblock.StructureNbtValidator;
import net.crystalnexus.multiblock.MultiblockPortTarget;
import net.crystalnexus.world.inventory.SolarEngineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class SolarEngineControllerBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer, MultiblockPortTarget {
	public static final int TANK_CAPACITY = 1_000_000;
	public static final int MAX_HEAT = 50_000;
	public static final int MAX_CONTAINMENT_STRESS = 10_000;
	private static final int VALIDATION_INTERVAL = 20;
	private static final ResourceLocation STRUCTURE = ResourceLocation.fromNamespaceAndPath("crystalnexus", "solar_engine");
	private static final int STAR_SLOT = 0;

	private record StarProfile(int baseFePerTick, int heat, int coolant, int stress) {}

	private NonNullList<ItemStack> stacks = NonNullList.withSize(1, ItemStack.EMPTY);
	private final FluidTank coolant = new FluidTank(TANK_CAPACITY, stack -> stack.is(Fluids.WATER)) {
		@Override protected void onContentsChanged() { sync(); }
	};
	private final GeneratorEnergyStorage energy = new GeneratorEnergyStorage(
		100_000_000, MachineEnergyOutputBlockEntity.MAX_TRANSFER, this::sync);
	private final List<BlockPos> energyOutputs = new ArrayList<>();
	private final List<BlockPos> fluidInputs = new ArrayList<>();
	@Nullable private StructureNbtValidator.Match structure;
	@Nullable private Vec3 formationCenter;
	private boolean formed;
	private boolean operating;
	private int extractionPercent = 25;
	private int heat;
	private int containmentStress;
	private int outputPerTick;
	private int validationDelay;

	public SolarEngineControllerBlockEntity(BlockPos pos, BlockState state) {
		super(CrystalnexusModBlockEntities.SOLAR_ENGINE_CONTROLLER.get(), pos, state);
	}

	public FluidTank getCoolantTank() { return coolant; }
	public boolean isFormed() { return formed; }
	public boolean isOperating() { return formed && operating; }
	public int getExtractionPercent() { return extractionPercent; }
	public int getHeat() { return heat; }
	public int getContainmentStress() { return containmentStress; }
	public int getOutputPerTick() { return outputPerTick; }
	@Nullable public Vec3 getFormationCenter() { return formationCenter; }

	public void setExtractionPercent(int value) {
		int next = Mth.clamp(value, 0, 100);
		if (next != extractionPercent) { extractionPercent = next; sync(); }
	}

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

		StarProfile profile = profile(stacks.get(STAR_SLOT));
		if (!formed || profile == null || extractionPercent == 0) {
			boolean changed = operating || outputPerTick != 0;
			operating = false;
			outputPerTick = 0;
			heat = Math.max(0, heat - 2);
			containmentStress = Math.max(0, containmentStress - 20);
			if (changed || serverLevel.getGameTime() % 20 == 0) sync();
			return;
		}

		double extraction = extractionPercent / 100.0D;
		double load = 0.25D + 3.75D * extraction * extraction;
		int heatGenerated = Math.max(1, (int) Math.ceil(profile.heat() * load));
		int coolantNeeded = Math.max(1, (int) Math.ceil(profile.coolant() * load));
		int coolantUsed = coolant.drain(coolantNeeded, IFluidHandler.FluidAction.EXECUTE).getAmount();
		heat = Mth.clamp(heat + heatGenerated - coolantUsed * 5, 0, MAX_HEAT);
		containmentStress = Mth.clamp((int) Math.round(profile.stress() * extraction * extraction
			+ heat * 7000.0D / MAX_HEAT), 0, MAX_CONTAINMENT_STRESS);

		if (heat >= MAX_HEAT || containmentStress >= MAX_CONTAINMENT_STRESS) {
			containmentFailure(serverLevel);
			return;
		}
		if (damageStar(serverLevel)) return;

		int requested = Math.max(1, (int) Math.round(profile.baseFePerTick() * extraction));
		outputPerTick = distributeEnergy(requested);
		operating = true;
		if (serverLevel.getGameTime() % 10 == 0) sync();
	}

	private void validateStructure(ServerLevel level) {
		Optional<StructureNbtValidator.Match> match = StructureNbtValidator.validate(level, STRUCTURE, worldPosition,
			getBlockState().getValue(SolarEngineControllerBlock.FACING), CrystalnexusModBlocks.SOLAR_ENGINE_CONTROLLER.get(),
			SolarEngineControllerBlock.FACING, Map.of(CrystalnexusModBlocks.TUNGSTEN_BLOCK.get(), Set.of(
				CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get(), CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get())),
			true, false);
		List<BlockPos> substitutions = match.map(StructureNbtValidator.Match::substitutionPositions).orElse(List.of());
		List<BlockPos> nextOutputs = substitutions.stream()
			.filter(pos -> level.getBlockState(pos).is(CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get())).toList();
		List<BlockPos> nextInputs = substitutions.stream()
			.filter(pos -> level.getBlockState(pos).is(CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get())).toList();
		boolean nextFormed = match.isPresent() && !nextOutputs.isEmpty() && !nextInputs.isEmpty()
			&& nextOutputs.size() + nextInputs.size() == substitutions.size();
		if (formed && operating && !nextFormed) {
			containmentFailure(level);
			return;
		}

		for (BlockPos old : List.copyOf(energyOutputs)) if (!nextOutputs.contains(old)
			&& level.getBlockEntity(old) instanceof MachineEnergyOutputBlockEntity output) output.unbindController(worldPosition);
		energyOutputs.clear();
		for (BlockPos pos : nextOutputs) if (level.getBlockEntity(pos) instanceof MachineEnergyOutputBlockEntity output) {
			output.bindController(worldPosition);
			energyOutputs.add(pos);
		}
		for (BlockPos old : List.copyOf(fluidInputs)) if (!nextInputs.contains(old)
			&& level.getBlockEntity(old) instanceof MachineFluidInputBlockEntity input) input.unbindController(worldPosition);
		fluidInputs.clear();
		for (BlockPos pos : nextInputs) if (level.getBlockEntity(pos) instanceof MachineFluidInputBlockEntity input) {
			input.bindController(worldPosition);
			fluidInputs.add(pos);
		}

		Vec3 nextCenter = nextFormed ? match.orElseThrow().center() : null;
		if (formed != nextFormed || !Objects.equals(formationCenter, nextCenter)) {
			formed = nextFormed;
			structure = nextFormed ? match.orElseThrow() : null;
			formationCenter = nextCenter;
			sync();
		} else {
			formed = nextFormed;
			structure = nextFormed ? match.orElseThrow() : null;
		}
	}

	@Override public IFluidHandler multiblockFluidInput() { return coolant; }

	private int distributeEnergy(int requested) {
		return energy.generateEnergy(requested, false);
	}

	@Override public IEnergyStorage multiblockEnergyOutput() { return energy; }

	private boolean damageStar(ServerLevel level) {
		ItemStack star = stacks.get(STAR_SLOT);
		if (!StarDurability.consumedBy(containmentStress, level.random.nextInt(MAX_CONTAINMENT_STRESS))) return false;
		int damage = star.getDamageValue() + 1;
		if (damage < star.getMaxDamage()) {
			star.setDamageValue(damage);
			setChanged();
			return false;
		}
		stacks.set(STAR_SLOT, new ItemStack(CrystalnexusModItems.DEAD_STAR.get()));
		operating = false;
		outputPerTick = 0;
		sync();
		return true;
	}

	private void containmentFailure(ServerLevel level) {
		stacks.set(STAR_SLOT, ItemStack.EMPTY);
		extractionPercent = 0;
		operating = false;
		outputPerTick = 0;
		heat = 0;
		containmentStress = 0;
		Vec3 center = formationCenter == null ? Vec3.atCenterOf(worldPosition) : formationCenter;
		if (structure != null) {
			List<BlockPos> casing = structure.positionsFor(CrystalnexusModBlocks.TUNGSTEN_BLOCK.get()).stream()
				.filter(pos -> level.getBlockState(pos).is(CrystalnexusModBlocks.TUNGSTEN_BLOCK.get())).toList();
			int damage = Math.min(12, Math.max(3, casing.size() / 8));
			Set<BlockPos> destroyed = new HashSet<>();
			while (destroyed.size() < damage && destroyed.size() < casing.size())
				destroyed.add(casing.get(level.random.nextInt(casing.size())));
			destroyed.forEach(pos -> level.destroyBlock(pos, false));
		}
		level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 4, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y, center.z, 80, 2.5, 2.5, 2.5, 0.08);
		level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F, 0.7F);
		level.explode(null, center.x, center.y, center.z, 3.0F, Level.ExplosionInteraction.NONE);
		for (BlockPos pos : energyOutputs) if (level.getBlockEntity(pos) instanceof MachineEnergyOutputBlockEntity output)
			output.unbindController(worldPosition);
		for (BlockPos pos : fluidInputs) if (level.getBlockEntity(pos) instanceof MachineFluidInputBlockEntity input)
			input.unbindController(worldPosition);
		energyOutputs.clear();
		fluidInputs.clear();
		formed = false;
		structure = null;
		formationCenter = null;
		sync();
	}

	public void onControllerRemoved() {
		if (operating && level instanceof ServerLevel serverLevel) {
			containmentFailure(serverLevel);
			return;
		}
		if (level != null) {
			for (BlockPos pos : energyOutputs) if (level.getBlockEntity(pos) instanceof MachineEnergyOutputBlockEntity output)
				output.unbindController(worldPosition);
			for (BlockPos pos : fluidInputs) if (level.getBlockEntity(pos) instanceof MachineFluidInputBlockEntity input)
				input.unbindController(worldPosition);
		}
		energyOutputs.clear();
		fluidInputs.clear();
		formed = false;
		operating = false;
		structure = null;
		formationCenter = null;
	}

	@Nullable private static StarProfile profile(ItemStack stack) {
		if (stack.is(CrystalnexusModItems.YELLOW_DWARF_STAR.get())) return new StarProfile(250_000, 8, 20, 1500);
		if (stack.is(CrystalnexusModItems.ORANGE_STAR.get())) return new StarProfile(750_000, 16, 50, 2500);
		if (stack.is(CrystalnexusModItems.BLUE_STAR.get())) return new StarProfile(2_000_000, 32, 100, 4000);
		if (stack.is(CrystalnexusModItems.PINK_STAR.get())) return new StarProfile(5_000_000, 64, 200, 6000);
		return null;
	}

	private void sync() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2); }
	@Override public int getContainerSize() { return stacks.size(); }
	@Override public boolean isEmpty() { return stacks.getFirst().isEmpty(); }
	@Override public Component getDefaultName() { return Component.translatable("block.crystalnexus.solar_engine_controller"); }
	@Override protected NonNullList<ItemStack> getItems() { return stacks; }
	@Override protected void setItems(NonNullList<ItemStack> items) { stacks = items; }
	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == STAR_SLOT && profile(stack) != null; }
	@Override public int[] getSlotsForFace(Direction side) { return new int[] { STAR_SLOT }; }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return true; }
	@Override public AbstractContainerMenu createMenu(int id, Inventory inventory) { return new SolarEngineMenu(id, inventory, this); }

	@Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (!tryLoadLootTable(tag)) stacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, stacks, registries);
		if (tag.get("coolant") instanceof CompoundTag fluid) coolant.readFromNBT(registries, fluid);
		if (tag.get("energy") instanceof IntTag stored) energy.deserializeNBT(registries, stored);
		formed = tag.getBoolean("formed");
		operating = tag.getBoolean("operating");
		extractionPercent = Mth.clamp(tag.getInt("extraction"), 0, 100);
		heat = tag.getInt("heat");
		containmentStress = tag.getInt("containmentStress");
		outputPerTick = tag.getInt("outputPerTick");
		formationCenter = tag.contains("formationX", Tag.TAG_DOUBLE)
			? new Vec3(tag.getDouble("formationX"), tag.getDouble("formationY"), tag.getDouble("formationZ")) : null;
	}
	@Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, stacks, registries);
		tag.put("coolant", coolant.writeToNBT(registries, new CompoundTag()));
		tag.put("energy", energy.serializeNBT(registries));
		tag.putBoolean("formed", formed);
		tag.putBoolean("operating", operating);
		tag.putInt("extraction", extractionPercent);
		tag.putInt("heat", heat);
		tag.putInt("containmentStress", containmentStress);
		tag.putInt("outputPerTick", outputPerTick);
		if (formationCenter != null) {
			tag.putDouble("formationX", formationCenter.x);
			tag.putDouble("formationY", formationCenter.y);
			tag.putDouble("formationZ", formationCenter.z);
		}
	}
	@Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
	@Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithFullMetadata(registries); }
}
