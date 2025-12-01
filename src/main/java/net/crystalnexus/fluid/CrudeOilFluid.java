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

public abstract class CrudeOilFluid extends BaseFlowingFluid {
	public static final BaseFlowingFluid.Properties PROPERTIES = new BaseFlowingFluid.Properties(() -> CrystalnexusModFluidTypes.CRUDE_OIL_TYPE.get(), () -> CrystalnexusModFluids.CRUDE_OIL.get(), () -> CrystalnexusModFluids.FLOWING_CRUDE_OIL.get())
			.explosionResistance(100f).tickRate(25).bucket(() -> CrystalnexusModItems.CRUDE_OIL_BUCKET.get()).block(() -> (LiquidBlock) CrystalnexusModBlocks.CRUDE_OIL.get());

	private CrudeOilFluid() {
		super(PROPERTIES);
	}

	public static class Source extends CrudeOilFluid {
		public int getAmount(FluidState state) {
			return 8;
		}

		public boolean isSource(FluidState state) {
			return true;
		}
	}

	public static class Flowing extends CrudeOilFluid {
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