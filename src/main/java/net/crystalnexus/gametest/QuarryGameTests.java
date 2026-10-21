package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.QuarryBlockEntity;
import net.crystalnexus.config.CrystalnexusConfig;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.util.QuarryChunkSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus_quarry")
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
	public static void hyperQuarryMinesConfiguredBatchesOutsideIn(GameTestHelper helper) {
		QuarryBlockEntity quarry = denseQuarry(helper);
		int batch = batchSize(helper);
		int energy = batch * QuarryBlockEntity.FE_PER_BLOCK;
		quarry.getEnergyStorage().receiveEnergy(energy - 1, false);
		tick(helper, quarry);
		helper.assertTrue(saved(helper, quarry).getInt("orderIndex") == 0
			&& helper.getLevel().getBlockState(position(quarry, 0)).is(Blocks.STONE),
			"Insufficient power must preserve blocks and the scan cursor");
		quarry.getEnergyStorage().receiveEnergy(1, false);
		tick(helper, quarry);
		for (int i = 0; i < 256; i++)
			helper.assertTrue(helper.getLevel().getBlockState(position(quarry, i)).isAir() == (i < batch),
				"Only the configured outside-in batch may be mined");
		helper.assertTrue(saved(helper, quarry).getInt("orderIndex") == batch
			&& quarry.getEnergyStorage().getEnergyStored() == 0,
			"A batch must commit its cursor and charge exactly 1,024 FE per block");
		helper.assertTrue(bufferedItems(helper, quarry) == batch, "Every mined stone must retain its drop");
		quarry.getEnergyStorage().receiveEnergy(energy, false);
		tick(helper, quarry);
		tick(helper, quarry);
		helper.assertTrue(helper.getLevel().getBlockState(position(quarry, batch)).is(Blocks.STONE),
			"The two default cooldown ticks must not mine blocks");
		tick(helper, quarry);
		helper.assertTrue(saved(helper, quarry).getInt("orderIndex") == batch * 2,
			"The next operation must resume after the previous batch");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void hyperQuarryFullBufferRetriesWithoutLosingDrops(GameTestHelper helper) {
		QuarryBlockEntity quarry = denseQuarry(helper);
		int batch = batchSize(helper);
		ItemStackHandler full = new ItemStackHandler(QuarryBlockEntity.HYPER_BUFFER_SLOTS);
		for (int slot = 0; slot < full.getSlots(); slot++) full.setStackInSlot(slot, new ItemStack(Items.DIRT, 64));
		for (int slot = 0; slot < 9; slot++) quarry.setItem(slot, new ItemStack(Items.DIRT, 64));
		CompoundTag data = saved(helper, quarry);
		data.put("hyperBuffer", full.serializeNBT(helper.getLevel().registryAccess()));
		quarry.loadWithComponents(data, helper.getLevel().registryAccess());
		int energy = batch * QuarryBlockEntity.FE_PER_BLOCK;
		quarry.getEnergyStorage().receiveEnergy(energy, false);
		tick(helper, quarry);
		helper.assertTrue(quarry.getHyperData().get(4) == QuarryBlockEntity.STATUS_BUFFER_FULL
			&& quarry.getEnergyStorage().getEnergyStored() == energy
			&& saved(helper, quarry).getInt("orderIndex") == 0,
			"A blocked batch must consume no power and commit no scan progress");
		helper.assertTrue(helper.getLevel().getBlockState(position(quarry, 0)).is(Blocks.STONE),
			"A full buffer must leave the batch intact");
		data = saved(helper, quarry);
		full.setStackInSlot(0, ItemStack.EMPTY);
		data.put("hyperBuffer", full.serializeNBT(helper.getLevel().registryAccess()));
		quarry.loadWithComponents(data, helper.getLevel().registryAccess());
		tick(helper, quarry);
		helper.assertTrue(saved(helper, quarry).getInt("orderIndex") == batch
			&& quarry.getEnergyStorage().getEnergyStored() == 0,
			"Freeing buffer space must retry the original batch");
		helper.assertTrue(bufferedItems(helper, quarry) == 255 * 64 + batch,
			"Buffer backpressure must neither lose nor duplicate drops");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void hyperQuarryPersistsCursorAndResetsOnResize(GameTestHelper helper) {
		QuarryBlockEntity quarry = denseQuarry(helper);
		int batch = batchSize(helper);
		quarry.getEnergyStorage().receiveEnergy(batch * QuarryBlockEntity.FE_PER_BLOCK, false);
		tick(helper, quarry);
		CompoundTag data = saved(helper, quarry);
		QuarryBlockEntity restored = new QuarryBlockEntity(quarry.getBlockPos(), quarry.getBlockState());
		restored.setLevel(helper.getLevel());
		restored.loadWithComponents(data, helper.getLevel().registryAccess());
		helper.assertTrue(saved(helper, restored).getInt("orderIndex") == batch
			&& bufferedItems(helper, restored) == batch, "Reload must retain the cursor and buffered drops");
		// A replaced block behind the saved cursor must not cause the completed prefix to be rescanned.
		helper.getLevel().setBlock(position(restored, 0), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		restored.getEnergyStorage().receiveEnergy(batch * QuarryBlockEntity.FE_PER_BLOCK, false);
		for (int i = 0; i < 3; i++) tick(helper, restored);
		helper.assertTrue(saved(helper, restored).getInt("orderIndex") == batch * 2
			&& helper.getLevel().getBlockState(position(restored, 0)).is(Blocks.STONE),
			"Reload must resume at the saved cursor");
		restored.resizeSelection(1, 0);
		helper.assertTrue(saved(helper, restored).getInt("orderIndex") == 0
			&& restored.getHyperData().get(2) == restored.getBlockPos().getY() - 1,
			"Resizing must restart the scan from the top");
		data.remove("orderIndex");
		restored.loadWithComponents(data, helper.getLevel().registryAccess());
		helper.assertTrue(saved(helper, restored).getInt("orderIndex") == 0,
			"Older saves without a cursor must start at the layer's beginning");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void hyperQuarryPreservesProtectedBlocksAndAdvancesLayers(GameTestHelper helper) {
		QuarryBlockEntity quarry = denseQuarry(helper);
		Block[] protectedBlocks = {Blocks.CHEST, Blocks.WATER, Blocks.LAVA, Blocks.BEDROCK};
		for (int i = 0; i < protectedBlocks.length; i++)
			helper.getLevel().setBlock(position(quarry, i * 4), protectedBlocks[i].defaultBlockState(), Block.UPDATE_ALL);
		int initialY = quarry.getHyperData().get(2);
		quarry.getEnergyStorage().receiveEnergy(256 * QuarryBlockEntity.FE_PER_BLOCK, false);
		for (int i = 0; i < 3 * 256 && quarry.getHyperData().get(2) == initialY; i++) tick(helper, quarry);
		helper.assertTrue(quarry.getHyperData().get(2) == initialY - 1
			&& saved(helper, quarry).getInt("orderIndex") == 0,
			"Exhausting a layer must advance exactly one level and reset the cursor");
		for (int i = 0; i < protectedBlocks.length; i++)
			helper.assertTrue(helper.getLevel().getBlockState(position(quarry, i * 4).above()).is(protectedBlocks[i]),
				"Hyper mining must preserve block entities, fluids, and unbreakable blocks");
		int total = bufferedItems(helper, quarry);
		for (int slot = 0; slot < 9; slot++) total += quarry.getItem(slot).getCount();
		helper.assertTrue(total == 252, "All mined drops must survive output draining");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void hyperQuarryDefersAndCoalescesUpdates(GameTestHelper helper) throws ReflectiveOperationException {
		QuarryBlockEntity quarry = denseQuarry(helper);
		var pending = QuarryBlockEntity.class.getDeclaredField("syncPending");
		pending.setAccessible(true);
		quarry.getEnergyStorage().receiveEnergy(1, false);
		quarry.getEnergyStorage().receiveEnergy(1, false);
		helper.assertTrue(pending.getBoolean(quarry), "Energy transfers must defer client updates until ticking");
		tick(helper, quarry);
		helper.assertTrue(!pending.getBoolean(quarry), "The server tick must flush pending updates");
		var tickHyper = QuarryBlockEntity.class.getDeclaredMethod("tickHyper", net.minecraft.server.level.ServerLevel.class);
		tickHyper.setAccessible(true);
		tickHyper.invoke(quarry, helper.getLevel());
		helper.assertTrue(!pending.getBoolean(quarry), "An unchanged no-power state must not request another update");
		var collect = QuarryBlockEntity.class.getDeclaredMethod("collectHyperSlice", net.minecraft.server.level.ServerLevel.class);
		collect.setAccessible(true);
		var candidates = (java.util.List<?>) collect.invoke(quarry, helper.getLevel());
		helper.assertTrue(candidates.size() == batchSize(helper), "A dense layer scan must collect only one batch");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void hyperQuarryUpgradePreservesMiningRate(GameTestHelper helper) {
		QuarryBlockEntity quarry = denseQuarry(helper);
		int batch = batchSize(helper);
		ItemStack upgrade = new ItemStack(Items.PAPER);
		CompoundTag speed = new CompoundTag();
		speed.putDouble("cook_mult", 0.05);
		upgrade.set(DataComponents.CUSTOM_DATA, CustomData.of(speed));
		quarry.setItem(9, upgrade);
		quarry.getEnergyStorage().receiveEnergy(2 * batch * QuarryBlockEntity.FE_PER_BLOCK, false);
		tick(helper, quarry);
		tick(helper, quarry);
		helper.assertTrue(saved(helper, quarry).getInt("orderIndex") == 2 * batch
			&& quarry.getEnergyStorage().getEnergyStored() == 0,
			"A zero-cooldown upgrade must still mine a complete batch every tick");
		helper.succeed();
	}

	private static QuarryBlockEntity denseQuarry(GameTestHelper helper) {
		BlockPos relative = new BlockPos(1, 1, 1);
		helper.setBlock(relative, CrystalnexusModBlocks.HYPER_LASER_QUARRY.get());
		QuarryBlockEntity quarry = helper.getBlockEntity(relative);
		quarry.getHyperData().set(2, quarry.getBlockPos().getY() + 32);
		for (int index = 0; index < 256; index++)
			helper.getLevel().setBlock(position(quarry, index), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
		return quarry;
	}

	private static int batchSize(GameTestHelper helper) {
		int batch = CrystalnexusConfig.MACHINES.HYPER_LASER_QUARRY.hyperBlocksPerTick();
		helper.assertTrue(batch > 0 && batch <= 64, "The focused fixture requires a batch of 1–64 blocks");
		return batch;
	}

	private static BlockPos position(QuarryBlockEntity quarry, int index) {
		ChunkPos chunk = new ChunkPos(quarry.getBlockPos());
		var offset = QuarryChunkSelection.blockOffsetsOuterToInner(1, 1).get(index);
		return new BlockPos(chunk.getMinBlockX() + offset.x(), quarry.getHyperData().get(2), chunk.getMinBlockZ() + offset.z());
	}

	private static void tick(GameTestHelper helper, QuarryBlockEntity quarry) {
		QuarryBlockEntity.tick(helper.getLevel(), quarry.getBlockPos(), quarry.getBlockState(), quarry);
	}

	private static CompoundTag saved(GameTestHelper helper, QuarryBlockEntity quarry) {
		return quarry.saveWithoutMetadata(helper.getLevel().registryAccess());
	}

	private static int bufferedItems(GameTestHelper helper, QuarryBlockEntity quarry) {
		ItemStackHandler buffer = new ItemStackHandler(QuarryBlockEntity.HYPER_BUFFER_SLOTS);
		buffer.deserializeNBT(helper.getLevel().registryAccess(), saved(helper, quarry).getCompound("hyperBuffer"));
		int total = 0;
		for (int slot = 0; slot < buffer.getSlots(); slot++) total += buffer.getStackInSlot(slot).getCount();
		return total;
	}
}
