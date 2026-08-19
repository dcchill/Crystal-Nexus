package net.crystalnexus.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;

import net.crystalnexus.init.CrystalnexusModBlocks;

public class BlocksCheckerProcedure {
	static final TagKey<Block> CASING = BlockTags.create(ResourceLocation.parse("crystalnexus:reactorblocks"));

	public static void execute(LevelAccessor world, double x, double y, double z) {
		CenteredMultiblockValidator.validateFromCore(world, BlockPos.containing(x, y, z),
				CrystalnexusModBlocks.REACTOR_CORE.get(), CrystalnexusModBlocks.REACTOR_COMPUTER.get(), CASING);
	}

	public static void executeFromController(LevelAccessor world, BlockPos controllerPos) {
		CenteredMultiblockValidator.validateFromController(world, controllerPos,
				CrystalnexusModBlocks.REACTOR_CORE.get(), CrystalnexusModBlocks.REACTOR_COMPUTER.get(), CASING);
	}
}