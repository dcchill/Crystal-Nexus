package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.DustSeparatorBlockEntity;
import net.crystalnexus.block.entity.FluidChemicalReactionChamberBlockEntity;
import net.crystalnexus.block.entity.RefineryBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.processing.MaterialProcessingCatalog;
import net.crystalnexus.procedures.DustSeparatorOnTickUpdateProcedure;
import net.crystalnexus.procedures.FluidChemicalReactionChamberOnTickUpdateProcedure;
import net.crystalnexus.procedures.RefineryOnTickUpdateProcedure;
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
        var advanced = catalog.materials().values().stream()
            .filter(material -> !material.profile().reagentTag()
                && material.profile().reagent().toString().equals("crystalnexus:sulfuric_acid"))
            .filter(material -> !material.nugget("crystalnexus", 1).isEmpty())
            .findFirst().orElseThrow();
        ItemStack source = BuiltInRegistries.ITEM.getTag(advanced.raw()).stream().flatMap(set -> set.stream()).findFirst()
            .or(() -> BuiltInRegistries.ITEM.getTag(advanced.ores()).stream().flatMap(set -> set.stream()).findFirst())
            .map(holder -> new ItemStack(holder.value())).orElseThrow();
        BlockPos chamberPos = new BlockPos(1, 1, 1);
        helper.setBlock(chamberPos, CrystalnexusModBlocks.FLUID_CHEMICAL_REACTION_CHAMBER.get());
        FluidChemicalReactionChamberBlockEntity chamber = helper.getBlockEntity(chamberPos);
        chamber.setItem(0, source);
        chamber.getTank(0).fill(new FluidStack(net.crystalnexus.init.CrystalnexusModFluids.SULFURIC_ACID.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        for (int i = 0; i < 4; i++) chamber.getEnergyStorage().receiveEnergy(1024, false);
        for (int tick = 0; tick < 100; tick++)
            FluidChemicalReactionChamberOnTickUpdateProcedure.execute(helper.getLevel(), helper.absolutePos(chamberPos));
        FluidStack slurry = chamber.getTank(2).getFluid();
        helper.assertTrue(chamber.getItem(0).isEmpty() && slurry.getAmount() == 1000
                && MaterialProcessingCatalog.slurryMaterial(slurry).filter(advanced.id()::equals).isPresent(),
            "Raw ore and sulfuric acid must produce identified mineral slurry");

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

        BlockPos separatorPos = new BlockPos(5, 1, 1);
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
