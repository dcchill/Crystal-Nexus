package net.crystalnexus.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;

import net.crystalnexus.init.CrystalnexusModBlocks;

public class ReactionBlocksCheckerProcedure {
	static final TagKey<Block> CASING = BlockTags.create(ResourceLocation.parse("crystalnexus:reactionblocks"));

	public static void execute(LevelAccessor world, double x, double y, double z) {
		CenteredMultiblockValidator.validateFromCore(world, BlockPos.containing(x, y, z),
				CrystalnexusModBlocks.REACTION_CHAMBER_CORE.get(), CrystalnexusModBlocks.REACTION_CHAMBER_COMPUTER.get(), CASING);
	}

	public static void executeFromController(LevelAccessor world, BlockPos controllerPos) {
		CenteredMultiblockValidator.validateFromController(world, controllerPos,
				CrystalnexusModBlocks.REACTION_CHAMBER_CORE.get(), CrystalnexusModBlocks.REACTION_CHAMBER_COMPUTER.get(), CASING);
	}
}
