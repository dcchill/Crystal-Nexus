package net.crystalnexus.block;

import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.processing.TieredMachineBlock;

public final class PlatePressBlock extends PartsAssemblerBlock implements TieredMachineBlock {
    private final MachineTier tier;
    public PlatePressBlock(MachineTier tier) { this.tier = tier; }
    @Override public MachineTier machineTier() { return tier; }
}
