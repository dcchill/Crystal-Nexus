package net.crystalnexus.procedures;

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
			double cookMult = tag.getDouble("cook_mult");
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

		String outputLine;
		if (!tag.contains("output_mult")) {
			outputLine = "Output Multiplier: §7???§r";
		} else {
			double outputMult = tag.getDouble("output_mult");

			if (god) {
				outputLine = godPrefix + "Output Multiplier: ★ " + df.format(outputMult) + "x" + reset;
			} else if (outputMult > 1.0) {
				outputLine = "Output Multiplier: §a▲ " + df.format(outputMult) + "x§r";
			} else if (outputMult < 1.0) {
				outputLine = "Output Multiplier: §c▼ " + df.format(outputMult) + "x§r";
			} else {
				outputLine = "Output Multiplier: §e▬ " + df.format(outputMult) + "x§r";
			}
		}

		// Optional: add a gold header line when jackpot

		return speedLine + "\n" + outputLine;
	}
}
