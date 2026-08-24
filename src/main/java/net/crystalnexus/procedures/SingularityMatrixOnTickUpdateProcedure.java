package net.crystalnexus.procedures;

import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class SingularityMatrixOnTickUpdateProcedure {
	private static final int REQUIRED_MATTER = 2_048;
	private static final TagKey<Item> VALUE_1 = valueTag(1);
	private static final TagKey<Item> VALUE_4 = valueTag(4);
	private static final TagKey<Item> VALUE_8 = valueTag(8);
	private static final TagKey<Item> VALUE_12 = valueTag(12);

	public static void execute(LevelAccessor world, double x, double y, double z) {
		if (world.isClientSide()) return;
		BlockPos pos = BlockPos.containing(x, y, z);
		setNumber(world, pos, "maxProgress", REQUIRED_MATTER);

		if (!(world instanceof ILevelExtension ext)
				|| !(ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) instanceof IItemHandlerModifiable inventory)) return;

		ItemStack input = inventory.getStackInSlot(0);
		ItemStack output = inventory.getStackInSlot(1);
		ItemStack eeMatter = new ItemStack(CrystalnexusModItems.EE_MATTER.get());
		int value = matterValue(input);
		if (value <= 0 || input.is(CrystalnexusModItems.UNSTABLE_EE_MATTER.get())
				|| !output.isEmpty() && !ItemStack.isSameItemSameComponents(output, eeMatter)
				|| output.getCount() >= eeMatter.getMaxStackSize()) return;

		ItemStack remaining = input.copy();
		remaining.shrink(1);
		inventory.setStackInSlot(0, remaining);
		double progress = getNumber(world, pos, "progress") + value;
		if (progress < REQUIRED_MATTER) {
			setNumber(world, pos, "progress", progress);
			return;
		}

		setNumber(world, pos, "progress", 0);
		eeMatter.setCount(output.getCount() + 1);
		inventory.setStackInSlot(1, eeMatter);
	}

	static int matterValue(ItemStack stack) {
		if (stack.isEmpty()) return 0;
		if (stack.is(VALUE_12)) return 12;
		if (stack.is(VALUE_8)) return 8;
		if (stack.is(VALUE_4)) return 4;
		return stack.is(VALUE_1) ? 1 : 0;
	}

	private static TagKey<Item> valueTag(int value) {
		return ItemTags.create(ResourceLocation.fromNamespaceAndPath("crystalnexus", "singularity_matrix_value_" + value));
	}

	private static double getNumber(LevelAccessor world, BlockPos pos, String key) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		return blockEntity == null ? 0 : blockEntity.getPersistentData().getDouble(key);
	}

	private static void setNumber(LevelAccessor world, BlockPos pos, String key, double value) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity == null || blockEntity.getPersistentData().getDouble(key) == value) return;
		blockEntity.getPersistentData().putDouble(key, value);
		if (world instanceof Level level) {
			BlockState state = world.getBlockState(pos);
			level.sendBlockUpdated(pos, state, state, 3);
		}
	}
}
