package net.crystalnexus.util;

import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class MachineUpgradeHelper {
	private static final double MIN_MULTIPLIER = 0.05;
	private static final double MAX_MULTIPLIER = 10.0;

	private MachineUpgradeHelper() {
	}

	public static double feEfficiency(ItemStack upgrade) {
		if (upgrade.is(CrystalnexusModItems.FE_EFFICIENCY_UPGRADE.get()))
			return 1.5;
		if (upgrade.is(CrystalnexusModItems.CARBON_FE_EFFICIENCY_UPGRADE.get()))
			return 2.0;
		CompoundTag data = customData(upgrade);
		return data != null && data.contains("fe_efficiency")
				? Math.clamp(data.getDouble("fe_efficiency"), MIN_MULTIPLIER, MAX_MULTIPLIER)
				: 1.0;
	}

	public static int energyCost(ItemStack upgrade, int baseCost) {
		return Math.max(1, (int) Math.ceil(baseCost / feEfficiency(upgrade)));
	}

	public static double generatorEfficiency(ItemStack upgrade, double basicUpgrade, double carbonUpgrade) {
		if (upgrade.is(CrystalnexusModItems.FE_EFFICIENCY_UPGRADE.get()))
			return basicUpgrade;
		if (upgrade.is(CrystalnexusModItems.CARBON_FE_EFFICIENCY_UPGRADE.get()))
			return carbonUpgrade;
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

	static double clampCookMultiplier(double multiplier) {
		return Math.clamp(multiplier, MIN_MULTIPLIER, MAX_MULTIPLIER);
	}

	private static CompoundTag customData(ItemStack upgrade) {
		CustomData data = upgrade.get(DataComponents.CUSTOM_DATA);
		return data == null ? null : data.copyTag();
	}
}
