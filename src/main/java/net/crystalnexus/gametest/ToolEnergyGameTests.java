package net.crystalnexus.gametest;

import com.mojang.authlib.GameProfile;
import net.crystalnexus.events.InventoryBatteryCharging;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.item.BatteryData;
import net.crystalnexus.item.ToolEnergy;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class ToolEnergyGameTests {
    @GameTest(template = "zero_point")
    public static void toolsStoreEnergyAndBatteriesChargeInventory(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
            new GameProfile(java.util.UUID.randomUUID(), "tool-energy-test"), ClientInformation.createDefault());
        Item[] tools = {CrystalnexusModItems.COMPOUND_PICKAXE.get(), CrystalnexusModItems.COMPOUND_SWORD.get(),
            CrystalnexusModItems.MINING_LASER.get(), CrystalnexusModItems.ORE_SCANNER.get(),
            CrystalnexusModItems.GEIGER_COUNTER.get(), CrystalnexusModItems.GRAVITY_GUN.get(),
            CrystalnexusModItems.BUILD_GUN.get(), CrystalnexusModItems.HOVER_PACK_CHESTPLATE.get(),
            CrystalnexusModItems.LASER_SABER.get(), CrystalnexusModItems.FLORATHANE_WAND.get()};
        for (Item item : tools) {
            ItemStack tool = new ItemStack(item);
            var energy = tool.getCapability(Capabilities.EnergyStorage.ITEM);
            helper.assertTrue(energy != null, "Every FE tool must expose energy storage");
            helper.assertTrue(!tool.isDamageableItem(), "FE tools must not break from durability");
            helper.assertTrue(item.isBarVisible(tool) && item.getBarWidth(tool) == 0, "Empty tools show an empty FE bar");
            energy.receiveEnergy(500, false);
            helper.assertTrue(ToolEnergy.consume(player, tool, 200, true) && energy.getEnergyStored() == 500,
                "Simulated use must not spend FE");
            helper.assertTrue(ToolEnergy.consume(player, tool, 200, false) && energy.getEnergyStored() == 300,
                "Use consumes the tool's FE");
            helper.assertTrue(!ToolEnergy.consume(player, tool, 500, false) && energy.getEnergyStored() == 300,
                "Failed use must not partially drain FE");
            BatteryData.setEnergy(tool, ToolEnergy.CAPACITY, ToolEnergy.CAPACITY);
            helper.assertTrue(item.getBarWidth(tool) == 13, "Full FE bar must span 13 pixels");
        }
        ItemStack laser = new ItemStack(CrystalnexusModItems.MINING_LASER.get());
        ItemStack pack = new ItemStack(CrystalnexusModItems.HOVER_PACK_CHESTPLATE.get());
        ItemStack battery = new ItemStack(CrystalnexusModItems.DARK_BATTERY_CELL.get());
        var source = battery.getCapability(Capabilities.EnergyStorage.ITEM);
        BatteryData.setEnergy(battery, 300_000, source.getMaxEnergyStored());
        player.getInventory().setItem(0, laser);
        player.setItemSlot(EquipmentSlot.CHEST, pack);
        player.setItemSlot(EquipmentSlot.OFFHAND, battery);
        int initial = source.getEnergyStored();
        helper.assertTrue(!ToolEnergy.consume(player, laser, 200, false) && source.getEnergyStored() == initial,
            "Empty tools cannot directly drain a charged inventory battery");
        for (int tick = 0; tick < 1000; tick++) InventoryBatteryCharging.chargeInventory(player);
        helper.assertTrue(BatteryData.getEnergy(laser) == ToolEnergy.CAPACITY && BatteryData.getEnergy(pack) == ToolEnergy.CAPACITY,
            "Offhand batteries must charge main inventory and equipped armor");
        helper.assertTrue(source.getEnergyStored() == initial - 2 * ToolEnergy.CAPACITY,
            "Charging must conserve FE and stop at capacity");
        helper.succeed();
    }
}
