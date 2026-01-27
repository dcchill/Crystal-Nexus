package net.crystalnexus.procedures;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

public class EpicSSDItemInInventoryTickProcedure {

	private static double biasedRange(RandomSource rand, double min, double max, double power) {
		double t = Math.pow(rand.nextDouble(), power);
		return Mth.lerp(t, min, max);
	}

	public static void execute(ItemStack itemstack) {
		var tag = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

		// Roll only once when tags are missing
		if (!tag.contains("cook_mult") || !tag.contains("output_mult")) {
			RandomSource rand = RandomSource.create();

			// === JACKPOT (hidden) ===
			if (rand.nextFloat() < 0.02f) {
				CustomData.update(DataComponents.CUSTOM_DATA, itemstack, t -> {
					t.putDouble("cook_mult", 0.01);     // max (fastest)
					t.putDouble("output_mult", 12.0);   // max (highest)
					t.putInt("god_roll", 1);            // <-- FLAG
				});
				return;
			}

			// Not jackpot -> normal roll (also clear flag)
			double cookMult;
			if (rand.nextFloat() < 0.85f) {
				cookMult = biasedRange(rand, 0.01, 1.00, 0.55);
			} else {
				cookMult = biasedRange(rand, 1.00, 1.25, 2.4);
			}

			double outputMult;
			if (rand.nextFloat() < 0.85f) {
				outputMult = biasedRange(rand, 1.00, 12.00, 2.8);
			} else {
				outputMult = biasedRange(rand, 0.85, 1.00, 0.6);
			}

			CustomData.update(DataComponents.CUSTOM_DATA, itemstack, t -> {
				t.putDouble("cook_mult", cookMult);
				t.putDouble("output_mult", outputMult);
				t.remove("god_roll"); // <-- make sure only jackpots are marked
			});
		}
	}
}
