package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.*;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.procedures.*;
import net.crystalnexus.util.EeMatterEconomy;
import net.crystalnexus.util.MachineUpgradeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class MachineProcessingGameTests {
    @GameTest(template = "zero_point")
    public static void smeltersKeepCycleLengthsCostsAndUpgradeEffects(GameTestHelper helper) {
        Block[] blocks = { CrystalnexusModBlocks.IRON_SMELTER.get(), CrystalnexusModBlocks.CRYSTAL_SMELTER.get(),
            CrystalnexusModBlocks.CHLOROPHYTE_SMELTER.get(), CrystalnexusModBlocks.INVERTIUM_SMELTER.get() };
        int[] times = {100, 75, 75, 60};
        int[] accelerated = {75, 50, 50, 40};
        for (int variant = 0; variant < blocks.length; variant++) {
            BlockPos pos = new BlockPos(1 + variant * 2, 1, 1);
            helper.setBlock(pos, blocks[variant]);
            BlockEntity machine = helper.getBlockEntity(pos);
            Container inventory = (Container) machine;
            var energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, machine.getBlockPos(), null);
            for (int mode = 0; mode < 3; mode++) {
                ItemStack upgrade = mode == 1 ? new ItemStack(CrystalnexusModItems.ACCELERATION_UPGRADE.get())
                    : mode == 2 ? new ItemStack(CrystalnexusModItems.SSD.get()) : ItemStack.EMPTY;
                if (mode == 2) CustomData.update(DataComponents.CUSTOM_DATA, upgrade, tag -> tag.putDouble("cook_mult", 0.5));
                inventory.setItem(2, upgrade);
                inventory.setItem(0, new ItemStack(Items.RAW_IRON, 2));
                inventory.setItem(1, ItemStack.EMPTY);
                machine.getPersistentData().putDouble("progress", 0);
                fill(energy);
                int before = energy.getEnergyStored();
                int cost = energy.extractEnergy(MachineUpgradeHelper.energyCost(machine.getBlockState(), variant == 0 ? ItemStack.EMPTY : upgrade, 2048), true);
                int ticks = variant == 0 ? times[variant] : mode == 1 ? accelerated[variant] : mode == 2 ? (int) Math.ceil(times[variant] * 0.5) : times[variant];
                // The first transition to the working animation invokes the existing onPlace reset.
                if (mode == 0) ticks++;
                for (int i = 1; i < ticks; i++) tick(machine);
                helper.assertTrue(inventory.getItem(1).isEmpty(), "Smelter must not finish early");
                tick(machine);
                helper.assertTrue(inventory.getItem(1).is(Items.IRON_INGOT) && inventory.getItem(1).getCount() == 1
                        && inventory.getItem(0).getCount() == 1 && energy.getEnergyStored() == before - cost,
                    "Smelter variant=" + variant + " mode=" + mode + " output=" + inventory.getItem(1) + " input=" + inventory.getItem(0) + " progress=" + machine.getPersistentData().getDouble("progress") + " energy=" + energy.getEnergyStored() + " expected=" + (before - cost));
            }
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void parallelizationChipsBatchSmelterCrafts(GameTestHelper helper) {
        Block[] blocks = { CrystalnexusModBlocks.IRON_SMELTER.get(), CrystalnexusModBlocks.CRYSTAL_SMELTER.get(),
            CrystalnexusModBlocks.CHLOROPHYTE_SMELTER.get(), CrystalnexusModBlocks.INVERTIUM_SMELTER.get() };
        int[] times = {100, 75, 75, 60};
        for (int variant = 1; variant < blocks.length; variant++) {
            BlockPos pos = new BlockPos(1 + variant * 2, 1, 1);
            helper.setBlock(pos, blocks[variant]);
            BlockEntity machine = helper.getBlockEntity(pos);
            Container inventory = (Container) machine;
            IEnergyStorage energy = machine instanceof IronSmelterBlockEntity smelter ? smelter.getEnergyStorage()
                : machine instanceof CrystalSmelterBlockEntity smelter ? smelter.getEnergyStorage()
                : machine instanceof ChlorophyteSmelterBlockEntity smelter ? smelter.getEnergyStorage()
                : ((InvertiumSmelterBlockEntity) machine).getEnergyStorage();
            ItemStack chips = new ItemStack(CrystalnexusModItems.PARALLELIZATION_CHIP.get(), 2);
            inventory.setItem(0, new ItemStack(Items.RAW_IRON, 4));
            inventory.setItem(1, ItemStack.EMPTY);
            inventory.setItem(2, chips);
            machine.getPersistentData().putDouble("progress", 0);
            fill(energy);
            int before = energy.getEnergyStored();
            int maxExtract = switch (variant) {
                case 0 -> net.crystalnexus.config.CrystalnexusConfig.MACHINES.IRON_SMELTER.maxExtract();
                case 1 -> net.crystalnexus.config.CrystalnexusConfig.MACHINES.CRYSTAL_SMELTER.maxExtract();
                case 2 -> net.crystalnexus.config.CrystalnexusConfig.MACHINES.CHLOROPHYTE_SMELTER.maxExtract();
                default -> net.crystalnexus.config.CrystalnexusConfig.MACHINES.INVERTIUM_SMELTER.maxExtract();
            };
            int energyPerCraft = Math.min(MachineUpgradeHelper.energyCost(machine.getBlockState(), chips, 2048), maxExtract);

            for (int tick = 0; tick < times[variant] + 1; tick++) tick(machine);

            helper.assertTrue(inventory.getItem(1).is(Items.IRON_INGOT) && inventory.getItem(1).getCount() == 4
                    && inventory.getItem(0).isEmpty() && energy.getEnergyStored() == before - energyPerCraft * 4,
                "Parallel smelter variant=" + variant + " output=" + inventory.getItem(1)
                    + " input=" + inventory.getItem(0) + " energy=" + energy.getEnergyStored());
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void processorsPauseAndResumeWithBlockedOutputs(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.INVERTIUM_SMELTER.get());
        InvertiumSmelterBlockEntity smelter = helper.getBlockEntity(pos);
        fill(smelter.getEnergyStorage());
        smelter.setItem(0, new ItemStack(Items.RAW_IRON, 2));
        for (int i = 0; i < 31; i++) tick(smelter);
        smelter.setItem(1, new ItemStack(Items.IRON_INGOT, 64));
        int energy = smelter.getEnergyStorage().getEnergyStored();
        for (int i = 0; i < 100; i++) tick(smelter);
        helper.assertTrue(smelter.getPersistentData().getDouble("progress") == 30
                && smelter.getEnergyStorage().getEnergyStored() == energy && smelter.getItem(0).getCount() == 2,
            "Blocked output: progress=" + smelter.getPersistentData().getDouble("progress") + " energy=" + smelter.getEnergyStorage().getEnergyStored() + " input=" + smelter.getItem(0));
        smelter.setItem(1, ItemStack.EMPTY);
        for (int i = 0; i < 30; i++) tick(smelter);
        helper.assertTrue(smelter.getItem(1).getCount() == 1 && smelter.getItem(0).getCount() == 1,
            "Unblocked machine must complete its remaining ticks");
        smelter.setItem(0, new ItemStack(Items.STICK));
        for (int i = 0; i < 60; i++) tick(smelter);
        helper.assertTrue(smelter.getItem(1).getCount() == 1, "Changed input must be matched again");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void advancedPressKeepsFluidAndBatchAccounting(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.TITANIUM_CARBIDE_CIRCUIT_PRESS.get());
        CircuitPressBlockEntity press = helper.getBlockEntity(pos);
        var recipe = helper.getLevel().getRecipeManager().getAllRecipesFor(
            net.crystalnexus.jei_recipes.TitaniumCarbideCircuitPressRecipe.Type.INSTANCE).stream()
            .map(holder -> holder.value()).filter(r -> r.itemInput(1).isEmpty()).findFirst().orElseThrow();
        ItemStack input = recipe.itemInput(0).orElseThrow().getItems()[0].copy();
        input.setCount(recipe.itemInputCount(0));
        press.setItem(0, input);
        fill(press.getEnergyStorage());
        for (int i = 0; i < 30; i++) tick(press);
        helper.assertTrue(press.getItem(1).isEmpty() && press.getItem(0).getCount() == input.getCount(),
            "Advanced recipe must wait for nitrogen");
        press.getNitrogenTank().fill(new FluidStack(CrystalnexusModFluids.NITROGEN.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        int before = press.getEnergyStorage().getEnergyStored();
        int cost = press.getEnergyStorage().extractEnergy(MachineUpgradeHelper.energyCost(press.getBlockState(), ItemStack.EMPTY, 2048), true);
        for (int i = 0; i < 29; i++) tick(press);
        helper.assertTrue(press.getItem(1).isEmpty(), "Ferrosteel press must not finish before tick 30");
        tick(press);
        helper.assertTrue(ItemStack.matches(press.getItem(1), recipe.getResultItem(null)) && press.getItem(0).isEmpty()
                && press.getNitrogenTank().getFluidAmount() == 1000 - recipe.fluidInput(0).orElseThrow().amount()
                && press.getEnergyStorage().getEnergyStored() == before - cost,
            "Advanced output, fuel and FE accounting must stay unchanged");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void extractorKeepsGenerationAndFullBufferBehavior(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.ENERGY_EXTRACTOR.get());
        EnergyExtractorBlockEntity extractor = helper.getBlockEntity(pos);
        extractor.setItem(0, new ItemStack(CrystalnexusModItems.EE_MATTER.get(), 2));
        extractor.getEnergyStorage().generateEnergy(Integer.MAX_VALUE, false);
        for (int i = 0; i < 200; i++) tick(extractor);
        helper.assertTrue(extractor.getItem(0).getCount() == 2 && extractor.getItem(2).isEmpty(),
            "Full generator buffer must not consume recipe input");
        while (extractor.getEnergyStorage().extractEnergy(Integer.MAX_VALUE, false) > 0) {}
        // Starting from idle includes the existing animation transition/reset tick.
        for (int i = 0; i < 200; i++) tick(extractor);
        helper.assertTrue(extractor.getItem(2).isEmpty(), "Extraction must take 200 processing ticks after the startup transition");
        tick(extractor);
        helper.assertTrue(extractor.getItem(0).getCount() == 1 && extractor.getItem(2).getCount() == 1
                && extractor.getEnergyStorage().getEnergyStored() == EeMatterEconomy.EXTRACTION_FE_PER_ITEM,
            "Extraction: input=" + extractor.getItem(0) + " output=" + extractor.getItem(2) + " progress=" + extractor.getPersistentData().getDouble("progress") + " energy=" + extractor.getEnergyStorage().getEnergyStored());
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void pistonKeepsFuelAndGenerationPerTick(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.PISTON_GENERATOR.get());
        PistonGeneratorBlockEntity generator = helper.getBlockEntity(pos);
        generator.getFluidTank().fill(new FluidStack(CrystalnexusModFluids.GASOLINE.get(), 250), IFluidHandler.FluidAction.EXECUTE);
        var energy = generator.getEnergyStorage();
        energy.generateEnergy(Integer.MAX_VALUE, false);
        for (int i = 0; i < 20; i++) tick(generator);
        helper.assertTrue(generator.getPersistentData().getDouble("progress") == 0
                && generator.getFluidTank().getFluidAmount() == 250, "Full generator must preserve fuel and progress");
        while (energy.extractEnergy(Integer.MAX_VALUE, false) > 0) {}
        int generated = 0;
        for (int i = 0; i < 350; i++) {
            tick(generator);
            generated += energy.extractEnergy(Integer.MAX_VALUE, false);
        }
        helper.assertTrue(generated == 350 * 256 && generator.getFluidTank().isEmpty()
                && generator.getPersistentData().getDouble("progress") == 0,
            "Piston generator must retain 256 FE/t and consume 250 mB per 350 ticks");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void recipeReplacementIsVisibleWithoutInputChange(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.INVERTIUM_SMELTER.get());
        InvertiumSmelterBlockEntity smelter = helper.getBlockEntity(pos);
        smelter.setItem(0, new ItemStack(Items.DIAMOND));
        fill(smelter.getEnergyStorage());
        tick(smelter);
        helper.assertTrue(smelter.getPersistentData().getDouble("progress") == 0, "Unknown input must stay idle");
        var manager = helper.getLevel().getRecipeManager();
        var original = java.util.List.copyOf(manager.getRecipes());
        try {
            var recipes = new java.util.ArrayList<net.minecraft.world.item.crafting.RecipeHolder<?>>(original);
            var replacement = new net.minecraft.world.item.crafting.RecipeHolder<>(
                net.minecraft.resources.ResourceLocation.parse("crystalnexus:test_smelter_reload"),
                new net.minecraft.world.item.crafting.SmeltingRecipe("", net.minecraft.world.item.crafting.CookingBookCategory.MISC,
                    net.minecraft.world.item.crafting.Ingredient.of(Items.DIAMOND), new ItemStack(Items.EMERALD), 0, 200));
            recipes.set(0, replacement);
            manager.replaceRecipes(recipes);
            for (int i = 0; i < 61; i++) tick(smelter);
            helper.assertTrue(smelter.getItem(1).is(Items.EMERALD) && smelter.getItem(0).isEmpty(),
                "Same-count recipe replacement must invalidate a previous no-match");

            manager.replaceRecipes(original);
            smelter.setItem(0, new ItemStack(Items.DIAMOND));
            smelter.setItem(1, ItemStack.EMPTY);
            tick(smelter);
            helper.assertTrue(smelter.getPersistentData().getDouble("progress") == 0,
                "Removing a recipe must invalidate a previous match");

            recipes = new java.util.ArrayList<>(original);
            recipes.add(replacement);
            manager.replaceRecipes(recipes);
            for (int i = 0; i < 61; i++) tick(smelter);
            helper.assertTrue(smelter.getItem(1).is(Items.EMERALD) && smelter.getItem(0).isEmpty(),
                "Recipe addition must be visible without changing the input");
        } finally {
            manager.replaceRecipes(original);
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void cachedSmeltingKeepsConsecutiveCycleTiming(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.INVERTIUM_SMELTER.get());
        InvertiumSmelterBlockEntity smelter = helper.getBlockEntity(pos);
        smelter.setItem(0, new ItemStack(Items.RAW_IRON, 3));
        fill(smelter.getEnergyStorage());
        for (int i = 0; i < 61; i++) tick(smelter);
        helper.assertTrue(smelter.getItem(1).getCount() == 1 && smelter.getItem(0).getCount() == 2,
            "First cycle must finish on its original tick");
        int afterFirst = smelter.getEnergyStorage().getEnergyStored();
        int cost = smelter.getEnergyStorage().extractEnergy(
            MachineUpgradeHelper.energyCost(smelter.getBlockState(), ItemStack.EMPTY, 2048), true);
        for (int i = 0; i < 59; i++) tick(smelter);
        helper.assertTrue(smelter.getItem(1).getCount() == 1 && smelter.getEnergyStorage().getEnergyStored() == afterFirst,
            "Next recipe must not complete early");
        tick(smelter);
        helper.assertTrue(smelter.getItem(1).getCount() == 2 && smelter.getItem(0).getCount() == 1
                && smelter.getEnergyStorage().getEnergyStored() == afterFirst - cost,
            "Consecutive recipe: output=" + smelter.getItem(1).getCount()
                + " input=" + smelter.getItem(0).getCount()
                + " progress=" + smelter.getPersistentData().getDouble("progress")
                + " energy=" + smelter.getEnergyStorage().getEnergyStored()
                + " expectedEnergy=" + (afterFirst - cost));
        helper.succeed();
    }

    private static void fill(IEnergyStorage energy) {
        while (energy.receiveEnergy(Integer.MAX_VALUE, false) > 0) {}
    }

    private static void tick(BlockEntity machine) {
        var level = machine.getLevel();
        var pos = machine.getBlockPos();
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        if (machine instanceof IronSmelterBlockEntity) IronSmelterOnTickUpdateProcedure.execute(level, x, y, z);
        else if (machine instanceof CrystalSmelterBlockEntity) CrystalSmelterOnTickUpdateProcedure.execute(level, x, y, z);
        else if (machine instanceof ChlorophyteSmelterBlockEntity) ChlorophyteSmelterOnTickUpdateProcedure.execute(level, x, y, z);
        else if (machine instanceof InvertiumSmelterBlockEntity) InvertiumSmelterOnTickUpdateProcedure.execute(level, x, y, z);
        else if (machine instanceof CircuitPressBlockEntity) CircuitPressOnTickUpdateProcedure.execute(level, x, y, z);
        else if (machine instanceof EnergyExtractorBlockEntity) EnergyExtractorOnTickUpdateProcedure.execute(level, x, y, z);
        else if (machine instanceof PistonGeneratorBlockEntity) PistonGeneratorOnTickUpdateProcedure.execute(level, x, y, z);
        else throw new IllegalArgumentException("Unsupported test machine");
    }
}
