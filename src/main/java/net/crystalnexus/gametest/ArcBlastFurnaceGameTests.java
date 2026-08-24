package net.crystalnexus.gametest;

import net.crystalnexus.block.HeatingCoreBlock;
import net.crystalnexus.block.entity.ArcFurnaceBlockEntity;
import net.crystalnexus.block.entity.MachineEnergyInputBlockEntity;
import net.crystalnexus.block.entity.MultiblockItemInputBlockEntity;
import net.crystalnexus.block.entity.MultiblockItemOutputBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class ArcBlastFurnaceGameTests {
    private ArcBlastFurnaceGameTests() {}

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

    private static List<BlockPos> find(GameTestHelper helper, Block block) {
        List<BlockPos> found = new ArrayList<>();
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (helper.getBlockState(pos).is(block)) found.add(pos);
        }
        return found;
    }
}
