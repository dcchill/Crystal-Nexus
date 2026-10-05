package net.crystalnexus.block.entity;

import net.crystalnexus.block.MasticatorBlock;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.jei_recipes.GeneSplicingRecipe;
import net.crystalnexus.item.PrisonCubeItem;
import net.crystalnexus.world.inventory.MasticatorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.stream.IntStream;

public class MasticatorBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
	public static final int BLOOD_TANK_CAPACITY = 4000;
	private final FluidTank bloodTank = new FluidTank(BLOOD_TANK_CAPACITY,
			fluid -> fluid.is(CrystalnexusModFluids.BLOOD.get())) {
		@Override protected void onContentsChanged() {
			setChanged();
			if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
		}
	};
	private static final int PROCESSING_TICKS = (int) MachineTier.TITANIUM.processingTime(100);
	private static final int MAX_OUTPUT = 16;
	// Keep legacy slot 0 so saved biomass remains recoverable through automation or breaking the block.
	private NonNullList<ItemStack> stacks = NonNullList.withSize(10, ItemStack.EMPTY);
	private int progress;
	private final ContainerData data = new ContainerData() {
		@Override public int get(int index) {
			return index == 0 ? progress : processingTicks();
		}

		@Override public void set(int index, int value) {
			if (index == 0) progress = value;
		}

		@Override public int getCount() { return 2; }
	};

	public MasticatorBlockEntity(BlockPos pos, BlockState state) {
		super(CrystalnexusModBlockEntities.MASTICATOR.get(), pos, state);
	}

    private int processingTicks() {
        var upgrades = net.crystalnexus.util.MachineUpgradeHelper.upgrades(this, 4, 5);
        return (int) Math.ceil(net.crystalnexus.util.MachineUpgradeHelper.cookTime(upgrades,
            net.crystalnexus.util.MachineUpgradeHelper.processingTime(upgrades, PROCESSING_TICKS, PROCESSING_TICKS * 0.75, PROCESSING_TICKS * 0.5)));
    }

	public ContainerData data() {
		return data;
	}

	public FluidTank getBloodTank() { return bloodTank; }
	@Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
	@Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithFullMetadata(registries); }

	public static void tick(Level level, BlockPos pos, BlockState state, MasticatorBlockEntity blockEntity) {
        int crafts = net.crystalnexus.util.MachineUpgradeHelper.parallelCraftCount(net.crystalnexus.util.MachineUpgradeHelper.upgrades(blockEntity, 4, 5));
        for (int craft = 0; craft < crafts; craft++) tickSingle(level, pos, state, blockEntity);
    }

    private static void tickSingle(Level level, BlockPos pos, BlockState state, MasticatorBlockEntity blockEntity) {
		if (level.isClientSide) {
			return;
		}

		ItemStack input = blockEntity.getItem(1);
		GeneSplicingRecipe recipe = level.getRecipeManager().getAllRecipesFor(GeneSplicingRecipe.Type.INSTANCE).stream()
				.map(holder -> holder.value())
				.filter(candidate -> candidate.matches(blockEntity.bloodTank.getFluid(), input))
				.findFirst().orElse(null);
		ItemStack result = recipe == null ? ItemStack.EMPTY : recipe.getResultItem(level.registryAccess());
		ItemStack egg = recipe == null ? ItemStack.EMPTY : recipe.spawnEgg();
		boolean running = recipe != null && (!egg.isEmpty() || !result.isEmpty())
				&& outputFits(blockEntity.getItem(2), egg) && outputFits(blockEntity.getItem(3), result);

		setRunning(level, pos, state, running);
		if (!running) {
			if (blockEntity.progress != 0) {
				blockEntity.progress = 0;
				blockEntity.setChanged();
			}
			return;
		}

		boolean completes = MasticatorCycle.completes(blockEntity.progress, blockEntity.processingTicks());
		blockEntity.progress = MasticatorCycle.advance(blockEntity.progress, blockEntity.processingTicks());
		if (completes) {
			blockEntity.bloodTank.drain(recipe.bloodAmount(), IFluidHandler.FluidAction.EXECUTE);
			PrisonCubeItem.clearStoredEntity(input);
			blockEntity.addOutput(2, egg);
			blockEntity.addOutput(3, result);
		}
		blockEntity.setChanged();
	}

	private static boolean outputFits(ItemStack output, ItemStack result) {
		return result.isEmpty() || ((output.isEmpty() || ItemStack.isSameItemSameComponents(output, result))
				&& output.getCount() + Math.min(MAX_OUTPUT, result.getCount()) <= result.getMaxStackSize());
	}

	private void addOutput(int slot, ItemStack result) {
		if (result.isEmpty()) return;
		int count = Math.min(MAX_OUTPUT, result.getCount());
		if (getItem(slot).isEmpty()) setItem(slot, result.copyWithCount(count));
		else getItem(slot).grow(count);
	}

	private static void setRunning(Level level, BlockPos pos, BlockState state, boolean running) {
		int value = running ? 2 : 1;
		if (state.getValue(MasticatorBlock.BLOCKSTATE) != value) {
			level.setBlock(pos, state.setValue(MasticatorBlock.BLOCKSTATE, value), Block.UPDATE_ALL);
		}
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("block.crystalnexus.gene_splicer");
	}

	@Override
	protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return new MasticatorMenu(id, inventory, this);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		stacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, stacks, registries);
		progress = tag.getInt("Progress");
		if (tag.get("blood") instanceof CompoundTag blood) bloodTank.readFromNBT(registries, blood);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		ContainerHelper.saveAllItems(tag, stacks, registries);
		tag.putInt("Progress", progress);
		tag.put("blood", bloodTank.writeToNBT(registries, new CompoundTag()));
	}

	@Override
	public int getContainerSize() {
		return stacks.size();
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return stacks;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> stacks) {
		this.stacks = stacks;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot >= 4) return net.crystalnexus.util.MachineUpgradeHelper.acceptsUpgrade(getBlockState(), slot - 4, stack);
		return slot == 1 && stack.is(net.crystalnexus.init.CrystalnexusModItems.PRISON_CUBE.get());
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return IntStream.range(0, getContainerSize()).toArray();
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot >= 4) return false;
		return slot == 0 || slot == 2 || slot == 3;
	}
}
