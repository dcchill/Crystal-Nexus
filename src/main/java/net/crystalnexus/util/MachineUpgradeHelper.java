package net.crystalnexus.util;

import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.processing.MachineTier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;

public final class MachineUpgradeHelper {
	private static final double MIN_MULTIPLIER = 0.05;
	private static final double MAX_MULTIPLIER = 10.0;
	
	private static final double[] STACK_WEIGHTS = {
		0, 1, 2, 2.875, 3.75, 4.5, 5.25, 5.875, 6.5,
		7, 7.5, 7.875, 8.25, 8.5625, 8.875, 9.125, 9.375
	};

	private MachineUpgradeHelper() {
	}

	public static boolean isStackableUpgrade(ItemStack stack) {
		return stack.is(CrystalnexusModItems.ACCELERATION_UPGRADE.get())
				|| stack.is(CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get())
				|| stack.is(CrystalnexusModItems.FE_EFFICIENCY_UPGRADE.get())
				|| stack.is(CrystalnexusModItems.CARBON_FE_EFFICIENCY_UPGRADE.get())
				|| stack.is(CrystalnexusModItems.RANGE_UPGRADE.get())
				|| stack.is(CrystalnexusModItems.CARBON_RANGE_UPGRADE.get());
	}

	public static boolean isParallelizationChip(ItemStack stack) {
		return stack.is(CrystalnexusModItems.PARALLELIZATION_CHIP.get());
	}

	public static int parallelCraftCount(ItemStack upgrade) {
		return isParallelizationChip(upgrade)
				? 2 * upgrade.getCount() : 1;
	}

	public static double stackWeight(ItemStack upgrade) {
		return weightForCount(upgrade.getCount());
	}

	static double weightForCount(int count) {
		return STACK_WEIGHTS[Math.clamp(count, 0, 16)];
	}

	public static double scaledEffect(ItemStack upgrade, double base, double singleUpgrade) {
		return base + (singleUpgrade - base) * stackWeight(upgrade);
	}

	public static double processingTime(ItemStack upgrade, double base, double basic, double carbon) {
		double single = upgrade.is(CrystalnexusModItems.ACCELERATION_UPGRADE.get()) ? basic
				: upgrade.is(CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get()) ? carbon : base;
		return scaledProcessingTime(base, single, stackWeight(upgrade));
	}

	static double scaledProcessingTime(double base, double single, double weight) {
		return base / (1.0 + (base / single - 1.0) * weight);
	}

	public static double generatorCycleTime(ItemStack upgrade, double base, double basic, double carbon) {
		double single = upgrade.is(CrystalnexusModItems.ACCELERATION_UPGRADE.get()) ? basic
				: upgrade.is(CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get()) ? carbon : base;
		return scaledEffect(upgrade, base, single);
	}

	public static double feEfficiency(ItemStack upgrade) {
		if (upgrade.is(CrystalnexusModItems.FE_EFFICIENCY_UPGRADE.get()))
			return scaledEffect(upgrade, 1.0, 1.5);
		if (upgrade.is(CrystalnexusModItems.CARBON_FE_EFFICIENCY_UPGRADE.get()))
			return scaledEffect(upgrade, 1.0, 2.0);
		CompoundTag data = customData(upgrade);
		return data != null && data.contains("fe_efficiency")
				? Math.clamp(data.getDouble("fe_efficiency"), MIN_MULTIPLIER, MAX_MULTIPLIER)
				: 1.0;
	}

	public static int energyCost(ItemStack upgrade, int baseCost) {
		return Math.max(1, (int) Math.ceil(baseCost / feEfficiency(upgrade)));
	}

	public static int energyCost(BlockState state, ItemStack upgrade, int baseCost) {
		return MachineTier.from(state).energyCost(energyCost(upgrade, baseCost));
	}

	public static double generatorEfficiency(ItemStack upgrade, double basicUpgrade, double carbonUpgrade) {
		if (upgrade.is(CrystalnexusModItems.FE_EFFICIENCY_UPGRADE.get()))
			return scaledEffect(upgrade, 1.0, basicUpgrade);
		if (upgrade.is(CrystalnexusModItems.CARBON_FE_EFFICIENCY_UPGRADE.get()))
			return scaledEffect(upgrade, 1.0, carbonUpgrade);
		CompoundTag data = customData(upgrade);
		return data != null && data.contains("fe_efficiency")
				? Math.clamp(data.getDouble("fe_efficiency"), MIN_MULTIPLIER, MAX_MULTIPLIER)
				: 1.0;
	}

	public static double cookTime(ItemStack upgrade, double baseCookTime) {
		return Math.max(1.0, baseCookTime * cookMultiplier(upgrade));
	}

	public static double cookMultiplier(ItemStack upgrade) {
		CompoundTag data = customData(upgrade);
		return data != null && data.contains("cook_mult") ? clampCookMultiplier(data.getDouble("cook_mult")) : 1.0;
	}

	public static double generatorSpeed(ItemStack upgrade) {
		return 1.0 / cookMultiplier(upgrade);
	}

	static double clampCookMultiplier(double multiplier) {
		return Math.clamp(multiplier, MIN_MULTIPLIER, MAX_MULTIPLIER);
	}

	private static CompoundTag customData(ItemStack upgrade) {
		CustomData data = upgrade.get(DataComponents.CUSTOM_DATA);
		return data == null ? null : data.copyTag();
	}
}
