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

public abstract class GasolineFluid extends BaseFlowingFluid {
	public static final BaseFlowingFluid.Properties PROPERTIES = new BaseFlowingFluid.Properties(() -> CrystalnexusModFluidTypes.GASOLINE_TYPE.get(), () -> CrystalnexusModFluids.GASOLINE.get(), () -> CrystalnexusModFluids.FLOWING_GASOLINE.get())
			.explosionResistance(100f).tickRate(7).bucket(() -> CrystalnexusModItems.GASOLINE_BUCKET.get()).block(() -> (LiquidBlock) CrystalnexusModBlocks.GASOLINE.get());

	private GasolineFluid() {
		super(PROPERTIES);
	}

	public static class Source extends GasolineFluid {
		public int getAmount(FluidState state) {
			return 8;
		}

		public boolean isSource(FluidState state) {
			return true;
		}
	}

	public static class Flowing extends GasolineFluid {
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