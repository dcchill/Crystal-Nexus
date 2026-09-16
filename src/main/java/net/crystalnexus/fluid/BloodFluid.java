package net.crystalnexus.fluid;

import net.crystalnexus.init.CrystalnexusModFluidTypes;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/** A machine fluid for biotech processing. It intentionally has no world block or bucket yet. */
public abstract class BloodFluid extends BaseFlowingFluid {
    public static final Properties PROPERTIES = new Properties(CrystalnexusModFluidTypes.BLOOD_TYPE,
        CrystalnexusModFluids.BLOOD, CrystalnexusModFluids.FLOWING_BLOOD).tickRate(15)
        .bucket(CrystalnexusModItems.BLOOD_BUCKET).block(() -> (LiquidBlock) CrystalnexusModBlocks.BLOOD.get());

    private BloodFluid() { super(PROPERTIES); }

    public static final class Source extends BloodFluid {
        @Override public int getAmount(FluidState state) { return 8; }
        @Override public boolean isSource(FluidState state) { return true; }
    }

    public static final class Flowing extends BloodFluid {
        @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
        @Override public int getAmount(FluidState state) { return state.getValue(LEVEL); }
        @Override public boolean isSource(FluidState state) { return false; }
    }
}
