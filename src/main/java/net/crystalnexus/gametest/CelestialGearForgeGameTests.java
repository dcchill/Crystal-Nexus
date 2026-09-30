package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.CelestialGearForgeBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.recipe.CelestialGearForgeRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class CelestialGearForgeGameTests {
	private static final int SEARCH_SIZE = 24;

	private static BlockPos forgePos(GameTestHelper helper) {
		for (int y = 0; y < SEARCH_SIZE; y++) for (int z = 0; z < SEARCH_SIZE; z++) for (int x = 0; x < SEARCH_SIZE; x++) {
			BlockPos pos = new BlockPos(x, y, z);
			if (helper.getBlockState(pos).is(CrystalnexusModBlocks.CELESTIAL_GEAR_FORGE.get())) return pos;
		}
		throw new AssertionError("The forge structure template has no Celestial Gear Forge block");
	}

	private static BlockPos anotherStructureBlock(GameTestHelper helper, BlockPos forgePos) {
		for (int y = 0; y < SEARCH_SIZE; y++) for (int z = 0; z < SEARCH_SIZE; z++) for (int x = 0; x < SEARCH_SIZE; x++) {
			BlockPos pos = new BlockPos(x, y, z);
			if (!pos.equals(forgePos) && !helper.getBlockState(pos).isAir()) return pos;
		}
		throw new AssertionError("The forge structure template has no casing blocks");
	}

	private static void setInputs(CelestialGearForgeBlockEntity forge) {
		forge.setItem(0, new ItemStack(Items.DIRT));
		forge.setItem(1, new ItemStack(Items.DIRT));
		forge.setItem(2, new ItemStack(CrystalnexusModItems.YELLOW_DWARF_STAR.get()));
		forge.setItem(3, new ItemStack(Items.DIRT));
		forge.setItem(4, new ItemStack(Items.DIRT));
	}

	private static void tick(CelestialGearForgeBlockEntity forge, int count) {
		for (int i = 0; i < count; i++) forge.serverTick();
	}

	@GameTest(template = "celestial_gear_forge")
	public static void recipesProcessingStructureAndAutomation(GameTestHelper helper) {
		BlockPos pos = forgePos(helper);
		CelestialGearForgeBlockEntity forge = (CelestialGearForgeBlockEntity) helper.getBlockEntity(pos);
		helper.assertTrue(forge.validateStructureNow(), "Supplied celestial_gear_forge structure must validate");
		var recipe = helper.getLevel().getRecipeManager().getAllRecipesFor(CelestialGearForgeRecipe.Type.INSTANCE).stream()
			.filter(holder -> holder.id().getPath().equals("celestial_gear_forge")).map(holder -> holder.value()).findFirst().orElse(null);
		helper.assertTrue(recipe != null, "Placeholder forge recipe must load");
		if (recipe == null) { helper.fail("Missing placeholder recipe"); return; }

		for (var star : List.of(CrystalnexusModItems.YELLOW_DWARF_STAR.get(), CrystalnexusModItems.ORANGE_STAR.get(),
			CrystalnexusModItems.BLUE_STAR.get(), CrystalnexusModItems.PINK_STAR.get())) {
			helper.assertTrue(recipe.matches(List.of(new ItemStack(Items.DIRT), new ItemStack(Items.DIRT),
				new ItemStack(star), new ItemStack(Items.DIRT), new ItemStack(Items.DIRT))), "All active stars must satisfy the placeholder recipe");
		}

		setInputs(forge);
		tick(forge, 100);
		helper.assertTrue(forge.getProgress() == 100, "Craft should be halfway after 100 ticks");
		forge.setItem(0, new ItemStack(Items.STONE));
		tick(forge, 1);
		helper.assertTrue(forge.getProgress() == 0 && forge.getItem(0).is(Items.STONE), "Changed input resets progress without consuming items");
		forge.setItem(0, new ItemStack(Items.DIRT));
		tick(forge, CelestialGearForgeBlockEntity.DURATION);
		helper.assertTrue(forge.getItem(CelestialGearForgeBlockEntity.OUTPUT_SLOT).is(Items.DIAMOND)
			&& forge.getItem(CelestialGearForgeBlockEntity.OUTPUT_SLOT).getCount() == 1, "Completed recipe produces one diamond");
		for (int slot = 0; slot < CelestialGearForgeBlockEntity.INPUT_COUNT; slot++)
			helper.assertTrue(forge.getItem(slot).isEmpty(), "All five inputs, including the star, are consumed");

		forge.setItem(CelestialGearForgeBlockEntity.OUTPUT_SLOT, new ItemStack(Items.DIAMOND, 64));
		setInputs(forge);
		tick(forge, 20);
		helper.assertTrue(forge.getProgress() == 0 && forge.getItem(0).is(Items.DIRT), "Full output retains the inputs and prevents processing");

		BlockPos casing = anotherStructureBlock(helper, pos);
		var originalState = helper.getBlockState(casing);
		helper.setBlock(casing, Blocks.AIR);
		helper.assertTrue(!forge.validateStructureNow(), "Broken multiblock prevents crafting");
		tick(forge, 1);
		helper.assertTrue(forge.getProgress() == 0, "Unformed forge makes no progress");
		helper.setBlock(casing, originalState);
		helper.assertTrue(forge.validateStructureNow(), "Restoring the multiblock allows processing again");

		IItemHandler inventory = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
		helper.assertTrue(inventory != null && inventory.getSlots() == 6, "All sides expose the six-slot item handler");
		if (inventory != null) {
			forge.setItem(2, ItemStack.EMPTY);
			helper.assertTrue(inventory.insertItem(2, new ItemStack(CrystalnexusModItems.BLUE_STAR.get()), false).isEmpty(), "Star slot accepts active stars");
			ItemStack rejected = inventory.insertItem(5, new ItemStack(Items.DIAMOND), false);
			helper.assertTrue(!rejected.isEmpty(), "Automation cannot insert into the output slot");
			ItemStack extracted = inventory.extractItem(2, 1, false);
			helper.assertTrue(extracted.is(CrystalnexusModItems.BLUE_STAR.get()), "Automation can remove inputs");
			forge.setItem(5, new ItemStack(Items.DIAMOND));
			helper.assertTrue(inventory.extractItem(5, 1, false).is(Items.DIAMOND), "Automation can extract the output");
		}
		helper.succeed();
	}
}
