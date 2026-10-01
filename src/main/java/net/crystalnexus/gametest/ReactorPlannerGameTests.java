package net.crystalnexus.gametest;

import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.block.entity.ReactorCoreBlockEntity;
import net.crystalnexus.reactor.ReactorBalance;
import net.crystalnexus.reactor.ReactorSimulation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.crystalnexus.reactor.ReactorPlanner;
import net.crystalnexus.procedures.BlocksCheckerProcedure;
import net.crystalnexus.block.entity.ReactorComputerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus_reactor_planner")
@PrefixGameTestTemplate(false)
public final class ReactorPlannerGameTests {
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void generatedBuildsMatchLiveReactorRules(GameTestHelper helper) {
        for (int size = ReactorPlanner.MIN_SIZE; size <= ReactorPlanner.MAX_SIZE; size++) {
            var candidates = ReactorPlanner.candidates(size);
            helper.assertTrue(!candidates.isEmpty(), "Every supported size needs a build");
            for (var build : candidates) {
                helper.assertTrue(build.layout().valid && build.layout().activeCoolantChannels > 0,
                        "Generated cores must have controls and connected cooling");
                helper.assertTrue(build.blocks().size() == size * size * size, "No holes or overlapping shell ports");
                helper.assertTrue(build.layout().energyOutputs().size() == 1 && build.layout().wasteOutputs().size() == 1,
                        "Required reactor ports must be present");
            }
            var power = ReactorPlanner.optimize(candidates, 0, 100);
            var economy = ReactorPlanner.optimize(candidates, 100, 100);
            helper.assertTrue(power.fePerTick() >= economy.fePerTick(), "Power priority must maximize candidate output");
            helper.assertTrue(economy.build().layout().fuelEfficiency >= power.build().layout().fuelEfficiency,
                    "Efficiency priority must maximize candidate efficiency");
            var stopped = ReactorPlanner.estimate(power.build(), 0);
            var half = ReactorPlanner.estimate(power.build(), 50);
            helper.assertTrue(stopped.fePerTick() == 0 && stopped.burnPerTick() == 0 && stopped.coolantDemand() == 0,
                    "Inserted rods must stop generation and fuel wear");
            helper.assertTrue(half.fePerTick() <= power.fePerTick(), "Reducing speed must not increase output");
        }
        var build = ReactorPlanner.optimize(ReactorPlanner.candidates(5), 50, 100).build();
        build.blocks().forEach(helper::setBlock);
        BlockPos controllerPos = helper.absolutePos(new BlockPos(1, 1, 0));
        BlocksCheckerProcedure.executeFromController(helper.getLevel(), controllerPos);
        ReactorComputerBlockEntity controller = (ReactorComputerBlockEntity) helper.getLevel().getBlockEntity(controllerPos);
        helper.assertTrue(controller.getPersistentData().getBoolean("canOpenInventory"), "Preview build must form a real reactor");
        helper.assertTrue(controller.getCachedLayout().fuelRods == build.layout().fuelRods,
                "Preview and placed reactor must agree on core count");
        for (var rod : controller.getCachedLayout().fuelRods()) {
            ReactorCoreBlockEntity core = (ReactorCoreBlockEntity) helper.getLevel().getBlockEntity(rod.pos());
            for (int slot = 0; slot < 3; slot++) core.setItem(slot, new ItemStack(CrystalnexusModItems.BLUTONIUM_FUEL_CELL.get()));
        }
        controller.getFluidTank().fill(new FluidStack(Fluids.WATER, 10_000), IFluidHandler.FluidAction.EXECUTE);
        controller.getPersistentData().putDouble("heat", ReactorBalance.TARGET_TEMPERATURE);
        ReactorSimulation.tick(helper.getLevel(), controllerPos, controller);
        var expected = ReactorPlanner.estimate(build, 100);
        helper.assertTrue(Math.abs(controller.getPersistentData().getDouble("lastFEt") - expected.fePerTick()) <= 1,
                "Displayed FE/t must match the live simulation");
        helper.assertTrue(controller.getPersistentData().getDouble("coolantDemand") == expected.coolantDemand(),
                "Displayed cooling demand must match the live simulation");
        helper.succeed();
    }
}
