package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.DustSeparatorBlockEntity;
import net.crystalnexus.block.entity.FluidChemicalReactionChamberBlockEntity;
import net.crystalnexus.block.entity.RefineryBlockEntity;
import net.crystalnexus.block.entity.CrystalCrusherBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.processing.MaterialProcessingCatalog;
import net.crystalnexus.procedures.DustSeparatorOnTickUpdateProcedure;
import net.crystalnexus.procedures.FluidChemicalReactionChamberOnTickUpdateProcedure;
import net.crystalnexus.procedures.RefineryOnTickUpdateProcedure;
import net.crystalnexus.procedures.CrystalCrusherOnTickUpdateProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class MaterialProcessingGameTests {
    private MaterialProcessingGameTests() {}

    @GameTest(template = "zero_point")
    public static void defaultAndAdvancedYields(GameTestHelper helper) {
        var catalog = MaterialProcessingCatalog.get(helper.getLevel());
        var advanced = java.util.Optional.ofNullable(catalog.materials().get("chlorophyte")).orElseThrow();
        ItemStack source = BuiltInRegistries.ITEM.getTag(advanced.raw()).stream().flatMap(set -> set.stream()).findFirst()
            .or(() -> BuiltInRegistries.ITEM.getTag(advanced.ores()).stream().flatMap(set -> set.stream()).findFirst())
            .map(holder -> new ItemStack(holder.value())).orElseThrow();
        BlockPos crusherPos = new BlockPos(1, 1, 1);
        helper.setBlock(crusherPos, CrystalnexusModBlocks.CRYSTAL_CRUSHER.get());
        CrystalCrusherBlockEntity crusher = helper.getBlockEntity(crusherPos);
        crusher.setItem(0, source);
        for (int i = 0; i < 4; i++) crusher.getEnergyStorage().receiveEnergy(1024, false);
        BlockPos absoluteCrusher = helper.absolutePos(crusherPos);
        for (int tick = 0; tick < 101; tick++)
            CrystalCrusherOnTickUpdateProcedure.execute(helper.getLevel(), absoluteCrusher.getX(),
                absoluteCrusher.getY(), absoluteCrusher.getZ());
        helper.assertTrue(crusher.getItem(1).is(CrystalnexusModItems.CHLOROPHYTE_DUST.get())
                && crusher.getItem(1).getCount() == 2,
            "Crystal-tier crushing must begin Chlorophyte wet processing");

        BlockPos chamberPos = new BlockPos(2, 1, 1);
        helper.setBlock(chamberPos, CrystalnexusModBlocks.FLUID_CHEMICAL_REACTION_CHAMBER.get());
        FluidChemicalReactionChamberBlockEntity chamber = helper.getBlockEntity(chamberPos);
        chamber.setItem(0, crusher.getItem(1).copy());
        chamber.getTank(0).fill(new FluidStack(net.crystalnexus.init.CrystalnexusModFluids.SULFURIC_ACID.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        for (int i = 0; i < 4; i++) chamber.getEnergyStorage().receiveEnergy(1024, false);
        for (int tick = 0; tick < 100; tick++)
            FluidChemicalReactionChamberOnTickUpdateProcedure.execute(helper.getLevel(), helper.absolutePos(chamberPos));
        FluidStack slurry = chamber.getTank(2).getFluid();
        helper.assertTrue(chamber.getItem(0).isEmpty() && slurry.getAmount() == 1000
                && MaterialProcessingCatalog.slurryMaterial(slurry).filter(advanced.id()::equals).isPresent(),
            "Chlorophyte Dust and sulfuric acid must produce identified mineral slurry");

        BlockPos refineryPos = new BlockPos(3, 1, 1);
        helper.setBlock(refineryPos, CrystalnexusModBlocks.REFINERY.get());
        RefineryBlockEntity refinery = helper.getBlockEntity(refineryPos);
        refinery.getTank(0).fill(slurry.copy(), IFluidHandler.FluidAction.EXECUTE);
        for (int i = 0; i < 4; i++) refinery.getEnergyStorage().receiveEnergy(1024, false);
        for (int tick = 0; tick < 100; tick++)
            RefineryOnTickUpdateProcedure.execute(helper.getLevel(), helper.absolutePos(refineryPos));
        helper.assertTrue(refinery.getTank(0).isEmpty() && refinery.getItem(1).is(advanced.dust())
                && refinery.getItem(1).getCount() == 3,
            "One bucket of identified slurry must refine to three dust");

        BlockPos separatorPos = new BlockPos(4, 1, 1);
        helper.setBlock(separatorPos, CrystalnexusModBlocks.DUST_SEPARATOR.get());
        DustSeparatorBlockEntity separator = helper.getBlockEntity(separatorPos);
        separator.setItem(0, refinery.getItem(1).copy());
        BlockPos absoluteSeparator = helper.absolutePos(separatorPos);
        for (int operation = 0; operation < 3; operation++) {
            for (int i = 0; i < 4; i++) separator.getEnergyStorage().receiveEnergy(1024, false);
            for (int tick = 0; tick < 100; tick++)
                DustSeparatorOnTickUpdateProcedure.execute(helper.getLevel(), absoluteSeparator.getX(),
                    absoluteSeparator.getY(), absoluteSeparator.getZ());
        }
        helper.assertTrue(separator.getItem(0).isEmpty() && separator.getItem(1).is(advanced.nugget())
                && separator.getItem(1).getCount() == 33,
            "Each of the three dust must separate into eleven nuggets");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void refineryRequiresFullSlurryAmount(GameTestHelper helper) {
        var material = MaterialProcessingCatalog.get(helper.getLevel()).materials().values().stream()
            .findFirst().orElseThrow();
        BlockPos refineryPos = new BlockPos(1, 1, 1);
        helper.setBlock(refineryPos, CrystalnexusModBlocks.REFINERY.get());
        RefineryBlockEntity refinery = helper.getBlockEntity(refineryPos);
        refinery.getTank(0).fill(MaterialProcessingCatalog.slurry(material.id(), 1),
            IFluidHandler.FluidAction.EXECUTE);
        for (int i = 0; i < 4; i++) refinery.getEnergyStorage().receiveEnergy(1024, false);

        BlockPos absoluteRefinery = helper.absolutePos(refineryPos);
        for (int tick = 0; tick < 100; tick++)
            RefineryOnTickUpdateProcedure.execute(helper.getLevel(), absoluteRefinery);

        helper.assertTrue(refinery.getTank(0).getFluidAmount() == 1,
            "An incomplete slurry batch must not be consumed");
        helper.assertTrue(refinery.getItem(1).isEmpty(),
            "One millibucket of slurry must not produce dust");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void invertiumRequiresChlorophyteCrusher(GameTestHelper helper) {
        BlockPos crystalPos = new BlockPos(1, 1, 1);
        BlockPos chlorophytePos = new BlockPos(3, 1, 1);
        helper.setBlock(crystalPos, CrystalnexusModBlocks.CRYSTAL_CRUSHER.get());
        helper.setBlock(chlorophytePos, CrystalnexusModBlocks.CHLOROPHYTE_CRUSHER.get());
        CrystalCrusherBlockEntity crystal = helper.getBlockEntity(crystalPos);
        CrystalCrusherBlockEntity chlorophyte = helper.getBlockEntity(chlorophytePos);
        crystal.setItem(0, new ItemStack(CrystalnexusModItems.RAW_INVERTIUM.get()));
        chlorophyte.setItem(0, new ItemStack(CrystalnexusModItems.RAW_INVERTIUM.get()));
        for (int i = 0; i < 4; i++) {
            crystal.getEnergyStorage().receiveEnergy(1024, false);
            chlorophyte.getEnergyStorage().receiveEnergy(1024, false);
        }

        BlockPos absoluteCrystal = helper.absolutePos(crystalPos);
        BlockPos absoluteChlorophyte = helper.absolutePos(chlorophytePos);
        for (int tick = 0; tick < 100; tick++) {
            CrystalCrusherOnTickUpdateProcedure.execute(helper.getLevel(), absoluteCrystal.getX(),
                absoluteCrystal.getY(), absoluteCrystal.getZ());
            CrystalCrusherOnTickUpdateProcedure.execute(helper.getLevel(), absoluteChlorophyte.getX(),
                absoluteChlorophyte.getY(), absoluteChlorophyte.getZ());
        }

        helper.assertTrue(crystal.getItem(1).isEmpty() && crystal.getItem(0).getCount() == 1,
            "Crystal-tier crushers must reject raw Invertium");
        helper.assertTrue(chlorophyte.getItem(1).is(CrystalnexusModItems.INVERTIUM_DUST.get())
                && chlorophyte.getItem(1).getCount() == 2 && chlorophyte.getItem(0).isEmpty(),
            "Chlorophyte-tier crushers must unlock Invertium processing");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void slurryComponentsSurviveStorageAndPreventMerging(GameTestHelper helper) {
        FluidStack copper = MaterialProcessingCatalog.slurry(net.minecraft.resources.ResourceLocation.parse("c:copper"), 1000);
        FluidStack tin = MaterialProcessingCatalog.slurry(net.minecraft.resources.ResourceLocation.parse("c:tin"), 1000);
        FluidTank tank = new FluidTank(4000);
        tank.fill(copper, IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(tank.fill(tin, IFluidHandler.FluidAction.SIMULATE) == 0,
            "Copper and tin slurry must not merge");
        var saved = tank.writeToNBT(helper.getLevel().registryAccess(), new net.minecraft.nbt.CompoundTag());
        FluidTank restored = new FluidTank(4000);
        restored.readFromNBT(helper.getLevel().registryAccess(), saved);
        helper.assertTrue(MaterialProcessingCatalog.slurryMaterial(restored.getFluid())
                .filter(net.minecraft.resources.ResourceLocation.parse("c:copper")::equals).isPresent(),
            "Slurry material identity must survive tank NBT persistence");
        helper.succeed();
    }
}
