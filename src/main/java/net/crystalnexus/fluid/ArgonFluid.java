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

public abstract class ArgonFluid extends BaseFlowingFluid {
    public static final Properties PROPERTIES = new Properties(CrystalnexusModFluidTypes.ARGON_TYPE,
        CrystalnexusModFluids.ARGON, CrystalnexusModFluids.FLOWING_ARGON)
        .explosionResistance(100F).tickRate(7)
        .bucket(CrystalnexusModItems.ARGON_BUCKET)
        .block(() -> (LiquidBlock) CrystalnexusModBlocks.ARGON.get());

    private ArgonFluid() { super(PROPERTIES); }

    public static final class Source extends ArgonFluid {
        @Override public int getAmount(FluidState state) { return 8; }
        @Override public boolean isSource(FluidState state) { return true; }
    }

    public static final class Flowing extends ArgonFluid {
        @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
        @Override public int getAmount(FluidState state) { return state.getValue(LEVEL); }
        @Override public boolean isSource(FluidState state) { return false; }
    }
}
