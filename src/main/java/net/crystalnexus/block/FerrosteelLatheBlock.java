package net.crystalnexus.block;

import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.processing.TieredMachineBlock;

public final class FerrosteelLatheBlock extends PartsAssemblerBlock implements TieredMachineBlock {
    @Override public MachineTier machineTier() { return MachineTier.TITANIUM_CARBIDE; }
}
