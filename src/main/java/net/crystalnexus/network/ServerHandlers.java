package net.crystalnexus.network;

import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.network.payload.C2S_RequestPage;
import net.crystalnexus.network.payload.C2S_Withdraw;
import net.crystalnexus.network.payload.S2C_SendPage;
import net.crystalnexus.world.inventory.DepotMenu;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public class ServerHandlers {

    // Signature MUST be (payload, context) for playToServer(...)
    public static void onRequestPage(C2S_RequestPage msg, IPayloadContext ctx) {
        // Ensure this runs on the server thread
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;

            DepotSavedData depot = DepotSavedData.get(sp.serverLevel());

            List<DepotSavedData.Entry> page = depot.page(msg.search(), msg.page(), DepotMenu.PAGE_SIZE);

            List<S2C_SendPage.Entry> payload = page.stream()
                    .map(e -> new S2C_SendPage.Entry(e.itemId(), e.count()))
                    .toList();

            PacketDistributor.sendToPlayer(
                    sp,
                    new S2C_SendPage(
                            payload,
                            depot.getUpgradeLevel(),
                            depot.getUsed(),
                            depot.getCapacity()
                    )
            );
        });
    }

    // Signature MUST be (payload, context) for playToServer(...)
    public static void onWithdraw(C2S_Withdraw msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;

            DepotSavedData depot = DepotSavedData.get(sp.serverLevel());

            ResourceLocation itemId = msg.itemId();
            int requested = msg.amount();
            if (requested <= 0) return;

            long taken = depot.remove(itemId, requested);
            if (taken <= 0) return;

            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId);
            if (item == null || item == net.minecraft.world.item.Items.AIR) return;

            int max = Math.max(1, item.getDefaultInstance().getMaxStackSize());
            long left = taken;

            while (left > 0) {
                int give = (int) Math.min(left, max);
                left -= give;

                var stack = new net.minecraft.world.item.ItemStack(item, give);

                if (!sp.getInventory().add(stack)) {
                    sp.drop(stack, false);
                }
            }
        });
    }
}
