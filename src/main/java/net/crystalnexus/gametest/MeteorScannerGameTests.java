package net.crystalnexus.gametest;

import com.mojang.authlib.GameProfile;
import net.crystalnexus.block.entity.SolarSimulatorControllerBlockEntity;
import net.crystalnexus.block.entity.SolarSimulatorItemHandler;
import net.crystalnexus.data.SummonedMeteorSavedData;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.item.BatteryData;
import net.crystalnexus.item.MeteorDataDiskItem;
import net.crystalnexus.item.MeteorScannerItem;
import net.crystalnexus.item.ToolEnergy;
import net.crystalnexus.multiblock.HeavenshatterMeteor;
import net.crystalnexus.multiblock.RecordedMeteor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.resources.ResourceLocation;
import net.crystalnexus.block.entity.MultiblockItemOutputBlockEntity;
import java.util.UUID;

@GameTestHolder("crystalnexus_heavenshatter")
@PrefixGameTestTemplate(false)
public final class MeteorScannerGameTests {
    private static ItemStack disk(RecordedMeteor meteor) {
        ItemStack disk = new ItemStack(CrystalnexusModItems.METEOR_DATA_DISK.get());
        MeteorDataDiskItem.write(disk, meteor);
        return disk;
    }

    private static void ticks(MeteorScannerItem scanner, Player player, int first, int last) {
        for (int tick = first; tick <= last; tick++)
            scanner.onUseTick(player.level(), player, player.getMainHandItem(), MeteorScannerItem.SCAN_TICKS - tick + 1);
    }

    private static void aim(Player player, BlockPos target, double distance) {
        player.setPos(target.getX() + 0.5, target.getY() + 0.5 - player.getEyeHeight(), target.getZ() - distance);
        player.setYRot(0);
        player.setXRot(0);
    }

    @GameTest(template = "zero_point")
    public static void scannerCompletionOverwriteAndCancellation(GameTestHelper helper) {
        var level = helper.getLevel();
        var data = SummonedMeteorSavedData.get(level);
        BlockPos base = helper.absolutePos(new BlockPos(4, 24, 4));
        var palette = List.of(Blocks.STONE.defaultBlockState(), Blocks.IRON_ORE.defaultBlockState());
        RecordedMeteor meteor = data.register(base, 7, 1234, palette);
        var cell = HeavenshatterMeteor.cells(7, 1234, 2).stream()
            .filter(c -> c.x() == 3 && c.y() == 3 && c.z() == 0).findFirst().orElseThrow();
        BlockPos target = base.offset(cell.x(), cell.y(), cell.z());
        level.setBlock(target, palette.get(cell.material()), 3);
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "meteor-scanner"));
        player.getAbilities().instabuild = false;
        var scanner = (MeteorScannerItem) CrystalnexusModItems.METEOR_SCANNER.get();
        ItemStack tool = new ItemStack(scanner);
        ItemStack disk = new ItemStack(CrystalnexusModItems.METEOR_DATA_DISK.get());
        player.setItemSlot(EquipmentSlot.MAINHAND, tool);
        player.setItemSlot(EquipmentSlot.OFFHAND, disk);
        aim(player, target, 5);

        helper.assertTrue(!scanner.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "Empty scanner fails");
        BatteryData.setEnergy(tool, 20_000, ToolEnergy.CAPACITY);
        helper.assertTrue(scanner.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "Valid target starts scanning");
        ticks(scanner, player, 1, 99);
        helper.assertTrue(MeteorDataDiskItem.read(disk).isEmpty() && BatteryData.getEnergy(tool) == 20_000, "No disk mutation or charge before completion");
        ticks(scanner, player, 100, 100);
        helper.assertTrue(MeteorDataDiskItem.read(disk).orElseThrow().equals(meteor) && BatteryData.getEnergy(tool) == 15_000, "Exactly 100 ticks records original composition and costs 5000 FE");
        helper.assertTrue(!player.isUsingItem(), "One hold completes only one scan");

        var previous = RecordedMeteor.create(base, 7, 4321, palette);
        MeteorDataDiskItem.write(disk, previous);
        scanner.use(level, player, InteractionHand.MAIN_HAND);
        ticks(scanner, player, 1, 20);
        player.releaseUsingItem();
        helper.assertTrue(MeteorDataDiskItem.read(disk).orElseThrow().equals(previous) && BatteryData.getEnergy(tool) == 15_000, "Release preserves old record and FE");

        scanner.use(level, player, InteractionHand.MAIN_HAND);
        ticks(scanner, player, 1, 20);
        player.setYRot(180);
        ticks(scanner, player, 21, 21);
        helper.assertTrue(!player.isUsingItem() && BatteryData.getEnergy(tool) == 15_000, "Losing the target cancels without charging");
        aim(player, target, 5);
        scanner.use(level, player, InteractionHand.MAIN_HAND);
        player.setItemSlot(EquipmentSlot.OFFHAND, disk.copy());
        ticks(scanner, player, 1, 1);
        helper.assertTrue(!player.isUsingItem(), "Replacing even an identical disk cancels the scan");
        player.setItemSlot(EquipmentSlot.OFFHAND, disk);
        scanner.use(level, player, InteractionHand.MAIN_HAND);
        BatteryData.setEnergy(tool, 4999, ToolEnergy.CAPACITY);
        ticks(scanner, player, 1, 1);
        helper.assertTrue(!player.isUsingItem() && BatteryData.getEnergy(tool) == 4999, "Power loss cancels without partial charge");

        BatteryData.setEnergy(tool, 15_000, ToolEnergy.CAPACITY);
        aim(player, target, MeteorScannerItem.RANGE + 1);
        helper.assertTrue(!scanner.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "Out-of-range meteor rejected");
        aim(player, target, 5);
        level.setBlock(target.north(), Blocks.OBSIDIAN.defaultBlockState(), 3);
        helper.assertTrue(!scanner.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "Intervening ordinary block blocks scanning");
        level.setBlock(target.north(), Blocks.AIR.defaultBlockState(), 3);
        scanner.use(level, player, InteractionHand.MAIN_HAND);
        ticks(scanner, player, 1, 100);
        helper.assertTrue(MeteorDataDiskItem.read(disk).orElseThrow().equals(meteor) && BatteryData.getEnergy(tool) == 10_000, "Successful rescan overwrites old disk");

        level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(data.at(level, target) == null && meteor.totalWeight() > 1, "Mining removes the target without changing original weights");
        level.setBlock(target, palette.get(cell.material()), 3);
        helper.assertTrue(SummonedMeteorSavedData.load(data.save(new CompoundTag(), level.registryAccess()), level.registryAccess()).at(level, target).equals(meteor), "Saved meteor identity and composition survive reload");
        player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        helper.assertTrue(!scanner.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "Offhand disk required");
        player.setItemSlot(EquipmentSlot.OFFHAND, disk);
        player.getAbilities().instabuild = true;
        BatteryData.setEnergy(tool, 0, ToolEnergy.CAPACITY);
        scanner.use(level, player, InteractionHand.MAIN_HAND);
        ticks(scanner, player, 1, 100);
        helper.assertTrue(MeteorDataDiskItem.read(disk).orElseThrow().equals(meteor) && BatteryData.getEnergy(tool) == 0, "Creative scanning needs no FE");
        player.getAbilities().instabuild = false;
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void nearbyMarkerDoesNotRequireAiming(GameTestHelper helper) {
        var level = helper.getLevel();
        var data = new SummonedMeteorSavedData();
        var palette = List.of(Blocks.STONE.defaultBlockState(), Blocks.IRON_ORE.defaultBlockState());
        BlockPos base = helper.absolutePos(new BlockPos(4, 24, 4));
        var near = data.register(base, 7, 1234, palette);
        var far = data.register(base.east(20), 7, 5678, palette);
        var cell = HeavenshatterMeteor.cells(7, 1234, 2).getFirst();
        BlockPos nearBlock = base.offset(cell.x(), cell.y(), cell.z());
        level.setBlock(nearBlock, palette.get(cell.material()), 3);
        var farCell = HeavenshatterMeteor.cells(7, 5678, 2).getFirst();
        BlockPos farBlock = far.base().offset(farCell.x(), farCell.y(), farCell.z());
        level.setBlock(farBlock, palette.get(farCell.material()), 3);
        var observer = net.minecraft.world.phys.Vec3.atCenterOf(base.west(5));
        helper.assertTrue(data.nearest(level, observer, MeteorScannerItem.RANGE).equals(near), "Nearest meteor is marked without a raycast");
        helper.assertTrue(data.nearest(level, observer, 4) == null, "Out-of-range meteors are not marked");
        level.setBlock(nearBlock, Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(data.nearest(level, observer, MeteorScannerItem.RANGE).equals(far), "Fully mined meteor is skipped");
        level.setBlock(farBlock, Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(data.nearest(level, observer, MeteorScannerItem.RANGE) == null, "No stale marker remains after mining");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void meteorRecordValidation(GameTestHelper helper) {
        var meteor = RecordedMeteor.create(BlockPos.ZERO, 7, 2345,
            List.of(Blocks.STONE.defaultBlockState(), Blocks.DIAMOND_ORE.defaultBlockState()));
        helper.assertTrue(RecordedMeteor.load(meteor.save()).orElseThrow().equals(meteor), "Record round-trips");
        helper.assertTrue(MeteorDataDiskItem.read(new ItemStack(CrystalnexusModItems.METEOR_DATA_DISK.get())).isEmpty(), "Blank disk invalid");
        CompoundTag invalid = meteor.save(); invalid.putInt("version", 99);
        helper.assertTrue(RecordedMeteor.load(invalid).isEmpty(), "Unknown version rejected");
        invalid = meteor.save(); invalid.putInt("diameter", 21);
        helper.assertTrue(RecordedMeteor.load(invalid).isEmpty(), "Invalid diameter rejected");
        invalid = meteor.save(); invalid.putIntArray("weights", new int[] {0, 10});
        helper.assertTrue(RecordedMeteor.load(invalid).isEmpty(), "Zero weight rejected");
        invalid = meteor.save(); invalid.getList("palette", Tag.TAG_COMPOUND).getCompound(0).putString("Name", "missing:ore");
        helper.assertTrue(RecordedMeteor.load(invalid).isEmpty(), "Missing block rejected instead of defaulting to air");
        invalid = meteor.save(); invalid.remove("id");
        helper.assertTrue(RecordedMeteor.load(invalid).isEmpty(), "Missing identity rejected");
        ItemStack disk = disk(meteor);
        var restored = ItemStack.parseOptional(helper.getLevel().registryAccess(), (CompoundTag) disk.save(helper.getLevel().registryAccess()));
        helper.assertTrue(MeteorDataDiskItem.read(restored).orElseThrow().equals(meteor), "Disk item serialization keeps its record");
        helper.succeed();
    }

    @GameTest(template = "solar_sim")
    public static void simulatorRejectsDisksAndPreservesStoredOnes(GameTestHelper helper) {
        var level = helper.getLevel();
        var size = level.getStructureManager().get(ResourceLocation.parse("crystalnexus:solar_sim")).orElseThrow().getSize();
        BlockPos controllerPos = null;
        List<BlockPos> ports = new ArrayList<>();
        for (int x = 0; x < size.getX(); x++) for (int y = 1; y <= size.getY(); y++) for (int z = 0; z < size.getZ(); z++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (helper.getBlockState(pos).is(CrystalnexusModBlocks.SOLAR_SIMULATOR_CONTROLLER.get())) controllerPos = pos;
            if (helper.getBlockState(pos).is(CrystalnexusModBlocks.TUNGSTEN_BLOCK.get())) ports.add(pos);
        }
        helper.assertTrue(controllerPos != null && ports.size() >= 2, "Simulator template has controller and casing");
        var state = helper.getBlockState(controllerPos);
        helper.setBlock(controllerPos, Blocks.AIR);
        helper.setBlock(controllerPos, state);
        SolarSimulatorControllerBlockEntity simulator = helper.getBlockEntity(controllerPos);
        helper.setBlock(ports.get(0), CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get());
        helper.setBlock(ports.get(1), CrystalnexusModBlocks.MULTIBLOCK_ITEM_OUTPUT.get());
        helper.assertTrue(simulator.validateStructureNow(), "Simulator forms");
        MultiblockItemOutputBlockEntity output = helper.getBlockEntity(ports.get(1));
        var meteor = RecordedMeteor.create(BlockPos.ZERO, 7, 3456,
            List.of(Blocks.STONE.defaultBlockState(), Blocks.DIAMOND_ORE.defaultBlockState()));
        ItemStack disk = disk(meteor);
        for (int slot = 0; slot < 5; slot++)
            helper.assertTrue(!simulator.canPlaceItem(slot, disk), "Simulator slots reject meteor disks");
        var handler = new SolarSimulatorItemHandler(simulator, Direction.UP);
        helper.assertTrue(ItemStack.matches(handler.insertItem(0, disk.copy(), false), disk), "Automation rejects disks");
        simulator.setItem(0, disk.copy());
        simulator.setItem(4, new ItemStack(CrystalnexusModItems.ORANGE_STAR.get()));
        var saved = simulator.saveWithoutMetadata(level.registryAccess());
        simulator.loadAdditional(saved, level.registryAccess());
        helper.assertTrue(simulator.validateStructureNow(), "Simulator reforms after reload");
        simulator.multiblockEnergyInput().receiveEnergy(Integer.MAX_VALUE, false);
        int energy = simulator.multiblockEnergyInput().getEnergyStored();
        for (int tick = 0; tick < 5; tick++) simulator.serverTick();
        helper.assertTrue(simulator.getProgress() == 0 && simulator.multiblockEnergyInput().getEnergyStored() == energy,
            "A legacy stored disk produces no progress or power use");
        helper.assertTrue(output.isEmpty(), "Stored disk generates no resources");
        helper.assertTrue(ItemStack.matches(simulator.removeItem(0, 1), disk), "Legacy stored disk survives reload and can be removed");
        helper.assertTrue(simulator.canPlaceItem(0, new ItemStack(CrystalnexusModItems.METEOR.get())), "Legacy Meteor item remains accepted");
        helper.succeed();
    }
}
