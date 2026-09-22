package net.crystalnexus.block.entity;


import net.crystalnexus.config.CrystalnexusConfig;
import net.crystalnexus.reactor.ReactorLayout;
import net.crystalnexus.reactor.ReactorSimulation;
import net.crystalnexus.multiblock.MultiblockPortTarget;
import net.crystalnexus.procedures.CenteredMultiblockValidator;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.crystalnexus.energy.GeneratorEnergyStorage;

import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.procedures.ReactorComputerOnTickUpdateProcedure;
import net.crystalnexus.init.CrystalnexusModItems;

import javax.annotation.Nullable;

import java.util.stream.IntStream;

public class ReactorComputerBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer, MultiblockPortTarget {
	private NonNullList<ItemStack> stacks = NonNullList.withSize(3, ItemStack.EMPTY);
	private ReactorLayout cachedLayout = ReactorLayout.invalid("Offline");
	private int layoutCheckDelay = 0;

	public ReactorComputerBlockEntity(BlockPos position, BlockState state) {
		super(CrystalnexusModBlockEntities.REACTOR_COMPUTER.get(), position, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ReactorComputerBlockEntity blockEntity) {
		if (level.isClientSide())
			return;
		ReactorComputerOnTickUpdateProcedure.execute(level, pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public void loadAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
		super.loadAdditional(compound, lookupProvider);
		if (!this.tryLoadLootTable(compound))
			this.stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(compound, this.stacks, lookupProvider);
		if (compound.get("energyStorage") instanceof IntTag intTag)
			energyStorage.deserializeNBT(lookupProvider, intTag);
		if (compound.get("fluidTank") instanceof CompoundTag compoundTag)
			fluidTank.readFromNBT(lookupProvider, compoundTag);
	}

	@Override
	public void saveAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
		super.saveAdditional(compound, lookupProvider);
		if (!this.trySaveLootTable(compound)) {
			ContainerHelper.saveAllItems(compound, this.stacks, lookupProvider);
		}
		compound.put("energyStorage", energyStorage.serializeNBT(lookupProvider));
		compound.put("fluidTank", fluidTank.writeToNBT(lookupProvider, new CompoundTag()));
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider lookupProvider) {
		return this.saveWithFullMetadata(lookupProvider);
	}

	@Override
	public int getContainerSize() {
		return stacks.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemstack : this.stacks)
			if (!itemstack.isEmpty())
				return false;
		return true;
	}

	@Override
	public Component getDefaultName() {
		return Component.literal("reactor_computer");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return ChestMenu.threeRows(id, inventory);
	}

	@Override
	public Component getDisplayName() {
		return Component.literal("Reactor Computer");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.stacks;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> stacks) {
		this.stacks = stacks;
	}

	@Override
	public boolean canPlaceItem(int index, ItemStack stack) {
		return index == 1 && (stack.is(CrystalnexusModItems.REACTOR_UPGRADE.get())
				|| stack.is(CrystalnexusModItems.REACTOR_UPGRADE_PERMAFROST.get()));
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return IntStream.range(0, this.getContainerSize()).toArray();
	}

	@Override
	public boolean canPlaceItemThroughFace(int index, ItemStack itemstack, @Nullable Direction direction) {
		return this.canPlaceItem(index, itemstack);
	}

	@Override
	public boolean canTakeItemThroughFace(int index, ItemStack itemstack, Direction direction) {
		return true;
	}

	private final GeneratorEnergyStorage energyStorage = new GeneratorEnergyStorage(CrystalnexusConfig.MACHINES.REACTOR_COMPUTER.capacity(), CrystalnexusConfig.MACHINES.REACTOR_COMPUTER.maxExtract(), this::syncEnergy);

	private void syncEnergy() {
		setChanged();
		level.sendBlockUpdated(worldPosition, level.getBlockState(worldPosition), level.getBlockState(worldPosition), 2);
	}

	public GeneratorEnergyStorage getEnergyStorage() {
		return energyStorage;
	}

	private final FluidTank fluidTank = new FluidTank(16000, fs -> fs.is(Fluids.WATER)
			|| fs.is(Fluids.FLOWING_WATER) || fs.is(CrystalnexusModFluids.NITROGEN.get())) {
		@Override
		protected void onContentsChanged() {
			super.onContentsChanged();
			setChanged();
			level.sendBlockUpdated(worldPosition, level.getBlockState(worldPosition), level.getBlockState(worldPosition), 2);
		}
	};

	public FluidTank getFluidTank() {
		return fluidTank;
	}

	@Override public boolean acceptsMultiblockPort(BlockPos pos) { return CenteredMultiblockValidator.acceptsPort(this, pos); }
	@Override public FluidTank multiblockFluidInput() { return fluidTank; }
	@Override public GeneratorEnergyStorage multiblockEnergyOutput() { return energyStorage; }
	public int masterControlRodInsertion() {
		if (level == null || cachedLayout.fuelRods().isEmpty()) return 0;
		int total = 0, count = 0;
		for (ReactorLayout.FuelRod rod : cachedLayout.fuelRods())
			if (level.getBlockEntity(rod.controlRodPos()) instanceof ReactorControlRodBlockEntity control) {
				total += control.getInsertion();
				count++;
			}
		return count == 0 ? 0 : Math.round(total / (float) count);
	}
	public void setAllControlRodInsertion(int insertion) {
		if (level == null) return;
		for (ReactorLayout.FuelRod rod : cachedLayout.fuelRods())
			if (level.getBlockEntity(rod.controlRodPos()) instanceof ReactorControlRodBlockEntity control)
				control.setInsertion(insertion);
	}
	public void pushEnergyOutputs() {
		if (level == null || !getPersistentData().getBoolean("canOpenInventory")) return;
		BlockPos min = BlockPos.of(getPersistentData().getLong("multiblockMinBounds"));
		BlockPos max = BlockPos.of(getPersistentData().getLong("multiblockMaxBounds"));
		for (BlockPos pos : BlockPos.betweenClosed(min, max))
			if (level.getBlockEntity(pos) instanceof MachineEnergyOutputBlockEntity output && acceptsMultiblockPort(pos)) output.pushEnergy();
	}

	public void pullFuelInputs() {
		if (level == null || !getPersistentData().getBoolean("canOpenInventory")) return;
		ReactorLayout layout = getCachedLayout();
		if (!layout.valid) return;
		BlockPos min = BlockPos.of(getPersistentData().getLong("multiblockMinBounds"));
		BlockPos max = BlockPos.of(getPersistentData().getLong("multiblockMaxBounds"));
		for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
			if (!acceptsMultiblockPort(pos) || !(level.getBlockEntity(pos) instanceof MultiblockItemInputBlockEntity input)) continue;
			for (int slot = 0; slot < input.getContainerSize(); slot++) {
				ItemStack offered = input.getItem(slot);
				if (!ReactorSimulation.isFuel(offered)) continue;
				for (ReactorLayout.FuelRod rod : layout.fuelRods()) {
					if (!(level.getBlockEntity(rod.pos()) instanceof ReactorCoreBlockEntity core)) continue;
					for (int cell = 0; cell < 3; cell++) {
						if (offered.isEmpty()) break;
						if (core.getItem(cell).isEmpty()) core.setItem(cell, input.removeItem(slot, 1));
					}
					if (offered.isEmpty()) break;
				}
			}
		}
	}

	public void pushSpentCells() {
		if (level == null || !getPersistentData().getBoolean("canOpenInventory") || !cachedLayout.valid) return;
		BlockPos min = BlockPos.of(getPersistentData().getLong("multiblockMinBounds"));
		BlockPos max = BlockPos.of(getPersistentData().getLong("multiblockMaxBounds"));
		java.util.List<MultiblockItemOutputBlockEntity> outputs = new java.util.ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
			if (acceptsMultiblockPort(pos) && level.getBlockEntity(pos) instanceof MultiblockItemOutputBlockEntity output)
				outputs.add(output);
		}
		if (outputs.isEmpty()) return;
		for (ReactorLayout.FuelRod rod : cachedLayout.fuelRods()) {
			if (!(level.getBlockEntity(rod.pos()) instanceof ReactorCoreBlockEntity core)) continue;
			for (int slot = 0; slot < 3; slot++) {
				ItemStack spent = core.getItem(slot);
				if (!spent.is(CrystalnexusModItems.SPENT_REACTOR_CELL.get())) continue;
				for (MultiblockItemOutputBlockEntity output : outputs) {
					if (output.insert(spent, true)) {
						output.insert(spent, false);
						core.setItem(slot, ItemStack.EMPTY);
						break;
					}
				}
			}
		}
	}

	public ReactorLayout getCachedLayout() {
		return cachedLayout;
	}

	public void updateLayoutCache(ReactorLayout layout) {
		this.cachedLayout = layout;
		this.layoutCheckDelay = 0;
		setChanged();
	}

	public boolean shouldRecheckLayout(int interval) {
		if (layoutCheckDelay-- <= 0) {
			layoutCheckDelay = interval;
			return true;
		}
		return false;
	}
}
