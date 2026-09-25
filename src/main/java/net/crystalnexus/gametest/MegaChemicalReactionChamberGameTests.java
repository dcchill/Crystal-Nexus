package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.MachineEnergyInputBlockEntity;
import net.crystalnexus.block.entity.MachineFluidInputBlockEntity;
import net.crystalnexus.block.entity.MegaChemicalReactionChamberBlockEntity;
import net.crystalnexus.block.entity.MultiblockFluidOutputBlockEntity;
import net.crystalnexus.block.entity.MultiblockItemInputBlockEntity;
import net.crystalnexus.block.entity.MultiblockItemOutputBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class MegaChemicalReactionChamberGameTests {
    private MegaChemicalReactionChamberGameTests() {}

    @GameTest(template = "mega_chem_reactor")
    public static void formsAndRoutesThroughEveryRequiredPort(GameTestHelper helper) {
        BlockPos controllerPos = find(helper, CrystalnexusModBlocks.MEGA_CHEMICAL_REACTION_CHAMBER.get()).getFirst();
        List<BlockPos> casing = find(helper, CrystalnexusModBlocks.TEMPERED_AZURINE_CASING.get());
        helper.assertTrue(casing.size() >= 6, "Mega chemical template needs casing positions for its ports");
        MegaChemicalReactionChamberBlockEntity controller = helper.getBlockEntity(controllerPos);
        helper.assertTrue(!controller.validateStructureNow(), "Mega chemical reactor must require all five ports");

        helper.setBlock(casing.get(0), CrystalnexusModBlocks.MULTIBLOCK_ITEM_INPUT.get());
        helper.setBlock(casing.get(1), CrystalnexusModBlocks.MULTIBLOCK_ITEM_OUTPUT.get());
        helper.setBlock(casing.get(2), CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get());
        helper.setBlock(casing.get(3), CrystalnexusModBlocks.MULTIBLOCK_FLUID_OUTPUT.get());
        helper.setBlock(casing.get(4), CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get());
        MultiblockItemInputBlockEntity itemInput = helper.getBlockEntity(casing.get(0));
        MultiblockItemOutputBlockEntity itemOutput = helper.getBlockEntity(casing.get(1));
        MachineFluidInputBlockEntity fluidInput = helper.getBlockEntity(casing.get(2));
        MultiblockFluidOutputBlockEntity fluidOutput = helper.getBlockEntity(casing.get(3));
        MachineEnergyInputBlockEntity energyInput = helper.getBlockEntity(casing.get(4));

        helper.assertTrue(controller.validateStructureNow() && controller.isFormed(),
            "Mega chemical reactor must form with item, fluid, and energy ports");
        BlockPos absoluteController = helper.absolutePos(controllerPos);
        helper.assertTrue(fluidInput.isBoundTo(absoluteController) && fluidOutput.isBoundTo(absoluteController)
                && energyInput.isBoundTo(absoluteController), "Fluid and energy ports must bind to the formed controller");

        itemInput.setItem(0, new ItemStack(Items.IRON_INGOT));
        MegaChemicalReactionChamberBlockEntity.tick(helper.getLevel(), helper.absolutePos(controllerPos), helper.getBlockState(controllerPos), controller);
        helper.assertTrue(controller.getItem(0).is(Items.IRON_INGOT), "Item input port must feed the controller");
        controller.setItem(2, new ItemStack(Items.GOLD_INGOT));
        MegaChemicalReactionChamberBlockEntity.tick(helper.getLevel(), helper.absolutePos(controllerPos), helper.getBlockState(controllerPos), controller);
        helper.assertTrue(itemOutput.getItem(0).is(Items.GOLD_INGOT), "Item output port must drain the controller");

        fluidInput.getFluidInput().fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(controller.getTank(0).getFluidAmount() == 100, "Fluid input port must fill the controller");
        controller.getTank(2).setFluid(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 100));
        helper.assertTrue(fluidOutput.getFluidOutput().drain(100, IFluidHandler.FluidAction.EXECUTE).getAmount() == 100,
            "Fluid output port must drain the controller");
        energyInput.getEnergyStorage().receiveEnergy(1_000, false);
        MachineEnergyInputBlockEntity.tick(helper.getLevel(), helper.absolutePos(casing.get(4)), helper.getBlockState(casing.get(4)), energyInput);
        helper.assertTrue(controller.getEnergyStorage().getEnergyStored() == 1_000,
            "Energy input port must feed the controller");
        helper.succeed();
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
