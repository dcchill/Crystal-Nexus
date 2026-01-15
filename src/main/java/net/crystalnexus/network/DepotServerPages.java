package net.crystalnexus.network;

import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.network.payload.C2S_RequestPage;
import net.crystalnexus.network.payload.S2C_SendPage;
import net.crystalnexus.world.inventory.DepotMenu;

import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class DepotServerPages {

    public static void handleRequestPage(ServerPlayer player, C2S_RequestPage msg) {
        DepotSavedData data = DepotSavedData.get(player.serverLevel());

        List<DepotSavedData.Entry> page = data.page(msg.search(), msg.page(), DepotMenu.PAGE_SIZE);

        List<S2C_SendPage.Entry> entries = page.stream()
                .map(e -> new S2C_SendPage.Entry(e.itemId(), e.count()))
                .toList();

        int upgradeLevel = data.getUpgradeLevel();
        long used = data.getUsed();
        long capacity = data.getCapacity();

        PacketDistributor.sendToPlayer(player, new S2C_SendPage(entries, upgradeLevel, used, capacity));
    }
}
