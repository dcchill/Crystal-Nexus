package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.FluidChemicalReactionChamberBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.procedures.FluidChemicalReactionChamberOnTickUpdateProcedure;
import net.crystalnexus.util.MachineUpgradeHelper;
import net.crystalnexus.jei_recipes.FluidChemicalReactionRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class FluidChemicalReactionChamberGameTests {
    private FluidChemicalReactionChamberGameTests() {}

    @GameTest(template = "zero_point")
    public static void feEfficiencyUpgradeCosts(GameTestHelper helper) {
        helper.assertTrue(MachineUpgradeHelper.energyCost(
                new ItemStack(CrystalnexusModItems.FE_EFFICIENCY_UPGRADE.get()), 4096) == 2731,
            "The FE Efficiency Upgrade must provide 1.5x FE efficiency");
        helper.assertTrue(MachineUpgradeHelper.energyCost(
                new ItemStack(CrystalnexusModItems.CARBON_FE_EFFICIENCY_UPGRADE.get()), 4096) == 2048,
            "The Carbon FE Efficiency Upgrade must provide 2x FE efficiency");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void tanksExposeInputsAndOutputSafely(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.FLUID_CHEMICAL_REACTION_CHAMBER.get());
        FluidChemicalReactionChamberBlockEntity chamber = helper.getBlockEntity(pos);
        IFluidHandler handler = chamber.getFluidHandler();

        helper.assertTrue(handler.fill(new FluidStack(Fluids.WATER, 5000), IFluidHandler.FluidAction.EXECUTE) == 4000,
            "Input tanks must cap each fill at 4000 mB");
        helper.assertTrue(handler.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0,
            "A full fluid must not overflow into the other input tank");
        helper.assertTrue(chamber.getTank(1).isEmpty(),
            "The second input tank must remain available for a different fluid");
        helper.assertTrue(handler.drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
            "External drains must not pull from input tanks");
        chamber.getTank(2).fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(handler.drain(1000, IFluidHandler.FluidAction.EXECUTE).getAmount() == 1000,
            "External drains must pull from the output tank");
        chamber.purge(0);
        helper.assertTrue(chamber.getTank(0).isEmpty(), "Purge must empty the selected tank");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void bucketEmptiesIntoInputTank(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.FLUID_CHEMICAL_REACTION_CHAMBER.get());
        FluidChemicalReactionChamberBlockEntity chamber = helper.getBlockEntity(pos);
        chamber.setItem(0, new ItemStack(CrystalnexusModItems.GASOLINE_BUCKET.get()));

        FluidChemicalReactionChamberOnTickUpdateProcedure.execute(helper.getLevel(), helper.absolutePos(pos));

        helper.assertTrue(chamber.getTank(0).getFluidAmount() == 1000
                && chamber.getTank(0).getFluid().is(CrystalnexusModFluids.GASOLINE.get()),
            "A modded fluid bucket in an input slot must empty into its matching tank");
        helper.assertTrue(chamber.getItem(0).is(Items.BUCKET),
            "An emptied input bucket must remain in the slot");

        chamber.setItem(1, new ItemStack(CrystalnexusModItems.SULFURIC_ACID_BUCKET.get()));
        FluidChemicalReactionChamberOnTickUpdateProcedure.execute(helper.getLevel(), helper.absolutePos(pos));
        helper.assertTrue(chamber.getTank(1).getFluidAmount() == 1000
                && chamber.getTank(1).getFluid().is(CrystalnexusModFluids.SULFURIC_ACID.get()),
            "A sulfuric acid bucket must empty into its matching input tank");
        helper.assertTrue(chamber.getItem(1).is(Items.BUCKET),
            "An emptied sulfuric acid bucket must remain in the slot");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void itemOutputMustFitSharedBucketSlot(GameTestHelper helper) {
        ItemStack output = new ItemStack(Items.IRON_INGOT, 2);
        helper.assertTrue(FluidChemicalReactionChamberOnTickUpdateProcedure.canStackOutput(ItemStack.EMPTY, output),
            "An empty bucket-output slot must accept an item result");
        helper.assertTrue(FluidChemicalReactionChamberOnTickUpdateProcedure.canStackOutput(new ItemStack(Items.IRON_INGOT, 62), output),
            "A matching item result must fill the shared slot to its stack limit");
        helper.assertFalse(FluidChemicalReactionChamberOnTickUpdateProcedure.canStackOutput(new ItemStack(Items.IRON_INGOT, 63), output),
            "An item result must not overflow the shared slot");
        helper.assertFalse(FluidChemicalReactionChamberOnTickUpdateProcedure.canStackOutput(new ItemStack(Items.GOLD_INGOT), output),
            "An item result must not replace a different item in the shared slot");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void reactionCanProduceFluidAndItemTogether(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.FLUID_CHEMICAL_REACTION_CHAMBER.get());
        FluidChemicalReactionChamberBlockEntity chamber = helper.getBlockEntity(pos);
        chamber.getTank(0).fill(new FluidStack(CrystalnexusModFluids.SULFURIC_ACID.get(), 250), IFluidHandler.FluidAction.EXECUTE);
        chamber.setItem(1, new ItemStack(CrystalnexusModItems.TARROCK.get()));
        for (int i = 0; i < 4; i++) chamber.getEnergyStorage().receiveEnergy(1024, false);

        for (int tick = 0; tick < 100; tick++)
            FluidChemicalReactionChamberOnTickUpdateProcedure.execute(helper.getLevel(), helper.absolutePos(pos));

        FluidChemicalReactionChamberBlockEntity current = helper.getBlockEntity(pos);
        helper.assertTrue(current.getTank(0).isEmpty() && current.getItem(1).isEmpty(),
            "A dual-output reaction must consume its fluid and item inputs");
        helper.assertTrue(current.getTank(2).getFluidAmount() == 100
                && current.getTank(2).getFluid().is(CrystalnexusModFluids.CRUDE_OIL.get()),
            "A dual-output reaction must fill the output tank");
        helper.assertTrue(current.getItem(2).is(Items.BLACKSTONE) && current.getItem(2).getCount() == 1,
            "A dual-output reaction must place its item in the shared bucket-output slot");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void waterAndLavaMakeObsidian(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.FLUID_CHEMICAL_REACTION_CHAMBER.get());
        FluidChemicalReactionChamberBlockEntity chamber = helper.getBlockEntity(pos);
        chamber.getTank(0).fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE);
        chamber.getTank(1).fill(new FluidStack(Fluids.LAVA, 100), IFluidHandler.FluidAction.EXECUTE);
        for (int i = 0; i < 4; i++) chamber.getEnergyStorage().receiveEnergy(1024, false);

        for (int tick = 0; tick < 100; tick++)
            FluidChemicalReactionChamberOnTickUpdateProcedure.execute(helper.getLevel(), helper.absolutePos(pos));

        helper.assertTrue(chamber.getTank(0).isEmpty() && chamber.getTank(1).isEmpty(),
            "The obsidian reaction must consume 100 mB each of Water and Lava");
        helper.assertTrue(chamber.getItem(2).is(Items.OBSIDIAN) && chamber.getItem(2).getCount() == 1,
            "The obsidian reaction must produce one Obsidian");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void acidicSlurryRecoversSulfuricAcid(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.FLUID_CHEMICAL_REACTION_CHAMBER.get());
        FluidChemicalReactionChamberBlockEntity chamber = helper.getBlockEntity(pos);
        chamber.getTank(0).fill(new FluidStack(CrystalnexusModFluids.ACIDIC_SLURRY.get(), 100), IFluidHandler.FluidAction.EXECUTE);
        chamber.setItem(0, new ItemStack(Items.CALCITE));
        for (int i = 0; i < 4; i++) chamber.getEnergyStorage().receiveEnergy(1024, false);

        for (int tick = 0; tick < 100; tick++)
            FluidChemicalReactionChamberOnTickUpdateProcedure.execute(helper.getLevel(), helper.absolutePos(pos));

        helper.assertTrue(chamber.getTank(0).isEmpty() && chamber.getItem(0).isEmpty(),
            "The recovery reaction must consume 100 mB Acidic Slurry and one Calcite");
        helper.assertTrue(chamber.getTank(2).getFluidAmount() == 50
                && chamber.getTank(2).getFluid().is(CrystalnexusModFluids.SULFURIC_ACID.get()),
            "The recovery reaction must produce 50 mB Sulfuric Acid");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void explicitTitaniumShortcutKeepsPriority(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.FLUID_CHEMICAL_REACTION_CHAMBER.get());
        FluidChemicalReactionChamberBlockEntity chamber = helper.getBlockEntity(pos);
        chamber.getTank(0).fill(new FluidStack(CrystalnexusModFluids.SULFURIC_ACID.get(), 100), IFluidHandler.FluidAction.EXECUTE);
        chamber.setItem(0, new ItemStack(CrystalnexusModItems.RAW_ILMENITE.get()));
        chamber.setItem(1, new ItemStack(Items.IRON_INGOT));
        for (int i = 0; i < 4; i++) chamber.getEnergyStorage().receiveEnergy(1024, false);

        for (int tick = 0; tick < 100; tick++)
            FluidChemicalReactionChamberOnTickUpdateProcedure.execute(helper.getLevel(), helper.absolutePos(pos));

        helper.assertTrue(chamber.getTank(0).isEmpty() && chamber.getItem(0).isEmpty() && chamber.getItem(1).isEmpty(),
            "The explicit shortcut must consume raw ilmenite, iron, and 100 mB sulfuric acid");
        helper.assertTrue(chamber.getItem(2).is(CrystalnexusModItems.TITANIUM_INGOT.get())
                && chamber.getItem(2).getCount() == 1,
            "The explicit titanium recipe must override generated material processing");
        helper.assertTrue(chamber.getTank(2).getFluidAmount() == 500
                && chamber.getTank(2).getFluid().is(CrystalnexusModFluids.OXYGEN.get()),
            "The titanium reaction must produce 500 mB Oxygen as a byproduct");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void crudeOilMakesResinAndResinMakesCarbonFiber(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.FLUID_CHEMICAL_REACTION_CHAMBER.get());
        FluidChemicalReactionChamberBlockEntity chamber = helper.getBlockEntity(pos);
        chamber.getTank(0).fill(new FluidStack(CrystalnexusModFluids.CRUDE_OIL.get(), 250), IFluidHandler.FluidAction.EXECUTE);
        chamber.setItem(0, new ItemStack(Items.SLIME_BALL));
        for (int i = 0; i < 4; i++) chamber.getEnergyStorage().receiveEnergy(1024, false);

        for (int tick = 0; tick < 100; tick++)
            FluidChemicalReactionChamberOnTickUpdateProcedure.execute(helper.getLevel(), helper.absolutePos(pos));

        helper.assertTrue(chamber.getTank(0).isEmpty() && chamber.getItem(0).isEmpty(),
            "The resin reaction must consume 250 mB crude oil and one slimeball");
        helper.assertTrue(chamber.getTank(2).getFluidAmount() == 250
                && chamber.getTank(2).getFluid().is(CrystalnexusModFluids.RESIN.get()),
            "The resin reaction must produce 250 mB resin");

        chamber.getFluidHandler().drain(250, IFluidHandler.FluidAction.EXECUTE);
        chamber.getFluidHandler().fill(new FluidStack(CrystalnexusModFluids.RESIN.get(), 250), IFluidHandler.FluidAction.EXECUTE);
        chamber.setItem(0, new ItemStack(CrystalnexusModItems.CARBON_COMPOSITE.get()));
        chamber.setItem(1, new ItemStack(CrystalnexusModItems.CERAMIC_PLATE.get()));
        for (int i = 0; i < 4; i++) chamber.getEnergyStorage().receiveEnergy(1024, false);

        for (int tick = 0; tick < 100; tick++)
            FluidChemicalReactionChamberOnTickUpdateProcedure.execute(helper.getLevel(), helper.absolutePos(pos));

        helper.assertTrue(chamber.getTank(0).getFluidAmount() == 200,
            "The carbon fiber reaction must consume 50 mB resin");
        helper.assertTrue(chamber.getItem(0).isEmpty() && chamber.getItem(1).isEmpty()
                && chamber.getItem(2).is(CrystalnexusModItems.CARBON_FIBER.get()),
            "Carbon Composite and a Ceramic Plate must produce one Carbon Fiber");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void ssdEfficiencyReducesEnergyWithoutMultiplyingOutputs(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.FLUID_CHEMICAL_REACTION_CHAMBER.get());
        FluidChemicalReactionChamberBlockEntity chamber = helper.getBlockEntity(pos);
        chamber.getTank(0).fill(new FluidStack(CrystalnexusModFluids.SULFURIC_ACID.get(), 250), IFluidHandler.FluidAction.EXECUTE);
        chamber.setItem(1, new ItemStack(CrystalnexusModItems.TARROCK.get()));

        ItemStack ssd = new ItemStack(CrystalnexusModItems.SSD.get());
        CustomData.update(DataComponents.CUSTOM_DATA, ssd, tag -> {
            tag.putDouble("cook_mult", 0.05);
            tag.putDouble("fe_efficiency", 2.0);
        });
        chamber.setItem(3, ssd);
        for (int i = 0; i < 2; i++) chamber.getEnergyStorage().receiveEnergy(1024, false);

        for (int tick = 0; tick < 5; tick++)
            FluidChemicalReactionChamberOnTickUpdateProcedure.execute(helper.getLevel(), helper.absolutePos(pos));

        FluidChemicalReactionChamberBlockEntity current = helper.getBlockEntity(pos);
        helper.assertTrue(current.getEnergyStorage().getEnergyStored() == 0,
            "2x FE efficiency must reduce a 4096 FE reaction to 2048 FE");
        helper.assertTrue(current.getTank(2).getFluidAmount() == 100,
            "FE efficiency must not multiply the recipe's fluid output");
        helper.assertTrue(current.getItem(2).is(Items.BLACKSTONE) && current.getItem(2).getCount() == 1,
            "FE efficiency must not multiply the recipe's item output");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void matchingRecipeWritesProgress(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.FLUID_CHEMICAL_REACTION_CHAMBER.get());
        FluidChemicalReactionChamberBlockEntity chamber = helper.getBlockEntity(pos);
        chamber.getTank(1).fill(new FluidStack(CrystalnexusModFluids.GASOLINE.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
		chamber.setItem(0, new ItemStack(Items.COAL_BLOCK, 3));
        for (int i = 0; i < 4; i++) chamber.getEnergyStorage().receiveEnergy(1024, false);

        var loaded = helper.getLevel().getRecipeManager().getAllRecipesFor(FluidChemicalReactionRecipe.Type.INSTANCE);
        int recipes = loaded.size();
        helper.assertTrue(recipes > 0, "The fluid chemical reaction recipe must load");
        FluidChemicalReactionRecipe recipe = loaded.stream().map(net.minecraft.world.item.crafting.RecipeHolder::value)
            .filter(candidate -> candidate.fluidOutput().map(FluidChemicalReactionRecipe.FluidAmount::stack)
                .filter(stack -> stack.is(CrystalnexusModFluids.OVERFUEL.get())).isPresent()).findFirst().orElseThrow();
        boolean fluid0 = recipe.fluidInput(0).isPresent()
            && chamber.getTank(1).getFluid().is(recipe.fluidInput(0).get().stack().getFluid())
            && chamber.getTank(1).getFluidAmount() >= recipe.fluidInput(0).get().amount();
        boolean fluid1 = recipe.fluidInput(1).isEmpty() && chamber.getTank(0).isEmpty();
        boolean item0 = recipe.itemInput(0).isEmpty();
        boolean item1 = recipe.itemInput(1).isPresent() && recipe.itemInput(1).get().test(chamber.getItem(0));
        helper.assertTrue(fluid0 && fluid1 && item0 && item1,
            "Recipe inputs must match; fluid0=" + fluid0 + ", fluid1=" + fluid1 + ", item0=" + item0 + ", item1=" + item1);
        FluidChemicalReactionRecipe.FluidAmount fluidOutput = recipe.fluidOutput().orElseThrow();
        FluidStack result = fluidOutput.stack();
        int accepted = chamber.getTank(2).fill(result, IFluidHandler.FluidAction.SIMULATE);
        helper.assertTrue(!result.isEmpty() && accepted == fluidOutput.amount(),
            "Output tank must accept the recipe result; result=" + result + ", accepted=" + accepted);

        FluidChemicalReactionChamberOnTickUpdateProcedure.execute(helper.getLevel(), helper.absolutePos(pos));

        FluidChemicalReactionChamberBlockEntity current = helper.getBlockEntity(pos);
        double progress = current.getPersistentData().getDouble("progress");
        helper.assertTrue(progress == 1,
            "A matching recipe must write one tick of progress to block NBT; got " + progress
                + ", energy=" + current.getEnergyStorage().getEnergyStored()
                + ", fluid=" + current.getTank(1).getFluidAmount()
                + ", item=" + current.getItem(0)
                + ", sameEntity=" + (current == chamber));

        for (int tick = 1; tick < 100; tick++)
            FluidChemicalReactionChamberOnTickUpdateProcedure.execute(helper.getLevel(), helper.absolutePos(pos));
        current = helper.getBlockEntity(pos);
        helper.assertTrue(current.getTank(1).getFluidAmount() == 750 && current.getItem(0).isEmpty()
                && current.getTank(2).getFluidAmount() == 250,
			"The Overfuel recipe must consume three matched coal blocks and produce its output");
        helper.succeed();
    }
}
