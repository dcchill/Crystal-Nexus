package net.crystalnexus.procedures;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

public class RareSSDItemInInventoryTickProcedure {

	private static double biasedRange(RandomSource rand, double min, double max, double power) {
		// t in [0..1], power>1 biases toward min, power<1 biases toward max
		double t = Math.pow(rand.nextDouble(), power);
		return Mth.lerp(t, min, max);
	}

	public static void execute(ItemStack itemstack) {
		var tag = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

		// Only roll once when tags don't exist yet
		if (!tag.contains("cook_mult") || !tag.contains("output_mult")) {
			RandomSource rand = RandomSource.create();

			// cook_mult is a TIME multiplier (smaller = faster, which is "good")
			// 75% good: [0.05 .. 1.00) biased toward 1.0 (not too crazy)
			// 25% bad : (1.00 .. 1.50] biased toward 1.0
			double cookMult;
			if (rand.nextFloat() < 0.70f) {
				// bias toward 1.0: power < 1 biases toward max
				cookMult = biasedRange(rand, 0.05, 1.00, 0.55);
			} else {
				// bias toward 1.0: power > 1 biases toward min (which is 1.0 here)
				cookMult = biasedRange(rand, 1.00, 1.50, 2.2);
			}

			// output_mult: bigger is better
			// 75% good: [1.00 .. 5.00] biased toward 1.0-2.0-ish
			// 25% bad : [0.80 .. 1.00) biased toward 1.0 (so "bad" is usually mild)
			double outputMult;
			if (rand.nextFloat() < 0.70f) {
				// bias toward 1.0: power > 1 biases toward min
				outputMult = biasedRange(rand, 1.00, 5.00, 2.6);
			} else {
				// bias toward 1.0: power < 1 biases toward max (1.0)
				outputMult = biasedRange(rand, 0.80, 1.00, 0.55);
			}

			CustomData.update(DataComponents.CUSTOM_DATA, itemstack, t -> {
				t.putDouble("cook_mult", cookMult);
				t.putDouble("output_mult", outputMult);
			});
		}
	}
}
