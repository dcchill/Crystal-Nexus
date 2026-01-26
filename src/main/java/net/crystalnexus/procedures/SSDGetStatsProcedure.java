package net.crystalnexus.procedures;

import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;

public class SSDGetStatsProcedure {
	public static String execute(ItemStack itemstack) {
		var tag = itemstack
			.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
			.copyTag();

		double cookMult = tag.getDouble("cook_mult");      // time multiplier
		double outputMult = tag.getDouble("output_mult");  // output multiplier

		// Avoid divide-by-zero if the tag is missing/0 for some reason
		double speedMult = (cookMult <= 0) ? 0 : (1.0 / cookMult);

		return "Speed Multiplier: " + new java.text.DecimalFormat("##.##").format(speedMult) + "x\n"
		     + "Output Multiplier: " + new java.text.DecimalFormat("##.##").format(outputMult) + "x";
	}
}

