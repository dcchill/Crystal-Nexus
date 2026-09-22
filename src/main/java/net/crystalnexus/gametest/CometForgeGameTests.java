package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.*;
import net.crystalnexus.init.*;
import net.crystalnexus.item.ResourceCometItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.List;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class CometForgeGameTests {
    private static final BlockPos CONTROLLER = new BlockPos(3, 1, 0);

    private static List<BlockPos> casing(GameTestHelper helper) {
        List<BlockPos> result = new ArrayList<>();
        for (int x = 0; x < 7; x++) for (int y = 1; y <= 4; y++) for (int z = 0; z < 7; z++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (helper.getBlockState(pos).is(CrystalnexusModBlocks.METEORITE_ALLOY_BLOCK.get())) result.add(pos);
        }
        return result;
    }
    private static CometForgeControllerBlockEntity setup(GameTestHelper helper) {
        var state = helper.getBlockState(CONTROLLER);
        helper.setBlock(CONTROLLER, Blocks.AIR);
        helper.setBlock(CONTROLLER, state);
        CometForgeControllerBlockEntity forge = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(!forge.validateStructureNow(), "Ports are required");
        var casing = casing(helper);
        helper.setBlock(casing.get(0), CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get());
        helper.assertTrue(!forge.validateStructureNow(), "Fluid port is required too");
        helper.setBlock(casing.get(1), CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get());
        helper.assertTrue(forge.validateStructureNow(), "Forge must form from the supplied NBT");
        MachineEnergyInputBlockEntity energy = helper.getBlockEntity(casing.get(0));
        MachineFluidInputBlockEntity fluid = helper.getBlockEntity(casing.get(1));
        helper.assertTrue(energy.isBoundTo(forge.getBlockPos()) && fluid.isBoundTo(forge.getBlockPos()), "Ports bind to forge");
        fluid.getFluidInput().fill(new FluidStack(CrystalnexusModFluids.TEMPORAL_ESSENCE.get(), 25_000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(forge.getTemporalFluidTank().getFluidAmount() == 25_000, "Fluid port forwards to controller");
        return forge;
    }
    private static void ingredients(CometForgeControllerBlockEntity forge, ItemStack material) {
        forge.setItem(0, new ItemStack(CrystalnexusModItems.DIAMOND_SINGULARITY.get()));
        forge.setItem(1, new ItemStack(CrystalnexusModItems.ENERGY_SINGULARITY.get()));
        forge.setItem(2, new ItemStack(CrystalnexusModItems.EMERALD_SINGULARITY.get()));
        forge.setItem(3, material);
    }
    private static void tick(CometForgeControllerBlockEntity forge, int count) {
        for (int i = 0; i < count; i++) {
            forge.multiblockEnergyInput().receiveEnergy(2500, false);
            forge.serverTick();
        }
    }
    @GameTest(template = "comet_forge")
    public static void craftsAndReloads(GameTestHelper helper) {
        var forge = setup(helper);
        ingredients(forge, new ItemStack(Items.IRON_INGOT, 64));
        forge.serverTick();
        helper.assertTrue(forge.getProgress() == 0, "No progress without energy");
        tick(forge, 80);
        helper.assertTrue(forge.getProgress() == 80 && forge.getItem(3).getCount() == 64, "Ingredients remain until completion");
        var saved = forge.saveWithoutMetadata(helper.getLevel().registryAccess());
        forge.loadAdditional(saved, helper.getLevel().registryAccess());
        forge.validateStructureNow();
        tick(forge, 120);
        helper.assertTrue(ResourceCometItem.material(forge.getItem(4)).is(Items.IRON_INGOT), "Correct comet after reload");
        helper.assertTrue(forge.getItem(0).isEmpty() && forge.getItem(1).isEmpty() && forge.getItem(2).isEmpty()
            && forge.getItem(3).isEmpty(), "Exactly three mixed singularities and full material stack consumed");
        helper.assertTrue(forge.getTemporalFluidTank().isEmpty() && forge.multiblockEnergyInput().getEnergyStored() == 0, "Exact costs charged");
        helper.succeed();
    }
    @GameTest(template = "comet_forge", rotationSteps = 1)
    public static void rotatedForge(GameTestHelper helper) { verifyStructure(helper); }
    @GameTest(template = "comet_forge", rotationSteps = 2)
    public static void reversedForge(GameTestHelper helper) { verifyStructure(helper); }
    @GameTest(template = "comet_forge", rotationSteps = 3)
    public static void thirdRotation(GameTestHelper helper) { verifyStructure(helper); }
    private static void verifyStructure(GameTestHelper helper) {
        var forge = setup(helper);
        BlockPos broken = casing(helper).getFirst();
        var state = helper.getBlockState(broken);
        helper.setBlock(broken, Blocks.AIR);
        helper.assertTrue(!forge.validateStructureNow(), "Broken casing invalidates forge");
        helper.setBlock(broken, state);
        helper.assertTrue(forge.validateStructureNow(), "Repair restores forge");
        forge.onControllerRemoved();
        helper.assertTrue(!forge.isFormed(), "Controller removal clears formation");
        helper.succeed();
    }
    @GameTest(template = "comet_forge")
    public static void inputRulesAndMaterialTag(GameTestHelper helper) {
        var forge = setup(helper);
        var named = new ItemStack(Items.IRON_INGOT, 64);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Custom"));
        helper.assertTrue(!forge.canPlaceItem(3, named) && !ResourceCometItem.isMaterial(new ItemStack(Items.DIAMOND_SWORD)), "Reject modified and unstackable materials");
        helper.assertTrue(!forge.canPlaceItemThroughFace(0, new ItemStack(Items.DIRT), Direction.UP)
            && !forge.canPlaceItem(4, new ItemStack(Items.DIRT)), "Automation obeys slot rules");
        helper.assertTrue(forge.canTakeItemThroughFace(4, ItemStack.EMPTY, Direction.DOWN)
            && !forge.canTakeItemThroughFace(0, ItemStack.EMPTY, Direction.DOWN), "Automation only extracts output");
        helper.assertTrue(ResourceCometItem.isMaterial(new ItemStack(Items.IRON_INGOT))
            && ResourceCometItem.isMaterial(new ItemStack(Items.RAW_IRON)), "Tag inherits ingots and raw materials");
        helper.assertTrue(!forge.canPlaceItem(3, new ItemStack(Items.ENDER_PEARL))
            && !forge.canPlaceItem(3, new ItemStack(Items.DIAMOND))
            && ResourceCometItem.create(new ItemStack(Items.DIRT)).isEmpty(), "Untagged materials cannot craft comets");
        helper.assertTrue(ResourceCometItem.materials().stream().allMatch(stack -> stack.is(ResourceCometItem.COMET_MATERIAL)),
            "JEI material enumeration respects the tag");
        ingredients(forge, new ItemStack(Items.RAW_IRON, 63));
        tick(forge, 1);
        helper.assertTrue(forge.getProgress() == 0, "Partial stack cannot craft");
        forge.multiblockEnergyInput().extractEnergy(2500, false);
        forge.setItem(3, new ItemStack(Items.RAW_IRON, 64));
        tick(forge, 200);
        helper.assertTrue(ResourceCometItem.material(forge.getItem(4)).is(Items.RAW_IRON), "Tagged raw material crafts");
        ItemStack legacy = new ItemStack(CrystalnexusModItems.RESOURCE_COMET.get());
        legacy.set(CrystalnexusModDataComponents.MATERIAL.get(), ResourceLocation.parse("minecraft:ender_pearl"));
        helper.assertTrue(ResourceCometItem.material(legacy).is(Items.ENDER_PEARL), "Existing comets retain their material");
        helper.succeed();
    }
    @GameTest(template = "comet_forge")
    public static void pausesAndChangesInputs(GameTestHelper helper) {
        var forge = setup(helper);
        ingredients(forge, new ItemStack(Items.IRON_INGOT, 64));
        forge.setItem(4, new ItemStack(Items.DIRT));
        tick(forge, 1);
        helper.assertTrue(forge.getProgress() == 0 && forge.getTemporalFluidTank().getFluidAmount() == 25_000, "Blocked output charges nothing");
        forge.setItem(4, ItemStack.EMPTY);
        forge.serverTick();
        helper.assertTrue(forge.getProgress() == 1, "Cleared output resumes");
        forge.setItem(3, new ItemStack(Items.GOLD_INGOT, 64));
        forge.serverTick();
        helper.assertTrue(forge.getProgress() == 0, "Different material resets progress");
        tick(forge, 2);
        var remaining = forge.getTemporalFluidTank().drain(100_000, IFluidHandler.FluidAction.EXECUTE);
        tick(forge, 1);
        helper.assertTrue(forge.getProgress() == 2, "Missing fluid pauses");
        forge.getTemporalFluidTank().fill(remaining, IFluidHandler.FluidAction.EXECUTE);
        forge.serverTick();
        helper.assertTrue(forge.getProgress() == 3, "Fluid restores progress");
        ItemStack invalid = new ItemStack(CrystalnexusModItems.RESOURCE_COMET.get());
        helper.assertTrue(ResourceCometItem.material(invalid).isEmpty(), "Missing target rejected");
        invalid.set(CrystalnexusModDataComponents.MATERIAL.get(), ResourceLocation.parse("missing:item"));
        helper.assertTrue(ResourceCometItem.material(invalid).isEmpty(), "Unknown target rejected");
        invalid.set(CrystalnexusModDataComponents.MATERIAL.get(), ResourceLocation.parse("crystalnexus:resource_comet"));
        helper.assertTrue(ResourceCometItem.material(invalid).isEmpty(), "Recursive target rejected");
        helper.succeed();
    }
    @GameTest(template = "solar_sim")
    public static void simulatorCometsAndStars(GameTestHelper helper) {
        var size = helper.getLevel().getStructureManager().get(ResourceLocation.parse("crystalnexus:solar_sim")).orElseThrow().getSize();
        BlockPos controllerPos = null;
        List<BlockPos> ports = new ArrayList<>();
        for (int x = 0; x < size.getX(); x++) for (int y = 1; y <= size.getY(); y++) for (int z = 0; z < size.getZ(); z++) {
            BlockPos pos = new BlockPos(x, y, z);
            var state = helper.getBlockState(pos);
            if (state.is(CrystalnexusModBlocks.SOLAR_SIMULATOR_CONTROLLER.get())) controllerPos = pos;
            if (state.is(CrystalnexusModBlocks.TUNGSTEN_BLOCK.get())) ports.add(pos);
        }
        helper.assertTrue(controllerPos != null && ports.size() >= 3, "Simulator anchor and port positions exist");
        var state = helper.getBlockState(controllerPos);
        helper.setBlock(controllerPos, Blocks.AIR);
        helper.setBlock(controllerPos, state);
        helper.setBlock(ports.get(0), CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get());
        helper.setBlock(ports.get(1), CrystalnexusModBlocks.MULTIBLOCK_ITEM_OUTPUT.get());
        SolarSimulatorControllerBlockEntity simulator = helper.getBlockEntity(controllerPos);
        helper.assertTrue(simulator.validateStructureNow(), "Simulator forms");
        MultiblockItemOutputBlockEntity output = helper.getBlockEntity(ports.get(1));
        var stars = List.of(CrystalnexusModItems.YELLOW_DWARF_STAR.get(), CrystalnexusModItems.ORANGE_STAR.get(),
            CrystalnexusModItems.BLUE_STAR.get(), CrystalnexusModItems.PINK_STAR.get());
        for (int star = 0; star < stars.size(); star++) {
            output.clearContent();
            for (int slot = 0; slot < 4; slot++) {
                var comet = ResourceCometItem.create(new ItemStack(Items.IRON_INGOT));
                helper.assertTrue(simulator.canPlaceItem(slot, comet), "All planet slots accept comets");
                simulator.setItem(slot, comet);
            }
            simulator.setItem(4, new ItemStack(stars.get(star)));
            long before = simulator.multiblockEnergyInput().getEnergyStored();
            long added = 0;
            for (int tick = 0; tick < SolarSimulatorControllerBlockEntity.DURATION; tick++) {
                added += simulator.multiblockEnergyInput().receiveEnergy(Integer.MAX_VALUE, false);
                simulator.serverTick();
            }
            int count = 0;
            for (int slot = 0; slot < output.getContainerSize(); slot++) {
                ItemStack stack = output.getItem(slot);
                helper.assertTrue(stack.isEmpty() || stack.is(Items.IRON_INGOT), "Output must be exact target");
                count += stack.getCount();
            }
            helper.assertTrue(count == 4 * (1 << star), "Star multiplier applies to every comet");
            helper.assertTrue(before + added - simulator.multiblockEnergyInput().getEnergyStored() == count * 200_000L, "Existing energy per item retained");
            for (int slot = 0; slot < 4; slot++) helper.assertTrue(simulator.getItem(slot).getCount() == 1, "Comets are reusable");
        }
        output.clearContent();
        for (int slot = 0; slot < 4; slot++) simulator.setItem(slot, ItemStack.EMPTY);
        simulator.setItem(0, new ItemStack(CrystalnexusModItems.NOX.get()));
        simulator.setItem(4, new ItemStack(CrystalnexusModItems.YELLOW_DWARF_STAR.get()));
        for (int i = 0; i < SolarSimulatorControllerBlockEntity.DURATION; i++) {
            simulator.multiblockEnergyInput().receiveEnergy(Integer.MAX_VALUE, false); simulator.serverTick();
        }
        helper.assertTrue(!output.isEmpty(), "Existing planet still produces output");
        helper.setBlock(ports.get(1), CrystalnexusModBlocks.MULTIBLOCK_FLUID_OUTPUT.get());
        helper.assertTrue(simulator.validateStructureNow(), "Fluid mode forms");
        simulator.setItem(0, ResourceCometItem.create(new ItemStack(Items.IRON_INGOT)));
        int before = simulator.multiblockEnergyInput().getEnergyStored();
        for (int i = 0; i < 6; i++) simulator.serverTick();
        helper.assertTrue(simulator.getProgress() == 0 && simulator.multiblockEnergyInput().getEnergyStored() == before,
            "Comets do not run in fluid mode");
        helper.assertTrue(!simulator.canPlaceItem(0, new ItemStack(CrystalnexusModItems.RESOURCE_COMET.get())), "Malformed comet rejected");
        helper.setBlock(ports.get(1), CrystalnexusModBlocks.MULTIBLOCK_ITEM_OUTPUT.get());
        helper.setBlock(ports.get(2), CrystalnexusModBlocks.MULTIBLOCK_FLUID_OUTPUT.get());
        helper.assertTrue(simulator.validateStructureNow(), "Combined output mode forms");
        output = helper.getBlockEntity(ports.get(1));
        MultiblockFluidOutputBlockEntity fluidOutput = helper.getBlockEntity(ports.get(2));
        simulator.setItem(0, new ItemStack(CrystalnexusModItems.CAELUS.get()));
        for (int i = 0; i < SolarSimulatorControllerBlockEntity.DURATION; i++) {
            simulator.multiblockEnergyInput().receiveEnergy(Integer.MAX_VALUE, false); simulator.serverTick();
        }
        helper.assertTrue(!output.isEmpty() && !fluidOutput.getFluidOutput().getFluidInTank(0).isEmpty(),
            "Combined output mode produces both materials and fluids");
        helper.succeed();
    }

}
