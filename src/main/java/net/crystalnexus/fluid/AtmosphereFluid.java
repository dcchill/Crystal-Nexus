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

public abstract class AtmosphereFluid extends BaseFlowingFluid {
    public static final Properties PROPERTIES = new Properties(CrystalnexusModFluidTypes.ATMOSPHERE_TYPE, CrystalnexusModFluids.ATMOSPHERE, CrystalnexusModFluids.FLOWING_ATMOSPHERE)
        .explosionResistance(100F).tickRate(7).bucket(CrystalnexusModItems.ATMOSPHERE_BUCKET).block(() -> (LiquidBlock) CrystalnexusModBlocks.ATMOSPHERE.get());
    private AtmosphereFluid() { super(PROPERTIES); }
    public static final class Source extends AtmosphereFluid { public int getAmount(FluidState state) { return 8; } public boolean isSource(FluidState state) { return true; } }
    public static final class Flowing extends AtmosphereFluid {
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) { super.createFluidStateDefinition(builder); builder.add(LEVEL); }
        public int getAmount(FluidState state) { return state.getValue(LEVEL); }
        public boolean isSource(FluidState state) { return false; }
    }
}
