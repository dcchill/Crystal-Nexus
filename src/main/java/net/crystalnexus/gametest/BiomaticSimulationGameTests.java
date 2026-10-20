package net.crystalnexus.gametest;

import io.netty.buffer.Unpooled;
import net.crystalnexus.block.BiomaticSimulatorBlock;
import net.crystalnexus.block.entity.BiomaticSimulatorBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.util.MachineUpgradeHelper;
import net.crystalnexus.jei_recipes.BiomaticSimulationRecipe;
import net.crystalnexus.processing.BiomaticSimulationDefaults;
import net.crystalnexus.procedures.BiomaticSimulatorOnTickUpdateProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@GameTestHolder("crystalnexus_biomatic")
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid = "crystalnexus")
public final class BiomaticSimulationGameTests {
    private BiomaticSimulationGameTests() {}

    // Fixtures only exist in the focused GameTest server, never in player worlds.
    @SubscribeEvent
    public static void fixtures(RegisterEvent event) {
        if (!List.of(System.getProperty("neoforge.enabledGameTestNamespaces", "").split(","))
                .contains("crystalnexus_biomatic")) return;
        event.register(Registries.BLOCK, helper -> {
            helper.register(id("crop"), new CropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)) {});
            helper.register(id("ambiguous_crop"), new CropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)) {});
            helper.register(id("sapling"), new SaplingBlock(TreeGrower.OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)) {});
            helper.register(id("mushroom"), new MushroomBlock(TreeFeatures.HUGE_BROWN_MUSHROOM,
                BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM)) {});
            var ambiguous = new TreeGrower("crystalnexus_biomatic_ambiguous", 0.5F,
                Optional.empty(), Optional.empty(), Optional.of(TreeFeatures.OAK), Optional.of(TreeFeatures.BIRCH),
                Optional.empty(), Optional.empty());
            helper.register(id("ambiguous_sapling"), new SaplingBlock(ambiguous, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)) {});
        });
        event.register(Registries.ITEM, helper -> {
            for (String name : List.of("crop", "ambiguous_crop", "sapling", "mushroom", "ambiguous_sapling"))
                helper.register(id(name), new BlockItem(BuiltInRegistries.BLOCK.get(id(name)), new Item.Properties()));
        });
    }

    @GameTest(template = "zero_point")
    public static void discoversMainProducts(GameTestHelper helper) {
        expect(helper, Items.WHEAT_SEEDS, Items.WHEAT);
        expect(helper, Items.BEETROOT_SEEDS, Items.BEETROOT);
        expect(helper, Items.CARROT, Items.CARROT);
        expect(helper, Items.POTATO, Items.POTATO);
        expect(helper, Items.TORCHFLOWER_SEEDS, Items.TORCHFLOWER);
        expect(helper, Items.OAK_SAPLING, Items.OAK_LOG);
        expect(helper, Items.DARK_OAK_SAPLING, Items.DARK_OAK_LOG);
        expect(helper, Items.MANGROVE_PROPAGULE, Items.MANGROVE_LOG);
        expect(helper, Items.BROWN_MUSHROOM, Items.BROWN_MUSHROOM_BLOCK);
        expect(helper, Items.CRIMSON_FUNGUS, Items.CRIMSON_STEM);
        expect(helper, Items.PUMPKIN_SEEDS, Items.PUMPKIN);
        expect(helper, Items.SUGAR_CANE, Items.SUGAR_CANE);
        expect(helper, Items.COCOA_BEANS, Items.COCOA_BEANS);
        expect(helper, Items.NETHER_WART, Items.NETHER_WART);
        expect(helper, BuiltInRegistries.ITEM.get(id("crop")), Items.DIAMOND);
        expect(helper, BuiltInRegistries.ITEM.get(id("sapling")), Items.OAK_LOG);
        expect(helper, BuiltInRegistries.ITEM.get(id("mushroom")), Items.BROWN_MUSHROOM_BLOCK);
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void rejectsAmbiguousAndNonplantInputs(GameTestHelper helper) {
        for (Item input : List.of(Items.STONE, BuiltInRegistries.ITEM.get(id("ambiguous_crop")),
                BuiltInRegistries.ITEM.get(id("ambiguous_sapling")))) {
            helper.assertTrue(BiomaticSimulationDefaults.discover(helper.getLevel(), (BlockItem) input).isEmpty(),
                "Must not guess a product for " + input);
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void overridesReloadAndSyncStayConsistent(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var manager = server.getRecipeManager();
        var original = List.copyOf(manager.getRecipes());
        try {
            BiomaticSimulationDefaults.rebuild(server);
            helper.assertTrue(matches(helper, Items.WHEAT_SEEDS).size() == 1, "Generate one wheat recipe");
            var beforeJoin = List.copyOf(manager.getRecipes());
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
            BiomaticSimulationDefaults.sync(new OnDatapackSyncEvent(server.getPlayerList(), player));
            helper.assertTrue(beforeJoin.equals(List.copyOf(manager.getRecipes())), "Joining must not duplicate or regenerate recipes");
            var reloaded = new ArrayList<RecipeHolder<?>>(original);
            reloaded.add(new RecipeHolder<>(id("override"), recipe(Items.WHEAT_SEEDS, new ItemStack(Items.EMERALD, 6))));
            manager.replaceRecipes(reloaded);
            BiomaticSimulationDefaults.sync(new OnDatapackSyncEvent(server.getPlayerList(), null));
            var overridden = matches(helper, Items.WHEAT_SEEDS);
            helper.assertTrue(overridden.size() == 1 && overridden.getFirst().getResultItem(null).is(Items.EMERALD),
                "Reloaded explicit recipes must replace generated outputs");
            helper.assertTrue(matches(helper, Items.BAMBOO).getFirst().getResultItem(null).is(Items.BAMBOO_BLOCK),
                "Keep the existing bamboo recipe");
            manager.replaceRecipes(original);
            BiomaticSimulationDefaults.sync(new OnDatapackSyncEvent(server.getPlayerList(), null));
            helper.assertTrue(matches(helper, Items.WHEAT_SEEDS).getFirst().getResultItem(null).is(Items.WHEAT),
                "Removing an override on reload must restore discovery");
            var generated = matches(helper, BuiltInRegistries.ITEM.get(id("crop")));
            helper.assertTrue(generated.size() == 1 && generated.getFirst().getResultItem(null).getCount() == 6,
                "Registered modded fixtures must become normal recipes with six outputs");
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                BiomaticSimulationRecipe.Serializer.STREAM_CODEC.encode(buffer, generated.getFirst());
                var clientRecipe = BiomaticSimulationRecipe.Serializer.STREAM_CODEC.decode(buffer);
                helper.assertTrue(clientRecipe.getIngredients().getFirst().test(new ItemStack(BuiltInRegistries.ITEM.get(id("crop"))))
                        && clientRecipe.getResultItem(null).is(Items.DIAMOND) && clientRecipe.getResultItem(null).getCount() == 6,
                    "Generated recipes must reach clients through the existing recipe codec");
            } finally {
                buffer.release();
            }
        } finally {
            manager.replaceRecipes(original);
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void processingPreservesResourcesAndStackComponents(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        var original = List.copyOf(manager.getRecipes());
        try {
            manager.replaceRecipes(List.of(new RecipeHolder<>(id("processing"), recipe(Items.WHEAT_SEEDS, new ItemStack(Items.WHEAT, 6)))));
            BlockPos pos = new BlockPos(2, 2, 2);
            helper.setBlock(pos, CrystalnexusModBlocks.BIOMATIC_SIMULATOR.get().defaultBlockState()
                .setValue(BiomaticSimulatorBlock.BLOCKSTATE, 2));
            BiomaticSimulatorBlockEntity machine = helper.getBlockEntity(pos);
            machine.setItem(0, new ItemStack(Items.WHEAT_SEEDS, 2));
            machine.getPersistentData().putDouble("progress", 400);
            tick(helper, pos);
            helper.assertTrue(machine.getItem(0).getCount() == 2 && machine.getItem(1).isEmpty(), "No craft without energy");
            for (int i = 0; i < 32; i++) machine.getEnergyStorage().receiveEnergy(8192, false);
            int energy = machine.getEnergyStorage().getEnergyStored();
            machine.setItem(1, new ItemStack(Items.WHEAT, 59));
            tick(helper, pos);
            helper.assertTrue(machine.getItem(0).getCount() == 2 && machine.getEnergyStorage().getEnergyStored() == energy,
                "A full output must not consume energy or inputs");
            var named = new ItemStack(Items.WHEAT);
            named.set(DataComponents.CUSTOM_NAME, Component.literal("Keep me"));
            machine.setItem(1, named);
            tick(helper, pos);
            helper.assertTrue(ItemStack.matches(machine.getItem(1), named) && machine.getItem(0).getCount() == 2,
                "Must not overwrite different output components");
            machine.setItem(1, new ItemStack(Items.WHEAT, 58));
            tick(helper, pos);
            helper.assertTrue(machine.getItem(0).getCount() == 1 && machine.getItem(1).getCount() == 64
                    && machine.getEnergyStorage().getEnergyStored() == energy - 8192,
                "Consume one input and 8192 FE to produce six items");
            var limited = new ItemStack(Items.WHEAT, 6);
            limited.set(DataComponents.MAX_STACK_SIZE, 8);
            for (int i = 0; i < 4; i++) machine.getEnergyStorage().receiveEnergy(8192, false);
            manager.replaceRecipes(List.of(new RecipeHolder<>(id("limited"), recipe(Items.WHEAT_SEEDS, limited))));
            machine.setItem(1, limited.copyWithCount(3));
            machine.getPersistentData().putDouble("progress", 400);
            tick(helper, pos);
            helper.assertTrue(machine.getItem(0).getCount() == 1 && machine.getItem(1).getCount() == 3, "Respect small output stack limits");
            machine.setItem(1, limited.copyWithCount(2));
            tick(helper, pos);
            helper.assertTrue(machine.getItem(0).isEmpty() && machine.getItem(1).getCount() == 8
                    && machine.getItem(1).getMaxStackSize() == 8, "Preserve recipe components when filling the last available space");
        } finally {
            manager.replaceRecipes(original);
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void allThreeUpgradeSlotsPersistAndAffectProcessing(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        var original = List.copyOf(manager.getRecipes());
        try {
            manager.replaceRecipes(List.of(new RecipeHolder<>(id("upgrades"), recipe(Items.WHEAT_SEEDS, new ItemStack(Items.WHEAT, 6)))));
            BlockPos pos = new BlockPos(2, 2, 2);
            helper.setBlock(pos, CrystalnexusModBlocks.BIOMATIC_SIMULATOR.get().defaultBlockState()
                .setValue(BiomaticSimulatorBlock.BLOCKSTATE, 2));
            BiomaticSimulatorBlockEntity machine = helper.getBlockEntity(pos);
            var items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), Direction.UP);
            helper.assertTrue(machine.getContainerSize() == 5 && MachineUpgradeHelper.upgradeSlots(machine.getBlockState()) == 3,
                "Simulator must have two material slots and three upgrade slots");
            for (int slot = 2; slot < 5; slot++) {
                helper.assertTrue(items.insertItem(slot, new ItemStack(Items.STONE), false).getCount() == 1,
                    "All upgrade slots must reject ordinary items");
                helper.assertTrue(items.insertItem(slot, new ItemStack(CrystalnexusModItems.ACCELERATION_UPGRADE.get(), 64), false).getCount() == 48,
                    "All upgrade slots must enforce upgrade stack limits");
                helper.assertTrue(items.extractItem(slot, 16, false).isEmpty(), "Automation must not extract upgrades");
                machine.setItem(slot, ItemStack.EMPTY);
            }
            machine.setItem(2, new ItemStack(CrystalnexusModItems.ACCELERATION_UPGRADE.get(), 16));
            machine.setItem(3, new ItemStack(CrystalnexusModItems.FE_EFFICIENCY_UPGRADE.get(), 16));
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
            var menu = machine.createMenu(0, player.getInventory());
            helper.assertTrue(menu.slots.size() == 41 && menu.slots.subList(2, 5).stream().allMatch(slot -> slot.isActive()),
                "Menu must expose three active upgrade slots");
            player.getInventory().setItem(0, new ItemStack(CrystalnexusModItems.PARALLELIZATION_CHIP.get()));
            menu.quickMoveStack(player, 32);
            helper.assertTrue(machine.getItem(4).is(CrystalnexusModItems.PARALLELIZATION_CHIP.get()) && player.getInventory().getItem(0).isEmpty(),
                "Shift-click must reach the third upgrade slot");
            var saved = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
            var restored = new BiomaticSimulatorBlockEntity(machine.getBlockPos(), machine.getBlockState());
            restored.loadWithComponents(saved, helper.getLevel().registryAccess());
            for (int slot = 2; slot < 5; slot++)
                helper.assertTrue(ItemStack.matches(machine.getItem(slot), restored.getItem(slot)), "All three upgrades must survive save/load");
            machine.setItem(0, new ItemStack(Items.WHEAT_SEEDS, 2));
            for (int i = 0; i < 8; i++) machine.getEnergyStorage().receiveEnergy(8192, false);
            int energy = machine.getEnergyStorage().getEnergyStored();
            int cost = MachineUpgradeHelper.energyCost(MachineUpgradeHelper.upgrades(machine, 2, 3), 8192);
            machine.getPersistentData().putDouble("progress", 400);
            tick(helper, pos);
            helper.assertTrue(machine.getItem(0).isEmpty() && machine.getItem(1).getCount() == 12
                    && machine.getEnergyStorage().getEnergyStored() == energy - 2 * cost
                    && machine.getPersistentData().getDouble("maxProgress") < 400,
                "Speed, efficiency and parallel upgrades must all affect the same operation");
            int before = player.getInventory().countItem(Items.WHEAT);
            menu.quickMoveStack(player, 1);
            helper.assertTrue(machine.getItem(1).isEmpty() && player.getInventory().countItem(Items.WHEAT) == before + 12,
                "Output shift-click must still reach the player inventory after adding upgrade slots");
        } finally {
            manager.replaceRecipes(original);
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void energizedSiliconTransmutationProducesFour(GameTestHelper helper) {
        var recipe = helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse("crystalnexus:energized_silicon_recipe"))
            .orElseThrow().value();
        var result = recipe.getResultItem(helper.getLevel().registryAccess());
        helper.assertTrue(result.is(CrystalnexusModItems.ENERGIZED_SILICON.get()) && result.getCount() == 4,
            "Matter transmutation must produce four Energized Silicon");
        helper.succeed();
    }

    private static void tick(GameTestHelper helper, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        BiomaticSimulatorOnTickUpdateProcedure.execute(helper.getLevel(), absolute.getX(), absolute.getY(), absolute.getZ());
    }

    private static BiomaticSimulationRecipe recipe(Item input, ItemStack output) {
        return new BiomaticSimulationRecipe(output, NonNullList.of(Ingredient.EMPTY, Ingredient.of(input)));
    }

    private static List<BiomaticSimulationRecipe> matches(GameTestHelper helper, Item input) {
        return helper.getLevel().getRecipeManager().getAllRecipesFor(BiomaticSimulationRecipe.Type.INSTANCE).stream()
            .map(RecipeHolder::value).filter(recipe -> recipe.getIngredients().getFirst().test(new ItemStack(input))).toList();
    }

    private static void expect(GameTestHelper helper, Item input, Item expected) {
        ItemStack actual = BiomaticSimulationDefaults.discover(helper.getLevel(), (BlockItem) input);
        helper.assertTrue(actual.is(expected), "Expected " + input + " -> " + expected + ", got " + actual);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("crystalnexus_biomatic", path);
    }
}
