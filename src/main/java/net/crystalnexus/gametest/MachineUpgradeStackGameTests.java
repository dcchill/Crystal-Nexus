package net.crystalnexus.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.crystalnexus.block.entity.CrystalCrusherBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.util.MachineUpgradeHelper;
import net.crystalnexus.world.inventory.CrusherGuiMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import java.util.UUID;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class MachineUpgradeStackGameTests {
    private MachineUpgradeStackGameTests() {}

    @GameTest(template = "zero_point")
    public static void stackSizeAndMachineSlotInsertion(GameTestHelper helper) {
        for (Item item : new Item[] {
                CrystalnexusModItems.ACCELERATION_UPGRADE.get(), CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get(),
                CrystalnexusModItems.FE_EFFICIENCY_UPGRADE.get(), CrystalnexusModItems.CARBON_FE_EFFICIENCY_UPGRADE.get(),
                CrystalnexusModItems.RANGE_UPGRADE.get(), CrystalnexusModItems.CARBON_RANGE_UPGRADE.get() })
            helper.assertTrue(new ItemStack(item).getMaxStackSize() == 16, "Machine upgrade must stack to 16: " + item);

        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.CRYSTAL_CRUSHER.get());
        CrystalCrusherBlockEntity crusher = helper.getBlockEntity(pos);
        InvWrapper automation = new InvWrapper(crusher);
        helper.assertTrue(automation.insertItem(2, new ItemStack(CrystalnexusModItems.ACCELERATION_UPGRADE.get(), 16), false).isEmpty()
                && crusher.getItem(2).getCount() == 16, "Automation must insert all 16 upgrades");
        crusher.setItem(2, ItemStack.EMPTY);

        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "upgrade-stack-test"), ClientInformation.createDefault());
        player.getInventory().setItem(9, new ItemStack(CrystalnexusModItems.ACCELERATION_UPGRADE.get(), 16));
        CrusherGuiMenu menu = new CrusherGuiMenu(1, player.getInventory(),
                new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(helper.absolutePos(pos)));
        menu.quickMoveStack(player, 3);
        helper.assertTrue(crusher.getItem(2).getCount() == 16 && player.getInventory().getItem(9).isEmpty(),
                "Shift-click must put the full stack in the upgrade slot");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void upgradeEffectsAndSsdRemainDistinct(GameTestHelper helper) {
        ItemStack empty = ItemStack.EMPTY;
        ItemStack one = new ItemStack(CrystalnexusModItems.ACCELERATION_UPGRADE.get());
        ItemStack two = new ItemStack(CrystalnexusModItems.ACCELERATION_UPGRADE.get(), 2);
        ItemStack speed = new ItemStack(CrystalnexusModItems.ACCELERATION_UPGRADE.get(), 16);
        ItemStack efficiency = new ItemStack(CrystalnexusModItems.FE_EFFICIENCY_UPGRADE.get(), 16);
        ItemStack range = new ItemStack(CrystalnexusModItems.RANGE_UPGRADE.get(), 16);
        helper.assertTrue(MachineUpgradeHelper.processingTime(empty, 100, 75, 50) == 100
                && MachineUpgradeHelper.processingTime(one, 100, 75, 50) == 75
                && MachineUpgradeHelper.processingTime(two, 100, 75, 50) < 75
                && MachineUpgradeHelper.processingTime(speed, 100, 75, 50) < MachineUpgradeHelper.processingTime(two, 100, 75, 50),
                "Processing speed must improve at counts 0, 1, 2, and 16");
        helper.assertTrue(MachineUpgradeHelper.energyCost(empty, 4096) == 4096
                && MachineUpgradeHelper.energyCost(new ItemStack(CrystalnexusModItems.FE_EFFICIENCY_UPGRADE.get()), 4096) == 2731
                && MachineUpgradeHelper.energyCost(new ItemStack(CrystalnexusModItems.FE_EFFICIENCY_UPGRADE.get(), 2), 4096) == 2048
                && MachineUpgradeHelper.energyCost(efficiency, 4096) < 2048,
                "FE efficiency must improve at counts 0, 1, 2, and 16");
        helper.assertTrue(MachineUpgradeHelper.generatorEfficiency(empty, 1.25, 1.5) == 1
                && MachineUpgradeHelper.generatorEfficiency(new ItemStack(CrystalnexusModItems.FE_EFFICIENCY_UPGRADE.get()), 1.25, 1.5) == 1.25
                && MachineUpgradeHelper.generatorEfficiency(new ItemStack(CrystalnexusModItems.FE_EFFICIENCY_UPGRADE.get(), 2), 1.25, 1.5) == 1.5
                && MachineUpgradeHelper.generatorEfficiency(efficiency, 1.25, 1.5) > 1.5,
                "Generator FE output must improve at counts 0, 1, 2, and 16");
        helper.assertTrue(MachineUpgradeHelper.scaledEffect(empty, 12, 25) == 12
                && MachineUpgradeHelper.scaledEffect(new ItemStack(CrystalnexusModItems.RANGE_UPGRADE.get()), 12, 25) == 25
                && MachineUpgradeHelper.scaledEffect(new ItemStack(CrystalnexusModItems.RANGE_UPGRADE.get(), 2), 12, 25) == 38
                && MachineUpgradeHelper.scaledEffect(range, 12, 25) > 38,
                "Range must improve at counts 0, 1, 2, and 16");
        helper.assertTrue(MachineUpgradeHelper.generatorCycleTime(empty, 250, 325, 500) == 250
                && MachineUpgradeHelper.generatorCycleTime(one, 250, 325, 500) == 325
                && MachineUpgradeHelper.generatorCycleTime(two, 250, 325, 500) == 400
                && MachineUpgradeHelper.generatorCycleTime(speed, 250, 325, 500) > 400,
                "Generator cycle effect must improve at counts 0, 1, 2, and 16");

        ItemStack ssd = new ItemStack(CrystalnexusModItems.SSD.get());
        CustomData.update(DataComponents.CUSTOM_DATA, ssd, tag -> {
            tag.putDouble("cook_mult", 0.5);
            tag.putDouble("fe_efficiency", 2.0);
        });
        helper.assertTrue(ssd.getMaxStackSize() == 64 && MachineUpgradeHelper.cookTime(ssd, 100) == 50
                && MachineUpgradeHelper.energyCost(ssd, 4096) == 2048,
                "SSD stack size and custom effects must stay unchanged");
        helper.succeed();
    }
}
