package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.MasticatorBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.crystalnexus.item.PrisonCubeItem;
import net.crystalnexus.processing.GeneSplicingDefaults;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class GeneSplicerGameTests {
    @GameTest(template = "zero_point")
    public static void producesEggAndDropWithoutConsumingBlockedInputs(GameTestHelper helper) {
        var level = helper.getLevel();
        GeneSplicingDefaults.sync(new OnDatapackSyncEvent(level.getServer().getPlayerList(), null));
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.MASTICATOR.get());
        MasticatorBlockEntity machine = helper.getBlockEntity(pos);
        for (String mob : new String[]{"zombie", "enderman"}) {
            helper.assertTrue(machine.getBloodTank().fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 500),
                    IFluidHandler.FluidAction.EXECUTE) == 0, "The gene splicer must reject fluids other than blood");
            ItemStack cube = new ItemStack(CrystalnexusModItems.PRISON_CUBE.get());
            PrisonCubeItem.setStoredEntityType(cube, ResourceLocation.withDefaultNamespace(mob));
            machine.setItem(1, cube);
            machine.getBloodTank().fill(new FluidStack(CrystalnexusModFluids.BLOOD.get(), 499), IFluidHandler.FluidAction.EXECUTE);
            tick(helper, pos, machine);
            helper.assertTrue(machine.getItem(2).isEmpty() && PrisonCubeItem.hasStoredEntity(cube)
                    && machine.getBloodTank().getFluidAmount() == 499, "Insufficient blood must preserve the captured mob");
            machine.getBloodTank().fill(new FluidStack(CrystalnexusModFluids.BLOOD.get(), 501), IFluidHandler.FluidAction.EXECUTE);
            for (int blockedSlot : new int[]{2, 3}) {
                machine.setItem(blockedSlot, new ItemStack(Items.COBBLESTONE, 64));
                tick(helper, pos, machine);
                helper.assertTrue(machine.getBloodTank().getFluidAmount() == 1000 && PrisonCubeItem.hasStoredEntity(cube),
                    "A blocked output must preserve blood and the captured mob");
                machine.setItem(blockedSlot, ItemStack.EMPTY);
            }
            tick(helper, pos, machine);
            helper.assertTrue(machine.getItem(2).is(mob.equals("zombie") ? Items.ZOMBIE_SPAWN_EGG : Items.ENDERMAN_SPAWN_EGG),
                "The top output must contain the captured mob's spawn egg");
            helper.assertTrue(machine.getItem(3).is(mob.equals("zombie") ? Items.ROTTEN_FLESH : CrystalnexusModItems.SPATIAL_GLAND.get()),
                "The bottom output must use the ordinary drop or explicit recipe override");
            helper.assertTrue(machine.getBloodTank().getFluidAmount() == 500 && !PrisonCubeItem.hasStoredEntity(cube) && !cube.isEmpty(),
                "A completed cycle must consume 500 mB blood and return the empty cube");
            helper.assertTrue(machine.canTakeItemThroughFace(2, machine.getItem(2), Direction.DOWN)
                    && machine.canTakeItemThroughFace(3, machine.getItem(3), Direction.DOWN),
                "Automation must extract both outputs");
            machine.setItem(2, ItemStack.EMPTY);
            machine.setItem(3, ItemStack.EMPTY);
            machine.getBloodTank().drain(500, IFluidHandler.FluidAction.EXECUTE);
        }
        helper.succeed();
    }

    private static void tick(GameTestHelper helper, BlockPos pos, MasticatorBlockEntity machine) {
        for (int i = 0; i < 200; i++) {
            MasticatorBlockEntity.tick(helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos), machine);
        }
    }
}
