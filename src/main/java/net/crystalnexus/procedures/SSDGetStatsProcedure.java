package net.crystalnexus.procedures;

import net.crystalnexus.util.MachineUpgradeHelper;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;

public class SSDGetStatsProcedure {
	public static String execute(ItemStack itemstack) {
		var tag = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		java.text.DecimalFormat df = new java.text.DecimalFormat("##.##");

		boolean god = tag.contains("god_roll") && tag.getInt("god_roll") == 1;

		// If god roll: make EVERYTHING gold
		String godPrefix = god ? "§6" : "";
		String reset = "§r";

		// Speed display uses inverse of cook_mult
		String speedLine;
		if (!tag.contains("cook_mult")) {
			speedLine = "Speed Multiplier: §7???§r";
		} else {
			double cookMult = MachineUpgradeHelper.cookMultiplier(itemstack);
			double speedMult = (cookMult <= 0) ? 0 : (1.0 / cookMult);

			if (god) {
				speedLine = godPrefix + "Speed Multiplier: ★ " + df.format(speedMult) + "x" + reset;
			} else if (speedMult > 1.0) {
				speedLine = "Speed Multiplier: §a▲ " + df.format(speedMult) + "x§r";
			} else if (speedMult < 1.0) {
				speedLine = "Speed Multiplier: §c▼ " + df.format(speedMult) + "x§r";
			} else {
				speedLine = "Speed Multiplier: §e▬ " + df.format(speedMult) + "x§r";
			}
		}

		String efficiencyLine;
		if (!tag.contains("fe_efficiency")) {
			efficiencyLine = "FE Efficiency: §7???§r";
		} else {
			double feEfficiency = tag.getDouble("fe_efficiency");

			if (god) {
				efficiencyLine = godPrefix + "FE Efficiency: ★ " + df.format(feEfficiency) + "x" + reset;
			} else if (feEfficiency > 1.0) {
				efficiencyLine = "FE Efficiency: §a▲ " + df.format(feEfficiency) + "x§r";
			} else if (feEfficiency < 1.0) {
				efficiencyLine = "FE Efficiency: §c▼ " + df.format(feEfficiency) + "x§r";
			} else {
				efficiencyLine = "FE Efficiency: §e▬ " + df.format(feEfficiency) + "x§r";
			}
		}

		// Optional: add a gold header line when jackpot

		return speedLine + "\n" + efficiencyLine;
	}
}
