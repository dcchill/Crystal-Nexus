package net.crystalnexus.fluid;

import net.neoforged.neoforge.fluids.BaseFlowingFluid;

import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.LiquidBlock;

import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.init.CrystalnexusModFluidTypes;
import net.crystalnexus.init.CrystalnexusModBlocks;

public abstract class OverfuelFluid extends BaseFlowingFluid {
	public static final BaseFlowingFluid.Properties PROPERTIES = new BaseFlowingFluid.Properties(() -> CrystalnexusModFluidTypes.OVERFUEL_TYPE.get(), () -> CrystalnexusModFluids.OVERFUEL.get(), () -> CrystalnexusModFluids.FLOWING_OVERFUEL.get())
			.explosionResistance(100f).levelDecreasePerBlock(2).slopeFindDistance(3).bucket(() -> CrystalnexusModItems.OVERFUEL_BUCKET.get()).block(() -> (LiquidBlock) CrystalnexusModBlocks.OVERFUEL.get());

	private OverfuelFluid() {
		super(PROPERTIES);
	}

	public static class Source extends OverfuelFluid {
		public int getAmount(FluidState state) {
			return 8;
		}

		public boolean isSource(FluidState state) {
			return true;
		}
	}

	public static class Flowing extends OverfuelFluid {
		protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
			super.createFluidStateDefinition(builder);
			builder.add(LEVEL);
		}

		public int getAmount(FluidState state) {
			return state.getValue(LEVEL);
		}

		public boolean isSource(FluidState state) {
			return false;
		}
	}
}