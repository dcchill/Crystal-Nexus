package net.crystalnexus.fluid;

import net.crystalnexus.init.CrystalnexusModFluidTypes;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/** Tank-only fluid: world placement intentionally resolves to air and no bucket is registered. */
public abstract class MineralSlurryFluid extends BaseFlowingFluid {
    public static final Properties PROPERTIES = new Properties(
        CrystalnexusModFluidTypes.MINERAL_SLURRY_TYPE,
        CrystalnexusModFluids.MINERAL_SLURRY,
        CrystalnexusModFluids.FLOWING_MINERAL_SLURRY).tickRate(25).explosionResistance(100f);

    private MineralSlurryFluid() { super(PROPERTIES); }

    public static final class Source extends MineralSlurryFluid {
        @Override public int getAmount(FluidState state) { return 8; }
        @Override public boolean isSource(FluidState state) { return true; }
    }

    public static final class Flowing extends MineralSlurryFluid {
        @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
        @Override public int getAmount(FluidState state) { return state.getValue(LEVEL); }
        @Override public boolean isSource(FluidState state) { return false; }
    }
}
