package net.crystalnexus.procedures;

import java.util.Optional;

import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.jei_recipes.DustSeperationRecipe;
import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.processing.MaterialProcessingCatalog;
import net.crystalnexus.util.CrushingRecipeSupport;
import net.crystalnexus.util.MachineUpgradeHelper;
import net.crystalnexus.util.MachineAnimationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public final class OreProcessorOnTickUpdateProcedure {
	private static final int ENERGY_PER_OPERATION = 8192;

	private OreProcessorOnTickUpdateProcedure() {
	}

	public static void execute(LevelAccessor world, double x, double y, double z) {
		BlockPos pos = BlockPos.containing(x, y, z);
		checkMachineCore(world, pos);
		if (!(world instanceof Level level) || level.isClientSide() || !getBlockNBTLogic(world, pos, "canOpenInventory")) {
			return;
		}
		IItemHandler handler = itemHandler(world, pos);
		if (!(handler instanceof IItemHandlerModifiable inventory)) {
			return;
		}

		ItemStack upgrade = inventory.getStackInSlot(0);
		double baseCookTime = upgrade.is(CrystalnexusModItems.ACCELERATION_UPGRADE.get()) ? 50
				: upgrade.is(CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get()) ? 25 : 75;
		double cookTime = MachineUpgradeHelper.cookTime(upgrade, baseCookTime);
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity == null) {
			return;
		}
		blockEntity.getPersistentData().putDouble("maxProgress", cookTime);
		blockEntity.getPersistentData().putDouble("maxProgress2", cookTime);

		ItemStack crushingResult = CrushingRecipeSupport.findResult(level, inventory.getStackInSlot(1), MachineTier.CRYSTAL);
		int crushingEnergy = crushingResult.is(CrystalnexusModItems.CARBON_COMPOSITE.get()) ? 4096 : ENERGY_PER_OPERATION;
		processStage(level, pos, inventory, upgrade, 1, 2, "progress", cookTime, crushingResult, 1, crushingEnergy);

		SeparationMatch separation = findSeparation(level, inventory.getStackInSlot(2));
		processStage(level, pos, inventory, upgrade, 2, 3, "progress2", cookTime,
				separation.output(), separation.inputCount(), ENERGY_PER_OPERATION);
		combineNuggets(inventory);
		double visualProgress = Math.max(blockEntity.getPersistentData().getDouble("progress"),
			blockEntity.getPersistentData().getDouble("progress2"));
		setActive(level, pos, !MachineAnimationHelper.shouldIdle(level, pos, visualProgress));
		blockEntity.setChanged();
		level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
	}

	private static void processStage(Level level, BlockPos pos, IItemHandlerModifiable inventory, ItemStack upgrade,
			int inputSlot, int outputSlot, String progressKey, double cookTime, ItemStack result,
			int inputCount, int baseEnergy) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (result.isEmpty() || inventory.getStackInSlot(inputSlot).getCount() < inputCount
				|| !fits(inventory.getStackInSlot(outputSlot), result)) {
			blockEntity.getPersistentData().putDouble(progressKey, 0);
			return;
		}
		int energyCost = MachineUpgradeHelper.energyCost(upgrade, baseEnergy);
		IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
		if (energy == null || energy.getEnergyStored() < energyCost) {
			return;
		}
		double progress = blockEntity.getPersistentData().getDouble(progressKey) + 1;
		blockEntity.getPersistentData().putDouble(progressKey, progress);
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, pos.getX() + 0.5, pos.getY() + 0.5,
					pos.getZ() + 0.5, 1, 0.25, 0, 0.25, 0);
		}
		if (progress < cookTime) {
			return;
		}
		inventory.setStackInSlot(outputSlot, merged(inventory.getStackInSlot(outputSlot), result));
		ItemStack input = inventory.getStackInSlot(inputSlot).copy();
		input.shrink(inputCount);
		inventory.setStackInSlot(inputSlot, input);
		energy.extractEnergy(energyCost, false);
		blockEntity.getPersistentData().putDouble(progressKey, 0);
	}

	private static SeparationMatch findSeparation(Level level, ItemStack input) {
		for (var holder : level.getRecipeManager().getAllRecipesFor(DustSeperationRecipe.Type.INSTANCE)) {
			DustSeperationRecipe recipe = holder.value();
			if (!recipe.fluidInput().isEmpty() || recipe.getIngredients().isEmpty()
					|| !recipe.getIngredients().getFirst().test(input) || input.getCount() < recipe.inputCount()) {
				continue;
			}
			ItemStack output = recipe.getResultItem(level.registryAccess());
			return output.isEmpty() || !MachineTier.CRYSTAL.supports(recipe.minimumMachineTier())
					? SeparationMatch.NONE : new SeparationMatch(output, recipe.inputCount());
		}
		Optional<MaterialProcessingCatalog.Material> material = MaterialProcessingCatalog.get(level).dust(input)
				.filter(value -> !value.profile().disabledStages().contains("separation"))
				.filter(value -> MachineTier.CRYSTAL.supports(value.profile().minimumMachineTier()));
		if (material.isEmpty()) {
			return SeparationMatch.NONE;
		}
		ResourceLocation inputId = BuiltInRegistries.ITEM.getKey(input.getItem());
		ItemStack output = material.get().nugget(inputId.getNamespace(), MaterialProcessingCatalog.NUGGETS_PER_DUST);
		return output.isEmpty() ? SeparationMatch.NONE : new SeparationMatch(output, 1);
	}

	private static void combineNuggets(IItemHandlerModifiable inventory) {
		ItemStack nuggets = inventory.getStackInSlot(3);
		if (nuggets.getCount() < 9) {
			return;
		}
		ResourceLocation nuggetId = BuiltInRegistries.ITEM.getKey(nuggets.getItem());
		String path = nuggetId.getPath();
		String ingotPath = path.endsWith("_nugget") ? path.substring(0, path.length() - 7) + "_ingot"
				: path.startsWith("nugget_") ? "ingot_" + path.substring(7) : null;
		if (ingotPath == null) {
			return;
		}
		Item ingot = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(nuggetId.getNamespace(), ingotPath));
		if (ingot == Items.AIR) {
			ingot = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("minecraft", ingotPath));
		}
		ItemStack result = new ItemStack(ingot);
		if (ingot == Items.AIR || !fits(inventory.getStackInSlot(4), result)) {
			return;
		}
		ItemStack remaining = nuggets.copy();
		remaining.shrink(9);
		inventory.setStackInSlot(3, remaining);
		inventory.setStackInSlot(4, merged(inventory.getStackInSlot(4), result));
	}

	private static boolean fits(ItemStack current, ItemStack result) {
		return current.isEmpty() || ItemStack.isSameItemSameComponents(current, result)
				&& current.getCount() + result.getCount() <= current.getMaxStackSize();
	}

	private static ItemStack merged(ItemStack current, ItemStack result) {
		ItemStack merged = result.copy();
		merged.setCount(current.getCount() + result.getCount());
		return merged;
	}

	private static void checkMachineCore(LevelAccessor world, BlockPos pos) {
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos core = pos.relative(direction);
			if (world.getBlockState(core).is(CrystalnexusModBlocks.MACHINE_CORE.get())) {
				MachineBlocksCheckerProcedure.execute(world, core.getX(), core.getY(), core.getZ());
				return;
			}
		}
	}

	private static void setActive(Level level, BlockPos pos, boolean active) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty property) {
			int value = active ? 2 : 1;
			if (property.getPossibleValues().contains(value) && state.getValue(property) != value) {
				level.setBlock(pos, state.setValue(property, value), 3);
			}
		}
	}

	private static boolean getBlockNBTLogic(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		return blockEntity != null && blockEntity.getPersistentData().getBoolean(tag);
	}

	private static IItemHandler itemHandler(LevelAccessor world, BlockPos pos) {
		return world instanceof ILevelExtension extension
				? extension.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) : null;
	}

	private record SeparationMatch(ItemStack output, int inputCount) {
		private static final SeparationMatch NONE = new SeparationMatch(ItemStack.EMPTY, 1);
	}
}
