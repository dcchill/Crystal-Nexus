package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.CelestialGearForgeBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.recipe.CelestialGearForgeRecipe;
import net.crystalnexus.recipe.CelestialGearForgeEnchanting;
import net.crystalnexus.block.entity.MachineEnergyInputBlockEntity;
import net.crystalnexus.block.CelestialGearForgeBlock;
import net.crystalnexus.multiblock.StructureNbtValidator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.crystalnexus.world.inventory.CelestialGearForgeMenu;
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

	@GameTest(template = "celestial_gear_forge", templateNamespace = "crystalnexus")
	public static void zeroStarEnchantmentsSurviveSerialization(GameTestHelper helper) {
		var fortune = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
		var inputs = NonNullList.withSize(CelestialGearForgeBlockEntity.INPUT_COUNT, ItemStack.EMPTY);
		inputs.set(0, new ItemStack(CrystalnexusModItems.SOLARIS.get()));
		var forge = (CelestialGearForgeBlockEntity) helper.getBlockEntity(forgePos(helper));
		helper.assertTrue(forge.canPlaceItem(0, inputs.get(0)), "Unenchanted Solaris is accepted in the enchanting slot");
		helper.assertTrue(fortune.value().canEnchant(inputs.get(0)), "Fortune can be applied to Solaris");
		inputs.set(CelestialGearForgeBlockEntity.STAR_SLOT, new ItemStack(CrystalnexusModItems.ZERO_STAR.get()));
		EnchantmentHelper.updateEnchantments(inputs.get(0), mutable -> mutable.set(fortune, 255));
		var operation = CelestialGearForgeEnchanting.calculate(inputs, fortune);
		helper.assertTrue(operation != null && EnchantmentHelper.getEnchantmentsForCrafting(operation.result()).getLevel(fortune) == 271,
			"Zero Star upgrades beyond the vanilla constructor limit");
		var registries = helper.getLevel().registryAccess();
		ItemStack restored = ItemStack.parseOptional(registries, (CompoundTag) operation.result().save(registries));
		helper.assertTrue(EnchantmentHelper.getEnchantmentsForCrafting(restored).getLevel(fortune) == 271,
			"High enchantment levels survive saving and loading");
		EnchantmentHelper.updateEnchantments(inputs.get(0), mutable -> mutable.set(fortune, 999));
		var capped = CelestialGearForgeEnchanting.calculate(inputs, fortune);
		helper.assertTrue(capped != null && EnchantmentHelper.getEnchantmentsForCrafting(capped.result()).getLevel(fortune) == 1000,
			"Zero Star upgrades stop at level 1000");
		inputs.set(0, capped.result());
		helper.assertTrue(CelestialGearForgeEnchanting.calculate(inputs, fortune) == null, "A capped item cannot be upgraded again");
		helper.succeed();
	}

	private static BlockPos forgePos(GameTestHelper helper) {
		for (int y = 0; y < SEARCH_SIZE; y++) for (int z = 0; z < SEARCH_SIZE; z++) for (int x = 0; x < SEARCH_SIZE; x++) {
			BlockPos pos = new BlockPos(x, y, z);
			if (helper.getBlockState(pos).is(CrystalnexusModBlocks.CELESTIAL_GEAR_FORGE.get())) return pos;
		}
		throw new AssertionError("The forge structure template has no Celestial Gear Forge block");
	}

	private static BlockPos anotherStructureBlock(GameTestHelper helper, BlockPos forgePos) {
		var match = StructureNbtValidator.validate(helper.getLevel(), ResourceLocation.fromNamespaceAndPath("crystalnexus", "celestial_gear_forge"),
			helper.absolutePos(forgePos), helper.getBlockState(forgePos).getValue(CelestialGearForgeBlock.FACING),
			CrystalnexusModBlocks.CELESTIAL_GEAR_FORGE.get(), CelestialGearForgeBlock.FACING, CelestialGearForgeBlockEntity.energySubstitutions(), false, false).orElseThrow();
		return match.templatePositions().entrySet().stream()
			.filter(entry -> entry.getKey() != CrystalnexusModBlocks.CELESTIAL_GEAR_FORGE.get())
			.flatMap(entry -> entry.getValue().stream()).filter(pos -> !helper.getLevel().getBlockState(pos).isAir()
				&& !helper.getLevel().getBlockState(pos).is(CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get())).findFirst().orElseThrow();
	}

	private static BlockPos installEnergyInput(GameTestHelper helper, CelestialGearForgeBlockEntity forge) {
		var pos = anotherStructureBlock(helper, forgePos(helper));
		helper.getLevel().setBlockAndUpdate(pos, CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get().defaultBlockState());
		helper.assertTrue(forge.validateStructureNow(), "Energy input forms the forge");
		return pos;
	}

	private static void powerForge(GameTestHelper helper, CelestialGearForgeBlockEntity forge) {
		BlockPos input = installEnergyInput(helper, forge);
		var energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, input, Direction.UP);
		int available = Integer.MAX_VALUE - forge.multiblockEnergyInput().getEnergyStored();
		helper.assertTrue(energy != null && energy.receiveEnergy(Integer.MAX_VALUE, false) == available, "Input fills the shared FE buffer");
	}

	private static void setInputs(CelestialGearForgeBlockEntity forge) {
		for (int slot = 0; slot < CelestialGearForgeBlockEntity.STAR_SLOT; slot++)
			forge.setItem(slot, new ItemStack(slot == 4 ? CrystalnexusModItems.COMPOUND_SWORD.get() : CrystalnexusModItems.METEORITE_ALLOY.get()));
		forge.setItem(CelestialGearForgeBlockEntity.STAR_SLOT, new ItemStack(CrystalnexusModItems.YELLOW_DWARF_STAR.get()));
	}

	private static void tick(CelestialGearForgeBlockEntity forge, int count) {
		for (int i = 0; i < count; i++) forge.serverTick();
	}

	private static void assertAnimationUpdate(GameTestHelper helper, CelestialGearForgeBlockEntity forge) {
		var received = new CelestialGearForgeBlockEntity(forge.getBlockPos(), forge.getBlockState());
		received.loadWithComponents(forge.getUpdateTag(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
		helper.assertTrue(received.isFormed() && received.isProcessing() && received.getProgress() == forge.getProgress(),
			"Block update preserves formed state and processing progress for the animation");
		forge.data().set(2, 0);
		received.loadWithComponents(forge.getUpdateTag(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
		helper.assertTrue(!received.isFormed(), "Block update clears formed state when the structure breaks");
		forge.validateStructureNow();
	}

	@GameTest(template = "celestial_gear_forge", templateNamespace = "crystalnexus")
	public static void energyInputsAnywhere(GameTestHelper helper) { checkEnergyInputs(helper); }
	@GameTest(template = "celestial_gear_forge", templateNamespace = "crystalnexus", rotationSteps = 1)
	public static void energyInputsRotatedOnce(GameTestHelper helper) { checkEnergyInputs(helper); }
	@GameTest(template = "celestial_gear_forge", templateNamespace = "crystalnexus", rotationSteps = 2)
	public static void energyInputsRotatedTwice(GameTestHelper helper) { checkEnergyInputs(helper); }
	@GameTest(template = "celestial_gear_forge", templateNamespace = "crystalnexus", rotationSteps = 3)
	public static void energyInputsRotatedThrice(GameTestHelper helper) { checkEnergyInputs(helper); }

	private static void checkEnergyInputs(GameTestHelper helper) {
		BlockPos pos = forgePos(helper);
		var forge = (CelestialGearForgeBlockEntity) helper.getBlockEntity(pos);
		helper.assertTrue(!forge.validateStructureNow(), "Forge requires an energy input");
		var match = StructureNbtValidator.validate(helper.getLevel(), ResourceLocation.fromNamespaceAndPath("crystalnexus", "celestial_gear_forge"),
			helper.absolutePos(pos), forge.getBlockState().getValue(CelestialGearForgeBlock.FACING),
			CrystalnexusModBlocks.CELESTIAL_GEAR_FORGE.get(), CelestialGearForgeBlock.FACING, java.util.Map.of(), false, false).orElseThrow();
		for (var block : List.of(CrystalnexusModBlocks.METEORITE_ALLOY_BLOCK.get(), CrystalnexusModBlocks.GRAVITY_CONTROL_POINT.get(),
				CrystalnexusModBlocks.HYPER_MACHINE_FRAME.get())) {
			var positions = match.positionsFor(block);
			BlockPos first = positions.get(0), second = positions.get(1);
			var firstState = helper.getLevel().getBlockState(first);
			var secondState = helper.getLevel().getBlockState(second);
			helper.getLevel().setBlockAndUpdate(first, CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get().defaultBlockState());
			helper.assertTrue(forge.validateStructureNow(), "Input may replace " + block.getName().getString());
			var port = (MachineEnergyInputBlockEntity) helper.getLevel().getBlockEntity(first);
			for (var side : Direction.values()) {
				var capability = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, first, side);
				helper.assertTrue(capability != null && capability.canReceive() && !capability.canExtract(), "Every side is input-only");
				helper.assertTrue(capability.receiveEnergy(1000, true) == 1000, "Simulation accepts FE");
			}
			int before = forge.multiblockEnergyInput().getEnergyStored();
			helper.assertTrue(port.getEnergyStorage().receiveEnergy(1000, false) == 1000, "First port receives FE");
			helper.getLevel().setBlockAndUpdate(second, CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get().defaultBlockState());
			helper.assertTrue(forge.validateStructureNow(), "Multiple ports are accepted");
			var other = (MachineEnergyInputBlockEntity) helper.getLevel().getBlockEntity(second);
			other.getEnergyStorage().receiveEnergy(2000, false);
			helper.assertTrue(port.getEnergyStorage().getEnergyStored() == before + 3000, "Both ports share one buffer; simulations consume nothing");
			var saved = forge.saveWithFullMetadata(helper.getLevel().registryAccess());
			forge.loadWithComponents(saved, helper.getLevel().registryAccess());
			tick(forge, 1);
			helper.assertTrue(forge.isFormed() && forge.multiblockEnergyInput().getEnergyStored() == before + 3000, "Reload revalidates ports and retains FE");
			forge.onControllerRemoved();
			helper.assertTrue(!port.isBoundTo(forge.getBlockPos()) && !other.isBoundTo(forge.getBlockPos())
				&& port.getEnergyStorage().receiveEnergy(1, false) == 0, "Controller removal unbinds all ports");
			forge.validateStructureNow();
			helper.getLevel().setBlockAndUpdate(second, Blocks.AIR.defaultBlockState());
			helper.assertTrue(!forge.validateStructureNow() && !port.isBoundTo(forge.getBlockPos())
				&& port.getEnergyStorage().receiveEnergy(1, false) == 0, "Broken structure unbinds surviving inputs");
			helper.getLevel().setBlockAndUpdate(first, firstState);
			helper.getLevel().setBlockAndUpdate(second, secondState);
		}
		powerForge(helper, forge);
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		var menu = new CelestialGearForgeMenu(1, player.getInventory(), forge);
		helper.assertTrue(menu.energyStored() == Integer.MAX_VALUE, "Paired menu halves retain FE above 65535");
		forge.data().set(6, 650250 & 0xffff);
		forge.data().set(7, 650250 >>> 16);
		var client = new CelestialGearForgeBlockEntity(forge.getBlockPos(), forge.getBlockState());
		for (int index = 4; index < CelestialGearForgeBlockEntity.DATA_COUNT; index++) client.data().set(index, (short) forge.data().get(index));
		var clientMenu = new CelestialGearForgeMenu(2, player.getInventory(), client);
		helper.assertTrue(clientMenu.energyStored() == Integer.MAX_VALUE && clientMenu.energyPerTick() == 650250,
			"Signed 16-bit menu packets preserve the entire buffer and high-level enchantment cost");
		helper.succeed();
	}

	@GameTest(template = "celestial_gear_forge", templateNamespace = "crystalnexus")
	public static void bookMergingRulesAndTierLimits(GameTestHelper helper) {
		var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		var sharpness = registry.getOrThrow(Enchantments.SHARPNESS);
		var smite = registry.getOrThrow(Enchantments.SMITE);
		var protection = registry.getOrThrow(Enchantments.PROTECTION);
		var unbreaking = registry.getOrThrow(Enchantments.UNBREAKING);
		for (var star : List.of(CrystalnexusModItems.YELLOW_DWARF_STAR.get(), CrystalnexusModItems.ORANGE_STAR.get(),
				CrystalnexusModItems.BLUE_STAR.get(), CrystalnexusModItems.PINK_STAR.get())) {
			var inputs = NonNullList.withSize(CelestialGearForgeBlockEntity.INPUT_COUNT, ItemStack.EMPTY);
			ItemStack target = new ItemStack(Items.ENCHANTED_BOOK);
			EnchantmentHelper.updateEnchantments(target, mutable -> { mutable.set(sharpness, 5); mutable.set(unbreaking, 3); });
			target.set(DataComponents.CUSTOM_NAME, Component.literal("Kept name"));
			inputs.set(0, target);
			inputs.set(8, new ItemStack(star));
			int limit = CelestialGearForgeEnchanting.bookLimit(inputs.get(8));
			for (int slot = 1; slot <= limit; slot++) {
				ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
				int donorLevel = 4 + slot;
				EnchantmentHelper.updateEnchantments(book, mutable -> mutable.set(sharpness, donorLevel));
				inputs.set(slot, book);
			}
			var operation = CelestialGearForgeEnchanting.calculate(inputs, null);
			helper.assertTrue(operation != null && EnchantmentHelper.getEnchantmentsForCrafting(operation.result()).getLevel(sharpness) == 5 + limit,
				"Equal levels combine sequentially beyond vanilla caps for each tier");
			helper.assertTrue(operation.energyPerTick() == 10 * (5 + limit) * (5 + limit), "Only changed enchantments contribute to quadratic cost");
			helper.assertTrue(operation.result().get(DataComponents.CUSTOM_NAME).equals(target.get(DataComponents.CUSTOM_NAME))
				&& EnchantmentHelper.getEnchantmentsForCrafting(operation.result()).getLevel(unbreaking) == 3, "Unchanged enchants and custom names survive");
			if (limit < 7) {
				inputs.set(limit + 1, inputs.get(1).copy());
				helper.assertTrue(CelestialGearForgeEnchanting.calculate(inputs, null) == null, "Oversized batch is rejected");
			}
		}
		var inputs = NonNullList.withSize(CelestialGearForgeBlockEntity.INPUT_COUNT, ItemStack.EMPTY);
		inputs.set(8, new ItemStack(CrystalnexusModItems.YELLOW_DWARF_STAR.get()));
		ItemStack gear = new ItemStack(Items.DIAMOND_SWORD);
		gear.setDamageValue(23);
		gear.set(DataComponents.CUSTOM_NAME, Component.literal("Named sword"));
		inputs.set(0, gear);
		ItemStack donor = new ItemStack(Items.ENCHANTED_BOOK);
		EnchantmentHelper.updateEnchantments(donor, mutable -> { mutable.set(sharpness, 5); mutable.set(protection, 4); });
		inputs.set(1, donor);
		var applied = CelestialGearForgeEnchanting.calculate(inputs, null);
		helper.assertTrue(applied != null && applied.result().getDamageValue() == 23
			&& EnchantmentHelper.getEnchantmentsForCrafting(applied.result()).getLevel(sharpness) == 5
			&& EnchantmentHelper.getEnchantmentsForCrafting(applied.result()).getLevel(protection) == 0, "Books apply to unenchanted gear and skip inapplicable enchants");
		inputs.set(0, applied.result());
		helper.assertTrue(CelestialGearForgeEnchanting.calculate(inputs, null).energyPerTick() == 360, "Equal V books produce VI at 360 FE/t");
		ItemStack conflict = new ItemStack(Items.ENCHANTED_BOOK);
		EnchantmentHelper.updateEnchantments(conflict, mutable -> mutable.set(smite, 5));
		inputs.set(1, conflict);
		helper.assertTrue(CelestialGearForgeEnchanting.calculate(inputs, null) == null, "Conflicting donor with no improvement is rejected");
		EnchantmentHelper.updateEnchantments(donor, mutable -> mutable.set(sharpness, 10));
		inputs.set(1, donor);
		helper.assertTrue(EnchantmentHelper.getEnchantmentsForCrafting(CelestialGearForgeEnchanting.calculate(inputs, null).result()).getLevel(sharpness) == 10,
			"Unequal levels retain the higher level");
		EnchantmentHelper.updateEnchantments(inputs.get(0), mutable -> mutable.set(sharpness, 254));
		EnchantmentHelper.updateEnchantments(donor, mutable -> mutable.set(sharpness, 254));
		var capped = CelestialGearForgeEnchanting.calculate(inputs, null);
		helper.assertTrue(EnchantmentHelper.getEnchantmentsForCrafting(capped.result()).getLevel(sharpness) == 255 && capped.energyPerTick() == 650250, "Levels clamp to 255");
		inputs.set(0, capped.result());
		EnchantmentHelper.updateEnchantments(donor, mutable -> mutable.set(sharpness, 255));
		helper.assertTrue(CelestialGearForgeEnchanting.calculate(inputs, null) == null, "Capped or ineffective donors are not wasted");
		helper.succeed();
	}

	@GameTest(template = "celestial_gear_forge", templateNamespace = "crystalnexus")
	public static void bookProcessingPowerPauseAndPersistence(GameTestHelper helper) {
		var forge = (CelestialGearForgeBlockEntity) helper.getBlockEntity(forgePos(helper));
		var input = installEnergyInput(helper, forge);
		var energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, input, Direction.UP);
		var sharpness = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
		ItemStack target = new ItemStack(Items.DIAMOND_SWORD), book = new ItemStack(Items.ENCHANTED_BOOK);
		target.setDamageValue(23);
		target.set(DataComponents.CUSTOM_NAME, Component.literal("Keep components"));
		EnchantmentHelper.updateEnchantments(target, mutable -> mutable.set(sharpness, 5));
		EnchantmentHelper.updateEnchantments(book, mutable -> mutable.set(sharpness, 5));
		forge.setItem(0, target.copy());
		forge.setItem(1, book.copy());
		forge.setItem(8, new ItemStack(CrystalnexusModItems.YELLOW_DWARF_STAR.get()));
		tick(forge, 20);
		helper.assertTrue(forge.getProgress() == 0 && forge.getEnergyPerTick() == 360 && forge.getItem(1).getCount() == 1, "Zero FE prevents progress and item consumption");
		energy.receiveEnergy(360 * 50, false);
		tick(forge, 50);
		helper.assertTrue(forge.getProgress() == 50 && energy.getEnergyStored() == 0, "Successful ticks deduct exactly 360 FE");
		tick(forge, 20);
		helper.assertTrue(forge.getProgress() == 50, "Power loss pauses progress");
		var saved = forge.saveWithFullMetadata(helper.getLevel().registryAccess());
		forge.loadWithComponents(saved, helper.getLevel().registryAccess());
		tick(forge, 1);
		helper.assertTrue(forge.getProgress() == 50 && forge.isFormed(), "Combining resumes after reload and port validation");
		energy.receiveEnergy(360 * 150, false);
		tick(forge, 150);
		ItemStack expected = target.copy();
		EnchantmentHelper.updateEnchantments(expected, mutable -> mutable.set(sharpness, 6));
		helper.assertTrue(ItemStack.matches(expected, forge.getItem(9)) && forge.getItem(0).isEmpty() && forge.getItem(1).isEmpty()
			&& forge.getItem(8).getCount() == 1 && energy.getEnergyStored() == 0, "Completion consumes target/book, retains star/components, and deducts exact total FE");
		forge.setItem(9, ItemStack.EMPTY);
		forge.setItem(0, target.copy());
		forge.setItem(1, book.copy());
		energy.receiveEnergy(360 * 100, false);
		tick(forge, 10);
		forge.setItem(9, new ItemStack(Items.DIAMOND));
		tick(forge, 1);
		helper.assertTrue(forge.getProgress() == 0 && forge.getItem(1).getCount() == 1 && energy.getEnergyStored() == 360 * 90, "Blocked output resets without refunding FE or consuming inputs");
		forge.setItem(9, ItemStack.EMPTY);
		tick(forge, 10);
		forge.setItem(1, ItemStack.EMPTY);
		tick(forge, 1);
		helper.assertTrue(forge.getProgress() == 0 && forge.getItem(0).getCount() == 1, "Changed donor resets progress safely");
		var inventory = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, forge.getBlockPos(), Direction.UP);
		helper.assertTrue(inventory != null && inventory.insertItem(1, book.copy(), false).isEmpty(), "Automation accepts donor books");
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(forge.getBlockPos().getCenter());
		var menu = new CelestialGearForgeMenu(1, player.getInventory(), forge);
		player.containerMenu = menu;
		helper.assertTrue(!menu.clickMenuButton(player, 1), "Combining disables enchantment selection on the server");
		helper.succeed();
	}

	@GameTest(template = "celestial_gear_forge", templateNamespace = "crystalnexus")
	public static void oldInventoryMigrationAndNewSlotLayout(GameTestHelper helper) {
		var pos = forgePos(helper);
		var original = (CelestialGearForgeBlockEntity) helper.getBlockEntity(pos);
		var oldItems = NonNullList.withSize(6, ItemStack.EMPTY);
		for (int i = 0; i < 6; i++) {
			oldItems.set(i, new ItemStack(Items.DIAMOND, i + 1));
			oldItems.get(i).set(DataComponents.CUSTOM_NAME, Component.literal("Old slot " + i));
		}
		var legacy = new CompoundTag();
		ContainerHelper.saveAllItems(legacy, oldItems, helper.getLevel().registryAccess());
		var migrated = new CelestialGearForgeBlockEntity(original.getBlockPos(), original.getBlockState());
		migrated.loadWithComponents(legacy, helper.getLevel().registryAccess());
		int[] newSlots = {0, 6, CelestialGearForgeBlockEntity.STAR_SLOT, 2, 4, CelestialGearForgeBlockEntity.OUTPUT_SLOT};
		for (int i = 0; i < newSlots.length; i++)
			helper.assertTrue(ItemStack.matches(oldItems.get(i), migrated.getItem(newSlots[i])), "Legacy inventory keeps every item, count and component");
		for (int slot : new int[] {1, 3, 5, 7}) helper.assertTrue(migrated.getItem(slot).isEmpty(), "New ingredient slots start empty");
		var saved = migrated.saveWithFullMetadata(helper.getLevel().registryAccess());
		var reloaded = new CelestialGearForgeBlockEntity(original.getBlockPos(), original.getBlockState());
		reloaded.loadWithComponents(saved, helper.getLevel().registryAccess());
		for (int i = 0; i < 10; i++) helper.assertTrue(ItemStack.matches(migrated.getItem(i), reloaded.getItem(i)), "New inventory reload keeps slot positions");
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		var menu = new CelestialGearForgeMenu(1, player.getInventory(), original);
		for (int i = 0; i < 10; i++)
			helper.assertTrue(menu.getSlot(i).x == CelestialGearForgeMenu.SLOT_POSITIONS[i][0] && menu.getSlot(i).y == CelestialGearForgeMenu.SLOT_POSITIONS[i][1],
				"Menu slots match the supplied texture coordinates");
		player.getInventory().setItem(9, new ItemStack(CrystalnexusModItems.YELLOW_DWARF_STAR.get()));
		menu.quickMoveStack(player, 10);
		helper.assertTrue(original.getItem(CelestialGearForgeBlockEntity.STAR_SLOT).is(CrystalnexusModItems.YELLOW_DWARF_STAR.get()), "Shift-click routes stars to the center slot");
		helper.succeed();
	}

	@GameTest(template = "celestial_gear_forge", templateNamespace = "crystalnexus")
	public static void craftingPowerAndRecipePrecedence(GameTestHelper helper) {
		var forge = (CelestialGearForgeBlockEntity) helper.getBlockEntity(forgePos(helper));
		var port = installEnergyInput(helper, forge);
		var energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, port, Direction.UP);
		setInputs(forge);
		tick(forge, 20);
		helper.assertTrue(forge.getProgress() == 0 && forge.getEnergyPerTick() == 100, "Crafting requires FE");
		energy.receiveEnergy(100 * 50, false);
		tick(forge, 50);
		tick(forge, 10);
		helper.assertTrue(forge.getProgress() == 50 && energy.getEnergyStored() == 0, "Crafting deducts 100 FE/t and pauses without power");
		energy.receiveEnergy(100 * 150, false);
		tick(forge, 150);
		helper.assertTrue(forge.getItem(9).is(CrystalnexusModItems.METEOR_SWORD.get()) && forge.getItem(8).isEmpty()
			&& energy.getEnergyStored() == 0, "Crafting completes at exactly 20000 FE and consumes its recipe star");
		forge.setItem(9, ItemStack.EMPTY);
		var sharpness = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
		var ingredients = new java.util.ArrayList<net.minecraft.world.item.crafting.Ingredient>();
		for (int slot = 0; slot < 8; slot++) {
			ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
			int enchantmentLevel = slot == 0 ? 5 : 4 + slot;
			EnchantmentHelper.updateEnchantments(book, mutable -> mutable.set(sharpness, enchantmentLevel));
			forge.setItem(slot, book);
			ingredients.add(net.minecraft.world.item.crafting.Ingredient.of(Items.ENCHANTED_BOOK));
		}
		forge.setItem(8, new ItemStack(CrystalnexusModItems.PINK_STAR.get()));
		ingredients.add(net.minecraft.world.item.crafting.Ingredient.of(CrystalnexusModItems.PINK_STAR.get()));
		var manager = helper.getLevel().getRecipeManager();
		var original = List.copyOf(manager.getRecipes());
		var recipes = new java.util.ArrayList<net.minecraft.world.item.crafting.RecipeHolder<?>>(original);
		recipes.add(new net.minecraft.world.item.crafting.RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("crystalnexus", "test_book_crafting_priority"),
			new CelestialGearForgeRecipe(ingredients, new ItemStack(Items.DIAMOND))));
		try {
			manager.replaceRecipes(recipes);
			energy.receiveEnergy(20000, false);
			tick(forge, 200);
			helper.assertTrue(forge.getItem(9).is(Items.DIAMOND) && forge.getItem(8).isEmpty(), "Ordinary book recipes take priority over valid combining layouts");
		} finally { manager.replaceRecipes(original); }
		helper.succeed();
	}

	@GameTest(template = "celestial_gear_forge", templateNamespace = "crystalnexus")
	public static void selectedEnchantmentUpgradesAndPersistence(GameTestHelper helper) {
		BlockPos pos = forgePos(helper);
		CelestialGearForgeBlockEntity forge = (CelestialGearForgeBlockEntity) helper.getBlockEntity(pos);
		powerForge(helper, forge);
		helper.assertTrue(forge.validateStructureNow(), "Forge structure must validate");
		var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		var sharpness = enchantments.getOrThrow(Enchantments.SHARPNESS);
		var unbreaking = enchantments.getOrThrow(Enchantments.UNBREAKING);
		var stars = List.of(CrystalnexusModItems.YELLOW_DWARF_STAR.get(), CrystalnexusModItems.ORANGE_STAR.get(),
			CrystalnexusModItems.BLUE_STAR.get(), CrystalnexusModItems.PINK_STAR.get());
		for (var item : List.of(Items.DIAMOND_SWORD, Items.ENCHANTED_BOOK)) {
			for (int i = 0; i < stars.size(); i++) {
				ItemStack input = new ItemStack(item);
				input.set(DataComponents.CUSTOM_NAME, Component.literal("Preserved name"));
				if (item == Items.DIAMOND_SWORD) input.setDamageValue(23);
				EnchantmentHelper.updateEnchantments(input, mutable -> { mutable.set(sharpness, 5); mutable.set(unbreaking, 3); });
				forge.setItem(0, input.copy());
				forge.setItem(CelestialGearForgeBlockEntity.STAR_SLOT, new ItemStack(stars.get(i)));
				forge.setItem(CelestialGearForgeBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
				tick(forge, 1);
				helper.assertTrue(forge.data().get(3) == 0, "First enchantment in registry order is selected by default");
				helper.assertTrue(forge.cycleEnchantment(1), "Can select the other enchantment");
				helper.assertTrue(forge.getProgress() == 0, "Changing selection resets progress");
				int boost = 1 << i;
				ItemStack expected = input.copy();
				EnchantmentHelper.updateEnchantments(expected, mutable -> mutable.set(unbreaking, 3 + boost));
				tick(forge, 100);
				assertAnimationUpdate(helper, forge);
				var saved = forge.saveWithFullMetadata(helper.getLevel().registryAccess());
				forge.loadWithComponents(saved, helper.getLevel().registryAccess());
				helper.assertTrue(forge.getProgress() == 100 && forge.data().get(3) == 1, "Progress and selected enchantment survive reload");
				tick(forge, 100);
				helper.assertTrue(ItemStack.isSameItemSameComponents(expected, forge.getItem(CelestialGearForgeBlockEntity.OUTPUT_SLOT)), "Only selected enchantment changes; components and book storage survive");
				helper.assertTrue(forge.getItem(0).isEmpty() && forge.getItem(CelestialGearForgeBlockEntity.STAR_SLOT).getCount() == 1, "Upgrade consumes the target and preserves the star");
			}
		}

		ItemStack nearCap = new ItemStack(Items.ENCHANTED_BOOK);
		EnchantmentHelper.updateEnchantments(nearCap, mutable -> mutable.set(sharpness, 254));
		forge.setItem(0, nearCap.copy());
		forge.setItem(CelestialGearForgeBlockEntity.STAR_SLOT, new ItemStack(CrystalnexusModItems.PINK_STAR.get()));
		forge.setItem(CelestialGearForgeBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
		tick(forge, 200);
		helper.assertTrue(EnchantmentHelper.getEnchantmentsForCrafting(forge.getItem(CelestialGearForgeBlockEntity.OUTPUT_SLOT)).getLevel(sharpness) == 255, "Upgrade clamps at 255");
		ItemStack capped = forge.removeItem(CelestialGearForgeBlockEntity.OUTPUT_SLOT, 1);
		forge.setItem(CelestialGearForgeBlockEntity.STAR_SLOT, new ItemStack(CrystalnexusModItems.PINK_STAR.get()));
		forge.setItem(0, capped);
		tick(forge, 200);
		helper.assertTrue(forge.getProgress() == 0 && forge.getItem(CelestialGearForgeBlockEntity.STAR_SLOT).getCount() == 1 && !forge.getItem(0).isEmpty(), "Capped input consumes nothing");
		forge.setItem(0, new ItemStack(Items.DIAMOND_SWORD));
		tick(forge, 200);
		helper.assertTrue(forge.getItem(CelestialGearForgeBlockEntity.STAR_SLOT).getCount() == 1 && forge.getItem(CelestialGearForgeBlockEntity.OUTPUT_SLOT).isEmpty(), "Unenchanted input consumes nothing");

		forge.setItem(0, nearCap.copy());
		forge.setItem(CelestialGearForgeBlockEntity.OUTPUT_SLOT, new ItemStack(Items.DIAMOND, 64));
		tick(forge, 20);
		helper.assertTrue(forge.getProgress() == 0 && forge.getItem(CelestialGearForgeBlockEntity.STAR_SLOT).getCount() == 1, "Blocked output preserves inputs");
		forge.setItem(CelestialGearForgeBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
		for (int slot = 1; slot < CelestialGearForgeBlockEntity.STAR_SLOT; slot++) {
			forge.setItem(slot, new ItemStack(Items.DIRT));
			tick(forge, 20);
			helper.assertTrue(forge.getProgress() == 0, "Upgrade requires every other ingredient slot empty");
			forge.setItem(slot, ItemStack.EMPTY);
		}
		tick(forge, 50);
		forge.setItem(CelestialGearForgeBlockEntity.STAR_SLOT, new ItemStack(CrystalnexusModItems.BLUE_STAR.get()));
		tick(forge, 1);
		helper.assertTrue(forge.getProgress() == 0 && !forge.getItem(0).isEmpty(), "Changing star interrupts without consuming input");
		tick(forge, 50);
		forge.setItem(CelestialGearForgeBlockEntity.OUTPUT_SLOT, new ItemStack(Items.DIAMOND));
		tick(forge, 1);
		helper.assertTrue(forge.getProgress() == 0 && forge.getItem(CelestialGearForgeBlockEntity.STAR_SLOT).getCount() == 1, "Blocking output interrupts without consuming star");
		forge.setItem(CelestialGearForgeBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
		tick(forge, 50);
		BlockPos casing = anotherStructureBlock(helper, pos);
		var casingState = helper.getLevel().getBlockState(casing);
		helper.getLevel().setBlockAndUpdate(casing, Blocks.AIR.defaultBlockState());
		forge.validateStructureNow();
		tick(forge, 1);
		helper.assertTrue(forge.getProgress() == 0 && !forge.getItem(0).isEmpty(), "Broken structure interrupts upgrade safely");
		helper.getLevel().setBlockAndUpdate(casing, casingState);
		forge.validateStructureNow();

		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(helper.absolutePos(pos).getCenter());
		var menu = new CelestialGearForgeMenu(1, player.getInventory(), forge);
		player.containerMenu = menu;
		helper.assertTrue(!menu.clickMenuButton(player, 2), "Unknown menu button rejected");
		helper.assertTrue(!menu.clickMenuButton(player, 1), "Selection request rejected when only one choice exists");
		ItemStack multiple = nearCap.copy();
		EnchantmentHelper.updateEnchantments(multiple, mutable -> mutable.set(unbreaking, 3));
		forge.setItem(0, multiple);
		tick(forge, 1);
		helper.assertTrue(menu.clickMenuButton(player, 1) && forge.data().get(3) == 1, "Valid open-menu selection accepted by server");
		player.containerMenu = player.inventoryMenu;
		helper.assertTrue(!menu.clickMenuButton(player, 0), "Closed-menu selection rejected");
		forge.setItem(0, ItemStack.EMPTY);
		var inventory = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), Direction.UP);
		helper.assertTrue(inventory != null && inventory.insertItem(0, nearCap.copy(), false).isEmpty(), "Automation accepts enchanted books");
		helper.succeed();
	}

	@GameTest(template = "celestial_gear_forge", templateNamespace = "crystalnexus")
	public static void recipesProcessingStructureAndAutomation(GameTestHelper helper) {
		BlockPos pos = forgePos(helper);
		CelestialGearForgeBlockEntity forge = (CelestialGearForgeBlockEntity) helper.getBlockEntity(pos);
		powerForge(helper, forge);
		helper.assertTrue(forge.validateStructureNow(), "Supplied celestial_gear_forge structure must validate");
		var recipe = helper.getLevel().getRecipeManager().getAllRecipesFor(CelestialGearForgeRecipe.Type.INSTANCE).stream()
			.filter(holder -> holder.id().getPath().equals("meteor_sword_recipe")).map(holder -> holder.value()).findFirst().orElse(null);
		helper.assertTrue(recipe != null, "Meteor sword forge recipe must load");
		if (recipe == null) { helper.fail("Missing meteor sword recipe"); return; }
		var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
		try {
			CelestialGearForgeRecipe.Serializer.INSTANCE.streamCodec().encode(buffer, recipe);
			var decoded = CelestialGearForgeRecipe.Serializer.INSTANCE.streamCodec().decode(buffer);
			setInputs(forge);
			helper.assertTrue(decoded.matches(java.util.stream.IntStream.range(0, CelestialGearForgeBlockEntity.INPUT_COUNT).mapToObj(forge::getItem).toList()),
				"Recipe network round trip preserves eight ingredients and the star");
		} finally { buffer.release(); }

		for (var star : List.of(CrystalnexusModItems.YELLOW_DWARF_STAR.get(), CrystalnexusModItems.ORANGE_STAR.get(),
			CrystalnexusModItems.BLUE_STAR.get(), CrystalnexusModItems.PINK_STAR.get())) {
			setInputs(forge);
			forge.setItem(CelestialGearForgeBlockEntity.STAR_SLOT, new ItemStack(star));
			var inputs = java.util.stream.IntStream.range(0, CelestialGearForgeBlockEntity.INPUT_COUNT).mapToObj(forge::getItem).toList();
			helper.assertTrue(recipe.matches(inputs), "All active stars satisfy the eight-ingredient recipe");
			forge.setItem(7, ItemStack.EMPTY);
			helper.assertTrue(!recipe.matches(java.util.stream.IntStream.range(0, CelestialGearForgeBlockEntity.INPUT_COUNT).mapToObj(forge::getItem).toList()),
				"Crafting requires all eight ingredient slots");
		}

		setInputs(forge);
		tick(forge, 100);
		assertAnimationUpdate(helper, forge);
		helper.assertTrue(forge.getProgress() == 100, "Craft should be halfway after 100 ticks");
		forge.setItem(0, new ItemStack(Items.STONE));
		tick(forge, 1);
		helper.assertTrue(forge.getProgress() == 0 && forge.getItem(0).is(Items.STONE), "Changed input resets progress without consuming items");
		forge.setItem(0, new ItemStack(CrystalnexusModItems.METEORITE_ALLOY.get()));
		tick(forge, CelestialGearForgeBlockEntity.DURATION);
		helper.assertTrue(forge.getItem(CelestialGearForgeBlockEntity.OUTPUT_SLOT).is(CrystalnexusModItems.METEOR_SWORD.get())
			&& forge.getItem(CelestialGearForgeBlockEntity.OUTPUT_SLOT).getCount() == 1, "Completed recipe produces one meteor sword");
		for (int slot = 0; slot < CelestialGearForgeBlockEntity.INPUT_COUNT; slot++)
			helper.assertTrue(forge.getItem(slot).isEmpty(), "All eight ingredients and the star, are consumed");

		forge.setItem(CelestialGearForgeBlockEntity.OUTPUT_SLOT, new ItemStack(Items.DIAMOND, 64));
		setInputs(forge);
		tick(forge, 20);
		helper.assertTrue(forge.getProgress() == 0 && forge.getItem(0).is(CrystalnexusModItems.METEORITE_ALLOY.get()), "Full output retains the inputs and prevents processing");

		BlockPos casing = anotherStructureBlock(helper, pos);
		var originalState = helper.getLevel().getBlockState(casing);
		helper.getLevel().setBlockAndUpdate(casing, Blocks.AIR.defaultBlockState());
		helper.assertTrue(!forge.validateStructureNow(), "Broken multiblock prevents crafting");
		tick(forge, 1);
		helper.assertTrue(forge.getProgress() == 0, "Unformed forge makes no progress");
		helper.getLevel().setBlockAndUpdate(casing, originalState);
		helper.assertTrue(forge.validateStructureNow(), "Restoring the multiblock allows processing again");

		IItemHandler inventory = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), Direction.UP);
		helper.assertTrue(inventory != null && inventory.getSlots() == 10, "All sides expose the ten-slot item handler");
		if (inventory != null) {
			forge.setItem(CelestialGearForgeBlockEntity.STAR_SLOT, ItemStack.EMPTY);
			helper.assertTrue(inventory.insertItem(CelestialGearForgeBlockEntity.STAR_SLOT, new ItemStack(CrystalnexusModItems.BLUE_STAR.get()), false).isEmpty(), "Star slot accepts active stars");
			ItemStack rejected = inventory.insertItem(CelestialGearForgeBlockEntity.OUTPUT_SLOT, new ItemStack(Items.DIAMOND), false);
			helper.assertTrue(!rejected.isEmpty(), "Automation cannot insert into the output slot");
			ItemStack extracted = inventory.extractItem(CelestialGearForgeBlockEntity.STAR_SLOT, 1, false);
			helper.assertTrue(extracted.isEmpty() && forge.getItem(CelestialGearForgeBlockEntity.STAR_SLOT).is(CrystalnexusModItems.BLUE_STAR.get()), "Automation extracts only from the output slot");
			forge.setItem(CelestialGearForgeBlockEntity.OUTPUT_SLOT, new ItemStack(Items.DIAMOND));
			helper.assertTrue(inventory.extractItem(CelestialGearForgeBlockEntity.OUTPUT_SLOT, 1, false).is(Items.DIAMOND), "Automation can extract the output");
		}
		helper.succeed();
	}
}
