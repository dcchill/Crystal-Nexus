package net.crystalnexus.gametest;

import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.reactor.ReactorLayout;
import net.minecraft.world.level.block.Blocks;
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
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void copiedPlanPreviewsAndBuildsWithSavedRodSettings(GameTestHelper helper) {
        var build = ReactorPlanner.select(ReactorPlanner.candidates(5), 50, 65).build();
        var messages = new java.util.ArrayList<String>();
        var player = new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "PlannerTest")) {
            @Override public void displayClientMessage(net.minecraft.network.chat.Component message, boolean actionBar) {
                messages.add(message.getString());
            }
        };
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var packet = new net.crystalnexus.network.ReactorPlanSaveMessage(5, 65, ReactorPlanner.columns(build));
        net.crystalnexus.network.ReactorPlanSaveMessage.save(player, packet);
        helper.assertTrue(player.getMainHandItem().isEmpty(), "Copying must not grant a free plans item");
        var plans = new ItemStack(CrystalnexusModItems.MULTIBLOCK_PLANS.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, plans);
        net.crystalnexus.network.ReactorPlanSaveMessage.save(player, packet);
        helper.assertTrue(net.crystalnexus.item.MultiblockPlansItem.hasGeneratedReactor(plans), "Copy must write to the held plans item");
        var saved = plans.copy();
        net.crystalnexus.network.ReactorPlanSaveMessage.save(player, new net.crystalnexus.network.ReactorPlanSaveMessage(99, 65, new byte[0]));
        helper.assertTrue(ItemStack.isSameItemSameComponents(saved, plans), "Invalid packets must not overwrite a saved plan");
        build.blocks().keySet().forEach(pos -> helper.setBlock(pos, Blocks.AIR));
        BlockPos controller = new BlockPos(1, 1, 0);
        helper.setBlock(controller, build.getBlockState(controller));
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(controller)),
                net.minecraft.core.Direction.NORTH, helper.absolutePos(controller), false);
        var context = new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        plans.useOn(context);
        helper.assertTrue(helper.absolutePos(controller).equals(net.crystalnexus.item.MultiblockPlansItem.previewController(plans)),
                "First use must anchor the generated preview");
        plans.useOn(context);
        helper.assertTrue(messages.stream().anyMatch(message -> message.startsWith("Missing ")), "Survival construction must require its materials");
        helper.assertTrue(helper.getBlockState(new BlockPos(2, 1, 0)).isAir(), "Missing materials must prevent a survival build");
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        plans.useOn(context);
        helper.assertTrue(messages.contains("Multiblock built."), "Build was refused: " + messages);
        helper.runAfterDelay(30, () -> {
            for (var entry : build.blocks().entrySet()) {
                var actual = helper.getBlockState(entry.getKey());
                if (actual.is(CrystalnexusModBlocks.REACTOR_COMPUTER.get()))
                    actual = actual.setValue(net.crystalnexus.block.ReactorComputerBlock.BLOCKSTATE,
                            entry.getValue().getValue(net.crystalnexus.block.ReactorComputerBlock.BLOCKSTATE));
                helper.assertTrue(actual.equals(entry.getValue()), "Built block at " + entry.getKey() + " must match " + entry.getValue() + ", got " + actual);
                if (entry.getValue().is(CrystalnexusModBlocks.REACTOR_CONTROL_ROD.get())) {
                    var rod = (net.crystalnexus.block.entity.ReactorControlRodBlockEntity) helper.getBlockEntity(entry.getKey());
                    helper.assertTrue(rod.getInsertion() == 35, "Building must restore the planner's rod insertion");
                }
            }
            helper.assertTrue(net.crystalnexus.item.MultiblockPlansItem.hasGeneratedReactor(plans), "The saved design must remain reusable after building");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void generatedPlansRoundTripAndRotate(GameTestHelper helper) {
        var build = ReactorPlanner.select(ReactorPlanner.candidates(5), 50, 100).build();
        byte[] columns = ReactorPlanner.columns(build);
        helper.assertTrue(ReactorPlanner.fromColumns(5, columns).blocks().equals(build.blocks()), "Saved columns must preserve every preview block");
        var packet = new net.crystalnexus.network.ReactorPlanSaveMessage(5, 65, columns);
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
        net.crystalnexus.network.ReactorPlanSaveMessage.STREAM_CODEC.encode(buffer, packet);
        var decoded = net.crystalnexus.network.ReactorPlanSaveMessage.STREAM_CODEC.decode(buffer);
        buffer.release();
        helper.assertTrue(decoded.size() == 5 && decoded.speed() == 65 && java.util.Arrays.equals(decoded.columns(), columns), "Copy packet must retain layout and speed");
        var plans = new ItemStack(CrystalnexusModItems.MULTIBLOCK_PLANS.get());
        net.crystalnexus.item.MultiblockPlansItem.saveReactor(plans, build, decoded.speed());
        plans = ItemStack.parseOptional(helper.getLevel().registryAccess(), (net.minecraft.nbt.CompoundTag) plans.save(helper.getLevel().registryAccess()));
        helper.assertTrue(net.crystalnexus.item.MultiblockPlansItem.hasGeneratedReactor(plans), "Generated design must persist on the plans item");
        var controller = new BlockPos(8, 2, 8);
        for (var facing : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            helper.setBlock(controller, CrystalnexusModBlocks.REACTOR_COMPUTER.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, facing));
            var plan = net.crystalnexus.item.MultiblockPlansItem.readPlan(helper.getLevel(), helper.absolutePos(controller), plans);
            helper.assertTrue(plan.size() == build.blocks().size(), "Rotating a saved plan must retain its full volume");
            helper.assertTrue(plan.stream().anyMatch(block -> block.pos().equals(helper.absolutePos(controller))
                    && block.state().is(CrystalnexusModBlocks.REACTOR_COMPUTER.get())
                    && block.state().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING) == facing),
                    "Generated design must align its controller with the clicked controller");
        }
        for (byte[] invalid : new byte[][] { new byte[0], new byte[] { 99, 0, 0, 0 } }) {
            boolean rejected = false;
            try { ReactorPlanner.fromColumns(4, invalid); } catch (IllegalArgumentException expected) { rejected = true; }
            helper.assertTrue(rejected, "Malformed or non-whitelisted plan data must be rejected");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fluidInputPositionDoesNotChangeCooling(GameTestHelper helper) {
        var build = ReactorPlanner.candidates(7).getFirst();
        helper.assertTrue(build.layout().fluidInputs().size() == 1, "Generated builds need only one shell fluid input");
        var blocks = new java.util.LinkedHashMap<>(build.blocks());
        blocks.put(build.layout().fluidInputs().getFirst(), CrystalnexusModBlocks.REACTOR_BLOCK.get().defaultBlockState());
        blocks.put(new BlockPos(0, 5, 5), CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get().defaultBlockState());
        var moved = new ReactorPlanner.Build(7, blocks, null);
        var layout = ReactorLayout.analyze(moved, BlockPos.ZERO, new BlockPos(6, 6, 6));
        helper.assertTrue(layout.coolantCapacityMbT == build.layout().coolantCapacityMbT, "Any shell input must supply the shared reactor tank");
        blocks.put(new BlockPos(0, 5, 5), CrystalnexusModBlocks.REACTOR_BLOCK.get().defaultBlockState());
        helper.assertTrue(ReactorLayout.analyze(moved, BlockPos.ZERO, new BlockPos(6, 6, 6)).coolantCapacityMbT == 0,
                "A reactor without a fluid input must not claim supplied cooling");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void componentSearchImprovesSeedsForRequestedPriorities(GameTestHelper helper) {
        var candidates = ReactorPlanner.candidates(7);
        helper.assertTrue(candidates.stream().anyMatch(build -> build.blocks().values().stream()
                .anyMatch(state -> state.is(CrystalnexusModBlocks.REACTOR_HEAT_CONDUCTOR.get()))),
                "The search must explore conductor layouts as well as moderators and reflectors");
        for (int priority : new int[] { 0, 100 }) {
            var initial = ReactorPlanner.select(candidates, priority, 100);
            var result = ReactorPlanner.optimize(candidates, priority, 100);
            helper.assertTrue(result.build().layout().valid && result.coolantDemand() <= result.build().layout().coolantCapacityMbT,
                    "The recommended design must meet the requested speed without cooling throttling");
            helper.assertTrue(priority == 0 ? result.fePerTick() >= initial.fePerTick()
                    : ReactorPlanner.energyPerFuel(result) >= ReactorPlanner.energyPerFuel(initial),
                    "Component placement search must not regress its selected objective");
            var analyzed = ReactorLayout.analyze(result.build(), BlockPos.ZERO, new BlockPos(6, 6, 6));
            helper.assertTrue(analyzed.valid && analyzed.coolantCapacityMbT == result.build().layout().coolantCapacityMbT,
                    "Search result must agree with live cooling connectivity");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void conductorConnectionsAffectPlannerCoolingAndOutput(GameTestHelper helper) {
        var blocks = new java.util.LinkedHashMap<>(ReactorPlanner.candidates(7).getFirst().blocks());
        for (int x = 1; x < 6; x++) for (int z = 1; z < 6; z++) {
            blocks.put(new BlockPos(x, 0, z), CrystalnexusModBlocks.REACTOR_BLOCK.get().defaultBlockState());
            blocks.put(new BlockPos(x, 6, z), CrystalnexusModBlocks.REACTOR_BLOCK.get().defaultBlockState());
            for (int y = 1; y < 6; y++) blocks.put(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        }
        for (int y = 1; y < 6; y++) {
            blocks.put(new BlockPos(2, y, 2), CrystalnexusModBlocks.REACTOR_CORE.get().defaultBlockState());
            blocks.put(new BlockPos(2, y, 3), CrystalnexusModBlocks.REACTOR_HEAT_CONDUCTOR.get().defaultBlockState());
            blocks.put(new BlockPos(2, y, 4), CrystalnexusModBlocks.REACTOR_COOLANT_CHANNEL.get().defaultBlockState());
            blocks.put(new BlockPos(3, y, 4), CrystalnexusModBlocks.REACTOR_COOLANT_CHANNEL.get().defaultBlockState());
        }
        blocks.put(new BlockPos(2, 6, 2), CrystalnexusModBlocks.REACTOR_CONTROL_ROD.get().defaultBlockState());
        blocks.put(new BlockPos(2, 0, 4), CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get().defaultBlockState());
        var view = new ReactorPlanner.Build(7, java.util.Map.copyOf(blocks), null);
        var connected = new ReactorPlanner.Build(7, view.blocks(), ReactorLayout.analyze(view, BlockPos.ZERO, new BlockPos(6, 6, 6)));
        for (int y = 1; y < 6; y++) blocks.put(new BlockPos(2, y, 3), Blocks.AIR.defaultBlockState());
        view = new ReactorPlanner.Build(7, java.util.Map.copyOf(blocks), null);
        var disconnected = new ReactorPlanner.Build(7, view.blocks(), ReactorLayout.analyze(view, BlockPos.ZERO, new BlockPos(6, 6, 6)));
        helper.assertTrue(connected.layout().coolantCapacityMbT == 150 && disconnected.layout().coolantCapacityMbT == 0,
                "A conductor bridge must activate the supplied channel network");
        helper.assertTrue(ReactorPlanner.estimate(connected, 100).fePerTick() > ReactorPlanner.estimate(disconnected, 100).fePerTick(),
                "The planner must account for conductor cooling in its output estimate");
        helper.succeed();
    }

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
            var power = ReactorPlanner.select(candidates, 0, 100);
            var economy = ReactorPlanner.select(candidates, 100, 100);
            helper.assertTrue(power.fePerTick() >= economy.fePerTick(), "Power priority must maximize candidate output");
            helper.assertTrue(ReactorPlanner.energyPerFuel(economy) >= ReactorPlanner.energyPerFuel(power),
                    "Efficiency priority must maximize energy obtained per fuel wear");
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
