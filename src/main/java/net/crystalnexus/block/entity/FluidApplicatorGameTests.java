package net.crystalnexus.block.entity;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.crystalnexus.block.ProcessingModuleBlock.Operation;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.recipe.ProcessingLineRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

@GameTestHolder("crystalnexus_processing")
@PrefixGameTestTemplate(false)
public final class FluidApplicatorGameTests {
    private FluidApplicatorGameTests() {}

    @GameTest(template = "zero_point")
    public static void fluidRecipeCodecsValidateAndRoundTrip(GameTestHelper h) {
        var ops = RegistryOps.create(JsonOps.INSTANCE, h.getLevel().registryAccess());
        var serializer = ProcessingLineRecipe.Serializer.INSTANCE;
        for (String id : List.of("processing_applied_leather", "processing_applied_carbon_fiber", "processing_battery_part")) {
            var recipe = (ProcessingLineRecipe) h.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath("crystalnexus", id)).orElseThrow().value();
            var json = serializer.codec().codec().encodeStart(ops, recipe).getOrThrow();
            var decoded = serializer.codec().codec().parse(ops, json).getOrThrow();
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                serializer.streamCodec().encode(buffer, decoded);
                var network = serializer.streamCodec().decode(buffer);
                h.assertTrue(network.steps().size() == recipe.steps().size(), "Step count must survive both codecs");
                for (int i = 0; i < recipe.steps().size(); i++) {
                    var expected = recipe.steps().get(i);
                    var actual = network.steps().get(i);
                    h.assertTrue(actual.operation() == expected.operation() && actual.time() == expected.time()
                        && actual.energyPerTick() == expected.energyPerTick()
                        && actual.fluid().isPresent() == expected.fluid().isPresent(), "Step fields must round trip");
                    expected.fluid().ifPresent(fluid -> h.assertTrue(
                        FluidStack.matches(fluid, actual.fluid().orElseThrow()), "Fluid identity and quantity must round trip"));
                }
            } finally { buffer.release(); }
        }
        String base = "{\"input\":{\"item\":\"minecraft:rotten_flesh\"},\"steps\":[%s],\"result\":{\"id\":\"minecraft:leather\"}}";
        for (String step : List.of(
                "{\"module\":\"apply_fluid\"}",
                "{\"module\":\"apply_fluid\",\"fluid\":{\"fluid\":\"minecraft:empty\",\"amount\":100}}",
                "{\"module\":\"apply_fluid\",\"fluid\":{\"fluid\":\"crystalnexus:resin\",\"amount\":0}}",
                "{\"module\":\"apply_fluid\",\"fluid\":{\"fluid\":\"crystalnexus:resin\",\"amount\":4001}}",
                "{\"module\":\"press\",\"fluid\":{\"fluid\":\"crystalnexus:resin\",\"amount\":100}}",
                "{\"module\":\"apply_fluid\",\"ingredient\":{\"item\":\"minecraft:stone\"},\"fluid\":{\"fluid\":\"crystalnexus:resin\",\"amount\":100}}")) {
            h.assertTrue(serializer.codec().codec().parse(ops, JsonParser.parseString(base.formatted(step))).error().isPresent(),
                "Invalid fluid step must be rejected: " + step);
        }
        h.succeed();
    }

    @GameTest(template = "zero_point")
    public static void pausesAndPersistsWithoutConsumingFluidEarly(GameTestHelper h) {
        var module = place(h, 2);
        var belt = belt(module);
        belt.setItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT, new ItemStack(Items.ROTTEN_FLESH, 2));
        module.energy().receiveEnergy(1000, false);
        tick(belt, 1);
        h.assertTrue(module.energyStored() == 1000 && !module.isWorking(), "Empty tank must pause before spending energy");
        module.fluidTank().fill(new FluidStack(Fluids.WATER, 4000), FluidAction.EXECUTE);
        tick(belt, 1);
        h.assertTrue(module.energyStored() == 1000, "Wrong fluid must not spend energy");
        module.fluidTank().drain(4000, FluidAction.EXECUTE);
        module.fluidTank().fill(resin(199), FluidAction.EXECUTE);
        tick(belt, 1);
        h.assertTrue(module.energyStored() == 1000, "A batch needs its full fluid cost");
        module.fluidTank().fill(resin(1), FluidAction.EXECUTE);
        tick(belt, 5);
        h.assertTrue(module.data().get(0) == 5 && module.fluidTank().getFluidAmount() == 200,
            "Fluid is consumed only on completion");
        module.energy().extractEnergy(Integer.MAX_VALUE, false);
        tick(belt, 1);
        h.assertTrue(module.data().get(0) == 5 && !module.isWorking(), "Power loss must retain progress and stop spraying");
        var savedModule = module.saveWithFullMetadata(h.getLevel().registryAccess());
        var savedBelt = belt.saveWithFullMetadata(h.getLevel().registryAccess());
        module.fluidTank().drain(4000, FluidAction.EXECUTE);
        module.loadWithComponents(savedModule, h.getLevel().registryAccess());
        belt.loadWithComponents(savedBelt, h.getLevel().registryAccess());
        h.assertTrue(module.fluidTank().getFluidAmount() == 200, "Tank contents must survive save/load");
        module.fluidTank().drain(1, FluidAction.EXECUTE);
        module.energy().receiveEnergy(1000, false);
        tick(belt, 1);
        h.assertTrue(module.data().get(0) == 5 && module.energyStored() == 1000, "Fluid loss mid-cycle must pause safely");
        module.fluidTank().fill(resin(1), FluidAction.EXECUTE);
        for (int i = 0; i < 35; i++) { module.energy().receiveEnergy(1000, false); tick(belt, 1); }
        h.assertTrue(belt.getItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT).is(Items.LEATHER)
            && belt.getItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT).getCount() == 2
            && module.fluidTank().isEmpty(), "Resumed batch must consume exactly 200 mB and make two leather");
        h.succeed();
    }

    @GameTest(template = "zero_point")
    public static void fullStacksSplitToTankCapacityAndRespectBlockedExit(GameTestHelper h) {
        var module = place(h, 2);
        var belt = belt(module);
        int work = ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT, exit = work + 1;
        belt.setItem(work, new ItemStack(Items.ROTTEN_FLESH, 64));
        belt.setItem(exit, new ItemStack(Items.STONE));
        module.fluidTank().fill(resin(4000), FluidAction.EXECUTE);
        module.energy().receiveEnergy(1000, false);
        tick(belt, 1);
        h.assertTrue(module.energyStored() == 1000 && module.fluidTank().getFluidAmount() == 4000,
            "Blocked partial-batch exit must consume no resources");
        belt.setItem(exit, ItemStack.EMPTY);
        for (int i = 0; i < 40; i++) { module.energy().receiveEnergy(1000, false); module.energy().receiveEnergy(1000, false); tick(belt, 1); }
        h.assertTrue(belt.getItem(work).getCount() == 24 && belt.getItem(exit).is(Items.LEATHER)
            && belt.getItem(exit).getCount() == 40 && module.fluidTank().isEmpty(),
            "A full tank must process 40 of 64 items and retain 24");
        belt.setItem(exit, ItemStack.EMPTY);
        module.fluidTank().fill(resin(2400), FluidAction.EXECUTE);
        for (int i = 0; i < 40; i++) { module.energy().receiveEnergy(1000, false); tick(belt, 1); }
        h.assertTrue(belt.getItem(work).is(Items.LEATHER) && belt.getItem(work).getCount() == 24
            && module.fluidTank().isEmpty(), "Remaining items must finish without duplication or extra fluid");
        h.succeed();
    }

    @GameTest(template = "zero_point")
    public static void fluidCapabilitiesAndContainersConserveContents(GameTestHelper h) {
        var module = place(h, 2);
        for (Direction side : Direction.values()) h.assertTrue(h.getLevel().getCapability(
            Capabilities.FluidHandler.BLOCK, module.getBlockPos(), side) != null, "Pipes must connect on every side");
        var handler = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, module.getBlockPos(), (Direction) null);
        var filled = FluidUtil.tryEmptyContainer(new ItemStack(CrystalnexusModItems.RESIN_BUCKET.get()), handler, 1000, null, true);
        h.assertTrue(filled.isSuccess() && filled.getResult().is(Items.BUCKET) && module.fluidTank().getFluidAmount() == 1000,
            "Filling must return an empty bucket and transfer 1000 mB");
        var rejected = FluidUtil.tryEmptyContainer(new ItemStack(Items.WATER_BUCKET), handler, 1000, null, true);
        h.assertTrue(!rejected.isSuccess() && module.fluidTank().getFluidAmount() == 1000,
            "Incompatible container contents must remain untouched");
        var drained = FluidUtil.tryFillContainer(new ItemStack(Items.BUCKET), handler, 1000, null, true);
        h.assertTrue(drained.isSuccess() && drained.getResult().is(CrystalnexusModItems.RESIN_BUCKET.get())
            && module.fluidTank().isEmpty(), "Draining must return a resin bucket with no loss");
        h.succeed();
    }

    @GameTest(template = "zero_point")
    public static void consecutiveApplicatorsHandOffAndCarbonFiberNeedsPress(GameTestHelper h) {
        var first = place(h, 2);
        var second = place(h, 3);
        var manager = h.getLevel().getRecipeManager();
        var original = List.copyOf(manager.getRecipes());
        var recipe = new ProcessingLineRecipe(Ingredient.of(Items.STONE), List.of(
            new ProcessingLineRecipe.Step(Operation.APPLY_FLUID, Ingredient.EMPTY, 2, 20, Optional.of(resin(100))),
            new ProcessingLineRecipe.Step(Operation.APPLY_FLUID, Ingredient.EMPTY, 2, 20, Optional.of(resin(50)))), new ItemStack(Items.DIAMOND));
        var recipes = new java.util.ArrayList<RecipeHolder<?>>(original);
        recipes.add(new RecipeHolder<>(ResourceLocation.parse("crystalnexus:test_fluid_chain"), recipe));
        manager.replaceRecipes(recipes);
        try {
            int work = ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT;
            belt(first).setItem(work, new ItemStack(Items.STONE));
            first.fluidTank().fill(resin(200), FluidAction.EXECUTE);
            first.energy().receiveEnergy(1000, false);
            tick(belt(first), 2);
            h.assertTrue(belt(first).getItem(work).isEmpty() && !belt(first).getItem(work + 1).isEmpty(),
                "Identical operations must hand off to the next machine");
            tick(belt(first), 3);
            h.assertTrue(first.fluidTank().getFluidAmount() == 100, "First applicator must not perform the next identical step");
            belt(second).setItem(work, belt(first).getItem(work + 1).copy());
            belt(first).setItem(work + 1, ItemStack.EMPTY);
            second.fluidTank().fill(resin(50), FluidAction.EXECUTE);
            second.energy().receiveEnergy(1000, false);
            tick(belt(second), 2);
            h.assertTrue(belt(second).getItem(work).is(Items.DIAMOND) && second.fluidTank().isEmpty(), "Second applicator completes its own step");
            belt(first).setItem(work, new ItemStack(CrystalnexusModItems.CARBON_COMPOSITE.get()));
            for (int i = 0; i < 40; i++) { first.energy().receiveEnergy(1000, false); tick(belt(first), 1); }
            h.assertTrue(belt(first).getItem(work).is(CrystalnexusModItems.CARBON_COMPOSITE.get())
                && first.fluidTank().getFluidAmount() == 50, "Resin treatment must leave carbon composite awaiting its press step");
            var pos = second.getBlockPos();
            h.getLevel().setBlockAndUpdate(pos, CrystalnexusModBlocks.PROCESSING_PRESS_MODULE.get().defaultBlockState());
            var press = (ProcessingModuleBlockEntity) h.getLevel().getBlockEntity(pos);
            belt(press).setItem(work, belt(first).getItem(work).copy());
            press.setItem(0, new ItemStack(CrystalnexusModItems.CERAMIC_PLATE.get()));
            for (int i = 0; i < 20; i++) { press.energy().receiveEnergy(1000, false); tick(belt(press), 1); }
            h.assertTrue(belt(press).getItem(work).is(CrystalnexusModItems.CARBON_FIBER.get()) && press.material().isEmpty(),
                "Pressing treated composite must consume a ceramic plate and produce carbon fiber");
        } finally { manager.replaceRecipes(original); }
        h.succeed();
    }

    private static ProcessingModuleBlockEntity place(GameTestHelper h, int x) {
        var pos = h.absolutePos(new BlockPos(x, 3, 2));
        h.getLevel().setBlockAndUpdate(pos, CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.above(), CrystalnexusModBlocks.FLUID_APPLICATOR.get().defaultBlockState());
        return (ProcessingModuleBlockEntity) h.getLevel().getBlockEntity(pos.above());
    }

    private static ConveyerBeltBaseBlockEntity belt(ProcessingModuleBlockEntity module) {
        return (ConveyerBeltBaseBlockEntity) module.getLevel().getBlockEntity(module.getBlockPos().below());
    }

    private static FluidStack resin(int amount) { return new FluidStack(CrystalnexusModFluids.RESIN.get(), amount); }

    private static void tick(ConveyerBeltBaseBlockEntity belt, int ticks) {
        for (int i = 0; i < ticks; i++) {
            for (int segment = 0; segment < ConveyerBeltBaseBlockEntity.SEGMENTS; segment++) {
                var stack = belt.getItem(segment);
                if (stack.isEmpty()) continue;
                CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                data.remove("CrystalNexusProcessingLastTick");
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            }
            belt.processProcessingModules();
        }
    }
}
