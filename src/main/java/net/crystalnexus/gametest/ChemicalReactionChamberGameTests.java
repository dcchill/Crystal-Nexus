package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.ChemicalReactionChamberBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.jei_recipes.ChemicalReactionRecipe;
import net.crystalnexus.jei_recipes.FluidChemicalReactionRecipe;
import net.crystalnexus.procedures.ChemicalReactionChamberOnTickUpdateProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Set;
import java.util.stream.Collectors;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class ChemicalReactionChamberGameTests {
    private ChemicalReactionChamberGameTests() {}

    @GameTest(template = "zero_point")
    public static void industrialRecipeExpansionLoads(GameTestHelper helper) {
        Set<ResourceLocation> chemicalIds = helper.getLevel().getRecipeManager()
            .getAllRecipesFor(ChemicalReactionRecipe.Type.INSTANCE).stream()
            .map(holder -> holder.id()).collect(Collectors.toSet());
        Set<ResourceLocation> fluidIds = helper.getLevel().getRecipeManager()
            .getAllRecipesFor(FluidChemicalReactionRecipe.Type.INSTANCE).stream()
            .map(holder -> holder.id()).collect(Collectors.toSet());

        Set<ResourceLocation> expectedChemical = ids(
            "chemical_reaction_gunpowder", "chemical_reaction_flint",
            "chemical_reaction_glow_ink_sac", "chemical_reaction_prismarine_crystals",
            "chemical_reaction_slimeball", "chemical_reaction_calcite");
        Set<ResourceLocation> expectedFluid = ids(
            "fluid_chemical_reaction_mud", "fluid_chemical_reaction_clay",
            "fluid_chemical_reaction_obsidian", "fluid_chemical_reaction_basalt",
            "fluid_chemical_reaction_leather", "fluid_chemical_reaction_sulfuric_acid_recovery");

        helper.assertTrue(chemicalIds.containsAll(expectedChemical),
            "All six new Chemical Reaction Chamber recipes must load");
        helper.assertTrue(fluidIds.containsAll(expectedFluid),
            "All six new Fluid Chemical Reaction Chamber recipes must load");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void carbonSulfurAndFlintMakeGunpowder(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.CHEMICAL_REACTION_CHAMBER.get());
        ChemicalReactionChamberBlockEntity chamber = helper.getBlockEntity(pos);
        chamber.setItem(0, new ItemStack(CrystalnexusModItems.RAW_CARBON.get()));
        chamber.setItem(1, new ItemStack(CrystalnexusModItems.SULFUR_DUST.get()));
        chamber.setItem(2, new ItemStack(Items.FLINT));
        for (int i = 0; i < 4; i++) chamber.getEnergyStorage().receiveEnergy(1024, false);

        // The first invocation switches the chamber from its placed blockstate to its idle state.
        for (int tick = 0; tick < 101; tick++)
            ChemicalReactionChamberOnTickUpdateProcedure.execute(
                helper.getLevel(), helper.absolutePos(pos).getX(),
                helper.absolutePos(pos).getY(), helper.absolutePos(pos).getZ());

        ChemicalReactionChamberBlockEntity current = helper.getBlockEntity(pos);
        helper.assertTrue(current.getItem(0).isEmpty() && current.getItem(1).isEmpty()
                && current.getItem(2).isEmpty(),
            "The gunpowder reaction must consume all three ingredients; inputs="
                + current.getItem(0) + ", " + current.getItem(1) + ", " + current.getItem(2)
                + ", output=" + current.getItem(3)
                + ", progress=" + current.getPersistentData().getDouble("progress")
                + ", energy=" + current.getEnergyStorage().getEnergyStored());
        helper.assertTrue(current.getItem(3).is(Items.GUNPOWDER) && current.getItem(3).getCount() == 4,
            "The gunpowder reaction must produce four Gunpowder");
        helper.succeed();
    }

    private static Set<ResourceLocation> ids(String... paths) {
        return java.util.Arrays.stream(paths)
            .map(path -> ResourceLocation.fromNamespaceAndPath("crystalnexus", path))
            .collect(Collectors.toSet());
    }
}
