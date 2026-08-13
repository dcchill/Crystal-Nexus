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

public abstract class ResinFluid extends BaseFlowingFluid {
    public static final Properties PROPERTIES = new Properties(() -> CrystalnexusModFluidTypes.RESIN_TYPE.get(),
        () -> CrystalnexusModFluids.RESIN.get(), () -> CrystalnexusModFluids.FLOWING_RESIN.get())
        .explosionResistance(100f).tickRate(25).bucket(() -> CrystalnexusModItems.RESIN_BUCKET.get())
        .block(() -> (LiquidBlock) CrystalnexusModBlocks.RESIN.get());

    private ResinFluid() { super(PROPERTIES); }

    public static class Source extends ResinFluid {
        @Override public int getAmount(FluidState state) { return 8; }
        @Override public boolean isSource(FluidState state) { return true; }
    }

    public static class Flowing extends ResinFluid {
        @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
        @Override public int getAmount(FluidState state) { return state.getValue(LEVEL); }
        @Override public boolean isSource(FluidState state) { return false; }
    }
}
