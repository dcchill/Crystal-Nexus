package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.QuarryBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class QuarryGameTests {
	private QuarryGameTests() {
	}

	@GameTest(template = "zero_point")
	public static void laserQuarryUsesBlockEntitySafePredicate(GameTestHelper helper) {
		BlockPos quarryPos = new BlockPos(1, 1, 1);
		BlockPos chestPos = new BlockPos(2, 2, 1);
		BlockPos stonePos = new BlockPos(3, 2, 1);
		BlockPos waterPos = new BlockPos(4, 2, 1);
		BlockPos lavaPos = new BlockPos(5, 2, 3);
		helper.setBlock(quarryPos, CrystalnexusModBlocks.QUARRY.get());
		helper.setBlock(chestPos, Blocks.CHEST);
		helper.setBlock(stonePos, Blocks.STONE);
		helper.setBlock(waterPos, Blocks.WATER);
		helper.setBlock(lavaPos, Blocks.LAVA);

		QuarryBlockEntity quarry = helper.getBlockEntity(quarryPos);
		helper.assertTrue(!quarry.isHyper(), "The regression fixture must use the normal Laser Quarry");
		helper.assertTrue(!QuarryBlockEntity.isMineable(helper.getLevel(), helper.absolutePos(chestPos)),
			"Laser Quarry mining predicate must reject blocks with block entities");
		helper.assertTrue(QuarryBlockEntity.isMineable(helper.getLevel(), helper.absolutePos(stonePos)),
			"Laser Quarry mining predicate must continue accepting ordinary blocks");
		helper.assertTrue(!QuarryBlockEntity.isMineable(helper.getLevel(), helper.absolutePos(waterPos))
			&& !QuarryBlockEntity.isMineable(helper.getLevel(), helper.absolutePos(lavaPos)),
			"Quarry mining predicate must reject fluids");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void hyperQuarryMinesAtomicLayerOutsideIn(GameTestHelper helper) {
		BlockPos quarryPos = new BlockPos(1, 1, 1);
		BlockPos quarryAbsolute = helper.absolutePos(quarryPos);
		ChunkPos chunk = new ChunkPos(quarryAbsolute);
		BlockPos edgeStone = quarryPos.offset(chunk.getMinBlockX() - quarryAbsolute.getX(), 4,
			chunk.getMinBlockZ() - quarryAbsolute.getZ());
		BlockPos innerStone = edgeStone.offset(7, 0, 7);
		BlockPos chestPos = edgeStone.offset(1, 0, 1);
		BlockPos waterPos = edgeStone.offset(2, 0, 2);
		helper.setBlock(quarryPos, CrystalnexusModBlocks.HYPER_LASER_QUARRY.get());
		helper.setBlock(edgeStone, Blocks.STONE);
		helper.setBlock(innerStone, Blocks.STONE);
		helper.setBlock(chestPos, Blocks.CHEST);
		helper.setBlock(waterPos, Blocks.WATER);

		QuarryBlockEntity quarry = helper.getBlockEntity(quarryPos);
		int layerY = helper.absolutePos(edgeStone).getY();
		quarry.getHyperData().set(2, layerY);
		quarry.getEnergyStorage().receiveEnergy(QuarryBlockEntity.FE_PER_BLOCK, false);
		QuarryBlockEntity.tick(helper.getLevel(), helper.absolutePos(quarryPos),
			helper.getBlockState(quarryPos), quarry);
		helper.assertTrue(helper.getBlockState(edgeStone).is(Blocks.STONE)
			&& helper.getBlockState(innerStone).is(Blocks.STONE),
			"A Hyper Quarry layer must not change any blocks when FE is insufficient");

		quarry.getEnergyStorage().receiveEnergy(QuarryBlockEntity.FE_PER_BLOCK, false);
		QuarryBlockEntity.tick(helper.getLevel(), helper.absolutePos(quarryPos),
			helper.getBlockState(quarryPos), quarry);
		helper.assertTrue(helper.getBlockState(edgeStone).isAir() && helper.getBlockState(innerStone).isAir(),
			"A powered Hyper Quarry must mine the complete Y layer in one operation");
		helper.assertTrue(quarry.getEnergyStorage().getEnergyStored() == 0,
			"The layer must charge exactly 1,024 FE per mined block");
		helper.assertTrue(helper.getBlockState(chestPos).is(Blocks.CHEST),
			"Hyper Quarry must preserve blocks with block entities");
		helper.assertTrue(helper.getBlockState(waterPos).is(Blocks.WATER),
			"Hyper Quarry must preserve fluids");
		helper.assertTrue(quarry.getHyperData().get(2) == layerY - 1,
			"Hyper Quarry must move down exactly one Y level after mining a layer");
		helper.succeed();
	}
}
