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

public abstract class SulfuricAcidFluid extends BaseFlowingFluid {
    public static final Properties PROPERTIES = new Properties(() -> CrystalnexusModFluidTypes.SULFURIC_ACID_TYPE.get(),
        () -> CrystalnexusModFluids.SULFURIC_ACID.get(), () -> CrystalnexusModFluids.FLOWING_SULFURIC_ACID.get())
        .explosionResistance(100f).tickRate(25).bucket(() -> CrystalnexusModItems.SULFURIC_ACID_BUCKET.get())
        .block(() -> (LiquidBlock) CrystalnexusModBlocks.SULFURIC_ACID.get());

    private SulfuricAcidFluid() { super(PROPERTIES); }

    public static class Source extends SulfuricAcidFluid {
        @Override public int getAmount(FluidState state) { return 8; }
        @Override public boolean isSource(FluidState state) { return true; }
    }

    public static class Flowing extends SulfuricAcidFluid {
        @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
        @Override public int getAmount(FluidState state) { return state.getValue(LEVEL); }
        @Override public boolean isSource(FluidState state) { return false; }
    }
}
