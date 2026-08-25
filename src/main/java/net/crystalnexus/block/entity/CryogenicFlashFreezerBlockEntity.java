package net.crystalnexus.block.entity;

import java.util.List;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import io.netty.buffer.Unpooled;
import net.crystalnexus.config.CrystalnexusConfig;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.jei_recipes.FluidChemicalReactionRecipe;
import net.crystalnexus.multiblock.CryogenicFreezerLayout;
import net.crystalnexus.multiblock.MultiblockPortTarget;
import net.crystalnexus.world.inventory.CryogenicFlashFreezerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public final class CryogenicFlashFreezerBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer, MultiblockPortTarget {
	public static final int TANK_CAPACITY = 4000;
	private static final int WORK_PER_RECIPE = 200;
	private static final int ENERGY_PER_OPERATION = 4096;
	private static final int VALIDATION_INTERVAL = 20;
	private NonNullList<ItemStack> stacks = NonNullList.withSize(2, ItemStack.EMPTY);
	private final FluidTank inputTank = tank();
	private final FluidTank outputTank = tank();
	private CryogenicFreezerLayout layout = CryogenicFreezerLayout.invalid("Unvalidated");
	private int validationDelay;
	private final EnergyStorage energyStorage = new EnergyStorage(
		CrystalnexusConfig.MACHINES.CRYOGENIC_FLASH_FREEZER.capacity(),
		CrystalnexusConfig.MACHINES.CRYOGENIC_FLASH_FREEZER.maxReceive(),
		CrystalnexusConfig.MACHINES.CRYOGENIC_FLASH_FREEZER.maxExtract(), 0) {
		@Override public int receiveEnergy(int amount, boolean simulate) {
			int received = super.receiveEnergy(amount, simulate); if (!simulate) changed(); return received;
		}
		@Override public int extractEnergy(int amount, boolean simulate) {
			int extracted = super.extractEnergy(amount, simulate); if (!simulate) changed(); return extracted;
		}
		private void changed() {
			setChanged();
			if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
		}
	};

	public CryogenicFlashFreezerBlockEntity(BlockPos pos, BlockState state) {
		super(CrystalnexusModBlockEntities.CRYOGENIC_FLASH_FREEZER.get(), pos, state);
	}

	private FluidTank tank() {
		return new FluidTank(TANK_CAPACITY) {
			@Override protected void onContentsChanged() { sync(); }
		};
	}

	public void serverTick() {
		if (!(level instanceof ServerLevel serverLevel)) return;
		if (validationDelay-- <= 0) {
			validate(serverLevel);
			validationDelay = VALIDATION_INTERVAL;
		}
		getPersistentData().putBoolean("formed", layout.valid());
		getPersistentData().putInt("coolingCoils", layout.coolingCoils());
		getPersistentData().putString("structureStatus", layout.reason());
		getPersistentData().putDouble("maxProgress", WORK_PER_RECIPE);
		if (!layout.valid()) { resetProgress(); return; }

		pullItemPorts();
		flushItemOutput();
		FluidChemicalReactionRecipe recipe = findRecipe(serverLevel);
		if (recipe == null || !canOutput(recipe) || energyStorage.getEnergyStored() < ENERGY_PER_OPERATION) {
			resetProgress();
			return;
		}
		double progress = getPersistentData().getDouble("progress") + layout.coolingCoils();
		if (progress < WORK_PER_RECIPE) {
			getPersistentData().putDouble("progress", progress);
			sync();
			return;
		}

		recipe.fluidInput(0).ifPresent(input -> inputTank.drain(input.amount(), IFluidHandler.FluidAction.EXECUTE));
		recipe.itemInput(0).ifPresent(input -> stacks.get(0).shrink(recipe.itemInputCount(0)));
		recipe.fluidOutput().map(FluidChemicalReactionRecipe.FluidAmount::stack)
			.ifPresent(output -> outputTank.fill(output, IFluidHandler.FluidAction.EXECUTE));
		recipe.itemOutput().ifPresent(output -> {
			if (stacks.get(1).isEmpty()) stacks.set(1, output.copy()); else stacks.get(1).grow(output.getCount());
		});
		energyStorage.extractEnergy(ENERGY_PER_OPERATION, false);
		getPersistentData().putDouble("progress", 0);
		sync();
	}

	private FluidChemicalReactionRecipe findRecipe(ServerLevel level) {
		for (var holder : level.getRecipeManager().getAllRecipesFor(FluidChemicalReactionRecipe.Type.INSTANCE)) {
			if (!holder.id().getPath().startsWith("cryogenic_flash_freezer_")) continue;
			FluidChemicalReactionRecipe recipe = holder.value();
			if (recipe.fluidInput(1).isPresent() || recipe.itemInput(1).isPresent()) continue;
			if (recipe.fluidInput(0).map(input -> input.matches(inputTank.getFluid())).orElse(inputTank.isEmpty())
					&& recipe.itemInput(0).map(item -> item.test(stacks.get(0))
						&& stacks.get(0).getCount() >= recipe.itemInputCount(0)).orElse(stacks.get(0).isEmpty())) return recipe;
		}
		return null;
	}

	private boolean canOutput(FluidChemicalReactionRecipe recipe) {
		FluidStack fluid = recipe.fluidOutput().map(FluidChemicalReactionRecipe.FluidAmount::stack).orElse(FluidStack.EMPTY);
		ItemStack item = recipe.itemOutput().orElse(ItemStack.EMPTY);
		return (fluid.isEmpty() || outputTank.fill(fluid, IFluidHandler.FluidAction.SIMULATE) == fluid.getAmount())
			&& (item.isEmpty() || stacks.get(1).isEmpty() || ItemStack.isSameItemSameComponents(stacks.get(1), item)
				&& stacks.get(1).getCount() + item.getCount() <= item.getMaxStackSize());
	}

	private void validate(ServerLevel level) {
		List<BlockPos> oldFluidInputs = layout.fluidInputs();
		List<BlockPos> oldFluidOutputs = layout.fluidOutputs();
		List<BlockPos> oldEnergyInputs = layout.energyInputs();
		layout = CryogenicFreezerLayout.analyze(level, worldPosition);
		for (BlockPos pos : oldFluidInputs) if (!layout.fluidInputs().contains(pos)
				&& level.getBlockEntity(pos) instanceof MachineFluidInputBlockEntity input) input.unbindController(worldPosition);
		for (BlockPos pos : layout.fluidInputs())
			if (level.getBlockEntity(pos) instanceof MachineFluidInputBlockEntity input) input.bindController(worldPosition);
		for (BlockPos pos : oldFluidOutputs) if (!layout.fluidOutputs().contains(pos)
				&& level.getBlockEntity(pos) instanceof MultiblockFluidOutputBlockEntity output) output.unbindController(worldPosition);
		for (BlockPos pos : layout.fluidOutputs())
			if (level.getBlockEntity(pos) instanceof MultiblockFluidOutputBlockEntity output) output.bindController(worldPosition);
		for (BlockPos pos : oldEnergyInputs) if (!layout.energyInputs().contains(pos)
				&& level.getBlockEntity(pos) instanceof MachineEnergyInputBlockEntity input) input.unbindController(worldPosition);
		for (BlockPos pos : layout.energyInputs())
			if (level.getBlockEntity(pos) instanceof MachineEnergyInputBlockEntity input) input.bindController(worldPosition);
		sync();
	}

	public boolean validateStructureNow() {
		if (!(level instanceof ServerLevel serverLevel)) return false;
		validate(serverLevel);
		validationDelay = VALIDATION_INTERVAL;
		return layout.valid();
	}

	private void pullItemPorts() {
		for (BlockPos pos : layout.itemInputs()) {
			if (!(level.getBlockEntity(pos) instanceof MultiblockItemInputBlockEntity input)) continue;
			for (int slot = 0; slot < input.getContainerSize() && stacks.get(0).getCount() < getMaxStackSize(); slot++) {
				ItemStack source = input.getItem(slot);
				if (source.isEmpty() || !stacks.get(0).isEmpty() && !ItemStack.isSameItemSameComponents(stacks.get(0), source)) continue;
				int moved = Math.min(source.getCount(), source.getMaxStackSize() - stacks.get(0).getCount());
				if (stacks.get(0).isEmpty()) stacks.set(0, source.copyWithCount(moved)); else stacks.get(0).grow(moved);
				source.shrink(moved);
			}
			input.setChanged();
		}
	}

	private void flushItemOutput() {
		for (BlockPos pos : layout.itemOutputs()) {
			if (stacks.get(1).isEmpty()) break;
			if (level.getBlockEntity(pos) instanceof MultiblockItemOutputBlockEntity output && output.insert(stacks.get(1), true)) {
				output.insert(stacks.get(1), false);
				stacks.set(1, ItemStack.EMPTY);
			}
		}
	}

	private void resetProgress() {
		if (getPersistentData().getDouble("progress") != 0) {
			getPersistentData().putDouble("progress", 0);
			sync();
		}
	}

	public void onControllerRemoved() {
		if (level != null) for (BlockPos pos : layout.fluidInputs())
			if (level.getBlockEntity(pos) instanceof MachineFluidInputBlockEntity input) input.unbindController(worldPosition);
		if (level != null) for (BlockPos pos : layout.fluidOutputs())
			if (level.getBlockEntity(pos) instanceof MultiblockFluidOutputBlockEntity output) output.unbindController(worldPosition);
		if (level != null) for (BlockPos pos : layout.energyInputs())
			if (level.getBlockEntity(pos) instanceof MachineEnergyInputBlockEntity input) input.unbindController(worldPosition);
	}

	@Override public IFluidHandler multiblockFluidInput() { return inputTank; }
	@Override public IFluidHandler multiblockFluidOutput() { return outputTank; }

	private final IFluidHandler fluidHandler = new IFluidHandler() {
		@Override public int getTanks() { return 2; }
		@Override public FluidStack getFluidInTank(int tank) { return getTank(tank).getFluid(); }
		@Override public int getTankCapacity(int tank) { return TANK_CAPACITY; }
		@Override public boolean isFluidValid(int tank, FluidStack stack) { return tank == 0; }
		@Override public int fill(FluidStack resource, FluidAction action) { return inputTank.fill(resource, action); }
		@Override public FluidStack drain(FluidStack resource, FluidAction action) { return outputTank.drain(resource, action); }
		@Override public FluidStack drain(int amount, FluidAction action) { return outputTank.drain(amount, action); }
	};

	public FluidTank getTank(int index) { return index == 0 ? inputTank : outputTank; }
	public IFluidHandler getFluidHandler() { return fluidHandler; }
	public EnergyStorage getEnergyStorage() { return energyStorage; }
	public boolean isFormed() { return layout.valid(); }
	public int coolingCoils() { return layout.coolingCoils(); }

	@Override public EnergyStorage multiblockEnergyInput() { return energyStorage; }

	@Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
		super.loadAdditional(tag, provider);
		if (!tryLoadLootTable(tag)) stacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, stacks, provider);
		if (tag.get("energyStorage") instanceof IntTag energy) energyStorage.deserializeNBT(provider, energy);
		if (tag.get("inputTank") instanceof CompoundTag tank) inputTank.readFromNBT(provider, tank);
		if (tag.get("outputTank") instanceof CompoundTag tank) outputTank.readFromNBT(provider, tank);
	}

	@Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
		super.saveAdditional(tag, provider);
		if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, stacks, provider);
		tag.put("energyStorage", energyStorage.serializeNBT(provider));
		tag.put("inputTank", inputTank.writeToNBT(provider, new CompoundTag()));
		tag.put("outputTank", outputTank.writeToNBT(provider, new CompoundTag()));
	}

	@Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
	@Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) { return saveWithFullMetadata(provider); }
	@Override public int getContainerSize() { return stacks.size(); }
	@Override public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }
	@Override public Component getDefaultName() { return Component.translatable("block.crystalnexus.cryogenic_flash_freezer_hatch"); }
	@Override protected NonNullList<ItemStack> getItems() { return stacks; }
	@Override protected void setItems(NonNullList<ItemStack> items) { stacks = items; }
	@Override public AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return new CryogenicFlashFreezerMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(worldPosition));
	}
	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0; }
	@Override public int[] getSlotsForFace(Direction side) { return IntStream.range(0, 2).toArray(); }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return slot == 0; }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 1; }

	private void sync() {
		setChanged();
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
	}
}
