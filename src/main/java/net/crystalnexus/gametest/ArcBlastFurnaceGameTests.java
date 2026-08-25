package net.crystalnexus.gametest;

import net.crystalnexus.block.HeatingCoreBlock;
import net.crystalnexus.block.entity.ArcFurnaceBlockEntity;
import net.crystalnexus.block.entity.MachineEnergyInputBlockEntity;
import net.crystalnexus.block.entity.MultiblockItemInputBlockEntity;
import net.crystalnexus.block.entity.MultiblockItemOutputBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.jei_recipes.ArcFurnaceRecipe;
import net.crystalnexus.procedures.ArcFurnaceOnTickUpdateProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class ArcBlastFurnaceGameTests {
    private ArcBlastFurnaceGameTests() {}

    private record ExpectedRecipe(List<Item> inputs, Item output, int count) {}

    @GameTest(template = "arc_blast_furnace")
    public static void validatesPortsAndActivatesHeatingCores(GameTestHelper helper) {
        BlockPos controllerPos = find(helper, CrystalnexusModBlocks.ARC_FURNACE.get()).getFirst();
        List<BlockPos> casing = find(helper, CrystalnexusModBlocks.TITANIUM_CARBIDE_BLOCK.get());
        List<BlockPos> cores = find(helper, CrystalnexusModBlocks.HEATING_CORE.get());
        helper.assertTrue(casing.size() >= 3 && !cores.isEmpty(),
            "The Arc Blast Furnace template needs three carbide casing positions and at least one Heating Core");

        BlockState controllerState = helper.getBlockState(controllerPos);
        helper.setBlock(controllerPos, Blocks.AIR);
        helper.setBlock(controllerPos, controllerState);
        ArcFurnaceBlockEntity controller = helper.getBlockEntity(controllerPos);
        helper.assertTrue(!controller.validateStructureNow(), "The structure must require a Machine Energy Input");

        helper.setBlock(casing.get(0), CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get());
        helper.assertTrue(controller.validateStructureNow(), "Item ports must be optional");
        helper.setBlock(casing.get(1), CrystalnexusModBlocks.MULTIBLOCK_ITEM_INPUT.get());
        helper.setBlock(casing.get(2), CrystalnexusModBlocks.MULTIBLOCK_ITEM_OUTPUT.get());
        helper.assertTrue(controller.validateStructureNow(), "All three port types must be valid carbide substitutions");

        MachineEnergyInputBlockEntity energy = helper.getBlockEntity(casing.get(0));
        MultiblockItemInputBlockEntity input = helper.getBlockEntity(casing.get(1));
        MultiblockItemOutputBlockEntity output = helper.getBlockEntity(casing.get(2));
        energy.getEnergyStorage().receiveEnergy(4096, false);
        input.setItem(0, new ItemStack(CrystalnexusModItems.TITANIUM_INGOT.get()));
        controller.setItem(2, new ItemStack(CrystalnexusModItems.TITANIUM_CARBIDE_INGOT.get()));
        controller.prepareForProcessing(helper.getLevel());
        helper.assertTrue(controller.getEnergyStorage().getEnergyStored() == 4096,
            "The energy port must relay FE into the controller");
        helper.assertTrue(controller.getItem(0).is(CrystalnexusModItems.TITANIUM_INGOT.get()),
            "The item input must feed a controller input slot");
        helper.assertTrue(output.getItem(0).is(CrystalnexusModItems.TITANIUM_CARBIDE_INGOT.get()),
            "The item output must receive the controller output");

        controller.setHeatingCoresActive(true);
        helper.assertTrue(cores.stream().allMatch(pos -> helper.getBlockState(pos).getValue(HeatingCoreBlock.LIT)),
            "Every Heating Core must use its active texture while smelting");
        helper.succeed();
    }

    @GameTest(template = "arc_blast_furnace")
    public static void loadsAndProcessesExpandedRecipePack(GameTestHelper helper) {
        Map<String, RecipeHolder<ArcFurnaceRecipe>> loaded = helper.getLevel().getRecipeManager()
            .getAllRecipesFor(ArcFurnaceRecipe.Type.INSTANCE).stream()
            .collect(Collectors.toMap(holder -> holder.id().getPath(), holder -> holder));
        Map<String, ExpectedRecipe> expected = Map.ofEntries(
            Map.entry("arc_silicon", recipe(CrystalnexusModItems.SILICON.get(), 2,
                Items.QUARTZ, CrystalnexusModItems.RAW_CARBON.get())),
            Map.entry("arc_conductive_alloy", recipe(CrystalnexusModItems.CONDUCTIVE_ALLOY.get(), 2,
                Items.REDSTONE, CrystalnexusModItems.RAW_CARBON.get())),
            Map.entry("arc_crystalized_alloy", recipe(CrystalnexusModItems.CRYSTALIZED_ALLOY.get(), 3,
                CrystalnexusModItems.ANCIENT_CRYSTAL.get(), Items.IRON_INGOT)),
            Map.entry("arc_energized_silicon", recipe(CrystalnexusModItems.ENERGIZED_SILICON.get(), 1,
                CrystalnexusModItems.SILICON.get(), CrystalnexusModItems.CONDUCTIVE_ALLOY.get())),
            Map.entry("arc_carbon_composite", recipe(CrystalnexusModItems.CARBON_COMPOSITE.get(), 3,
                Items.NETHERITE_SCRAP, CrystalnexusModItems.INVERTIUM_DUST.get())),
            Map.entry("arc_chlorophyte_dust", recipe(CrystalnexusModItems.CHLOROPHYTE_INGOT.get(), 2,
                CrystalnexusModItems.CHLOROPHYTE_DUST.get())),
            Map.entry("arc_invertium_dust", recipe(CrystalnexusModItems.INVERTIUM_INGOT.get(), 2,
                CrystalnexusModItems.INVERTIUM_DUST.get())),
            Map.entry("arc_recycle_iron_sheet", recipe(Items.IRON_INGOT, 2, CrystalnexusModItems.IRON_SHEET.get())),
            Map.entry("arc_recycle_iron_rod", recipe(Items.IRON_INGOT, 2, CrystalnexusModItems.IRON_ROD.get())),
            Map.entry("arc_recycle_titanium_sheet", recipe(CrystalnexusModItems.TITANIUM_INGOT.get(), 2,
                CrystalnexusModItems.TITANIUM_SHEET.get())),
            Map.entry("arc_recycle_titanium_rod", recipe(CrystalnexusModItems.TITANIUM_INGOT.get(), 2,
                CrystalnexusModItems.TITANIUM_ROD.get())),
            Map.entry("arc_recycle_copper_sheet", recipe(Items.COPPER_INGOT, 2,
                CrystalnexusModItems.COPPER_SHEET.get())),
            Map.entry("arc_recycle_copper_rod", recipe(Items.COPPER_INGOT, 2,
                CrystalnexusModItems.COPPER_ROD.get())),
            Map.entry("arc_recycle_gold_sheet", recipe(Items.GOLD_INGOT, 2,
                CrystalnexusModItems.GOLD_SHEET.get())),
            Map.entry("arc_recycle_gold_rod", recipe(Items.GOLD_INGOT, 2,
                CrystalnexusModItems.GOLD_ROD.get())),
            Map.entry("arc_recycle_carbon_fiber_rod", recipe(CrystalnexusModItems.CARBON_FIBER.get(), 2,
                CrystalnexusModItems.CARBON_FIBER_ROD.get()))
        );

        helper.assertTrue(loaded.size() == 19, "The Arc Blast Furnace must load its three original and sixteen new recipes");
        for (Map.Entry<String, ExpectedRecipe> entry : expected.entrySet()) {
            RecipeHolder<ArcFurnaceRecipe> holder = loaded.get(entry.getKey());
            helper.assertTrue(holder != null && matches(entry.getValue(), holder.value(), helper),
                "Arc furnace recipe has incorrect inputs or output: " + entry.getKey());
        }

        BlockPos controllerPos = find(helper, CrystalnexusModBlocks.ARC_FURNACE.get()).getFirst();
        List<BlockPos> casing = find(helper, CrystalnexusModBlocks.TITANIUM_CARBIDE_BLOCK.get());
        BlockState controllerState = helper.getBlockState(controllerPos);
        helper.setBlock(controllerPos, Blocks.AIR);
        helper.setBlock(controllerPos, controllerState);
        helper.setBlock(casing.getFirst(), CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get());
        ArcFurnaceBlockEntity controller = helper.getBlockEntity(controllerPos);
        helper.assertTrue(controller.validateStructureNow(), "Arc Blast Furnace must form for recipe processing tests");

        process(helper, controllerPos, controller, new ItemStack(CrystalnexusModItems.GOLD_SHEET.get()), ItemStack.EMPTY,
            Items.GOLD_INGOT, 2);
        process(helper, controllerPos, controller, new ItemStack(CrystalnexusModItems.RAW_CARBON.get()),
            new ItemStack(Items.QUARTZ), CrystalnexusModItems.SILICON.get(), 2);
        process(helper, controllerPos, controller, ItemStack.EMPTY,
            new ItemStack(CrystalnexusModItems.TITANIUM_SHEET.get()), CrystalnexusModItems.TITANIUM_INGOT.get(), 2);
        process(helper, controllerPos, controller, new ItemStack(CrystalnexusModItems.TUNGSTEN_DUST.get()),
            ItemStack.EMPTY, CrystalnexusModItems.HOT_TUNGSTEN.get(), 1);

        controller.setItem(0, new ItemStack(Items.QUARTZ));
        controller.setItem(1, new ItemStack(CrystalnexusModItems.RAW_CARBON.get()));
        controller.setItem(2, new ItemStack(CrystalnexusModItems.SILICON.get(), 63));
        refillEnergy(controller);
        int energyBefore = controller.availableEnergy();
        for (int tick = 0; tick < 100; tick++)
            ArcFurnaceOnTickUpdateProcedure.execute(helper.getLevel(), helper.absolutePos(controllerPos));
        helper.assertTrue(controller.getItem(0).is(Items.QUARTZ) && controller.getItem(1).is(CrystalnexusModItems.RAW_CARBON.get())
                && controller.getItem(2).getCount() == 63 && controller.availableEnergy() == energyBefore,
            "A two-item output must not consume inputs or energy when only one output slot remains");
        helper.succeed();
    }

    private static ExpectedRecipe recipe(Item output, int count, Item... inputs) {
        return new ExpectedRecipe(List.of(inputs), output, count);
    }

    private static boolean matches(ExpectedRecipe expected, ArcFurnaceRecipe recipe, GameTestHelper helper) {
        if (recipe.getIngredients().size() != expected.inputs().size()) return false;
        List<Ingredient> unmatched = new ArrayList<>(recipe.getIngredients());
        for (Item item : expected.inputs()) {
            int index = -1;
            for (int i = 0; i < unmatched.size(); i++) if (unmatched.get(i).test(new ItemStack(item))) {
                index = i;
                break;
            }
            if (index < 0) return false;
            unmatched.remove(index);
        }
        ItemStack output = recipe.getResultItem(helper.getLevel().registryAccess());
        return output.is(expected.output()) && output.getCount() == expected.count();
    }

    private static void process(GameTestHelper helper, BlockPos pos, ArcFurnaceBlockEntity controller,
            ItemStack first, ItemStack second, Item output, int count) {
        controller.setItem(0, first);
        controller.setItem(1, second);
        controller.setItem(2, ItemStack.EMPTY);
        refillEnergy(controller);
        for (int tick = 0; tick < 100; tick++)
            ArcFurnaceOnTickUpdateProcedure.execute(helper.getLevel(), helper.absolutePos(pos));
        helper.assertTrue(controller.getItem(0).isEmpty() && controller.getItem(1).isEmpty()
                && controller.getItem(2).is(output) && controller.getItem(2).getCount() == count,
            "Arc Blast Furnace did not consume one of each input and produce " + count + " "
                + output);
    }

    private static void refillEnergy(ArcFurnaceBlockEntity controller) {
        controller.getEnergyStorage().extractEnergy(Integer.MAX_VALUE, false);
        controller.getEnergyStorage().receiveEnergy(Integer.MAX_VALUE, false);
    }

    private static List<BlockPos> find(GameTestHelper helper, Block block) {
        List<BlockPos> found = new ArrayList<>();
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (helper.getBlockState(pos).is(block)) found.add(pos);
        }
        return found;
    }
}
