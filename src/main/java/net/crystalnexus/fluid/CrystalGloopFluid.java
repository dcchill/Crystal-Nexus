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

public abstract class CrystalGloopFluid extends BaseFlowingFluid {
	public static final BaseFlowingFluid.Properties PROPERTIES = new BaseFlowingFluid.Properties(() -> CrystalnexusModFluidTypes.CRYSTAL_GLOOP_TYPE.get(), () -> CrystalnexusModFluids.CRYSTAL_GLOOP.get(),
			() -> CrystalnexusModFluids.FLOWING_CRYSTAL_GLOOP.get()).explosionResistance(1000f).tickRate(10).bucket(() -> CrystalnexusModItems.CRYSTAL_GLOOP_BUCKET.get()).block(() -> (LiquidBlock) CrystalnexusModBlocks.CRYSTAL_GLOOP.get());

	private CrystalGloopFluid() {
		super(PROPERTIES);
	}

	public static class Source extends CrystalGloopFluid {
		public int getAmount(FluidState state) {
			return 8;
		}

		public boolean isSource(FluidState state) {
			return true;
		}
	}

	public static class Flowing extends CrystalGloopFluid {
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