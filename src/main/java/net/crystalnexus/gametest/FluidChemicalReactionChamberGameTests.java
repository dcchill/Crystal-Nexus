package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.FluidChemicalReactionChamberBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.procedures.FluidChemicalReactionChamberOnTickUpdateProcedure;
import net.crystalnexus.jei_recipes.FluidChemicalReactionRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    public static void matchingRecipeWritesProgress(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.FLUID_CHEMICAL_REACTION_CHAMBER.get());
        FluidChemicalReactionChamberBlockEntity chamber = helper.getBlockEntity(pos);
        chamber.getTank(1).fill(new FluidStack(CrystalnexusModFluids.GASOLINE.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        chamber.setItem(0, new ItemStack(CrystalnexusModItems.CONDUCTIVE_ALLOY.get()));
        for (int i = 0; i < 4; i++) chamber.getEnergyStorage().receiveEnergy(1024, false);

        var loaded = helper.getLevel().getRecipeManager().getAllRecipesFor(FluidChemicalReactionRecipe.Type.INSTANCE);
        int recipes = loaded.size();
        helper.assertTrue(recipes > 0, "The fluid chemical reaction recipe must load");
        FluidChemicalReactionRecipe recipe = loaded.stream().map(net.minecraft.world.item.crafting.RecipeHolder::value)
            .filter(candidate -> candidate.output().stack().is(CrystalnexusModFluids.OVERFUEL.get())).findFirst().orElseThrow();
        boolean fluid0 = recipe.fluidInput(0).isPresent()
            && chamber.getTank(1).getFluid().is(recipe.fluidInput(0).get().stack().getFluid())
            && chamber.getTank(1).getFluidAmount() >= recipe.fluidInput(0).get().amount();
        boolean fluid1 = recipe.fluidInput(1).isEmpty() && chamber.getTank(0).isEmpty();
        boolean item0 = recipe.itemInput(0).isEmpty();
        boolean item1 = recipe.itemInput(1).isPresent() && recipe.itemInput(1).get().test(chamber.getItem(0));
        helper.assertTrue(fluid0 && fluid1 && item0 && item1,
            "Recipe inputs must match; fluid0=" + fluid0 + ", fluid1=" + fluid1 + ", item0=" + item0 + ", item1=" + item1);
        FluidStack result = recipe.output().stack();
        int accepted = chamber.getTank(2).fill(result, IFluidHandler.FluidAction.SIMULATE);
        helper.assertTrue(!result.isEmpty() && accepted == recipe.output().amount(),
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
        helper.assertTrue(current.getTank(1).getFluidAmount() == 900 && current.getItem(0).isEmpty()
                && current.getTank(2).getFluidAmount() == 100,
            "A shapeless recipe must consume the matched swapped inputs and produce its output");
        helper.succeed();
    }
}
