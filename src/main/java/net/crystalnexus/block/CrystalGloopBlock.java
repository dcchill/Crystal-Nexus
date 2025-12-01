package net.crystalnexus.block;

import org.checkerframework.checker.units.qual.s;

import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.LiquidBlock;

import net.crystalnexus.init.CrystalnexusModFluids;

public class CrystalGloopBlock extends LiquidBlock {
	public CrystalGloopBlock() {
		super(CrystalnexusModFluids.CRYSTAL_GLOOP.get(),
				BlockBehaviour.Properties.of().mapColor(MapColor.WATER).strength(1000f).lightLevel(s -> 12).noCollission().noLootTable().liquid().pushReaction(PushReaction.DESTROY).sound(SoundType.EMPTY).replaceable());
	}
}