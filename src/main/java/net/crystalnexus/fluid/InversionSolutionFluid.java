package net.crystalnexus.fluid;

import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModFluidTypes;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public abstract class InversionSolutionFluid extends BaseFlowingFluid {
    public static final Properties PROPERTIES = new Properties(() -> CrystalnexusModFluidTypes.INVERSION_SOLUTION_TYPE.get(),
        () -> CrystalnexusModFluids.INVERSION_SOLUTION.get(), () -> CrystalnexusModFluids.FLOWING_INVERSION_SOLUTION.get())
        .explosionResistance(100f).tickRate(25).bucket(() -> CrystalnexusModItems.INVERSION_SOLUTION_BUCKET.get())
        .block(() -> (LiquidBlock) CrystalnexusModBlocks.INVERSION_SOLUTION.get());

    private InversionSolutionFluid() { super(PROPERTIES); }

    public static class Source extends InversionSolutionFluid {
        @Override public int getAmount(FluidState state) { return 8; }
        @Override public boolean isSource(FluidState state) { return true; }
    }

    public static class Flowing extends InversionSolutionFluid {
        @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
        @Override public int getAmount(FluidState state) { return state.getValue(LEVEL); }
        @Override public boolean isSource(FluidState state) { return false; }
    }
}
