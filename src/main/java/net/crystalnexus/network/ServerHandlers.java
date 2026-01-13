package net.crystalnexus.network;

import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.network.payload.C2S_RequestPage;
import net.crystalnexus.network.payload.C2S_Withdraw;
import net.crystalnexus.network.payload.S2C_SendPage;
import net.crystalnexus.world.inventory.DepotMenu;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerHandlers {

    // Remember what each player is currently viewing so we can "push" updates after withdraw.
    private static final ConcurrentHashMap<UUID, LastView> LAST_VIEW = new ConcurrentHashMap<>();

    private record LastView(String search, int page) {}

    private static void sendPage(ServerPlayer sp, String search, int page) {
        DepotSavedData data = DepotSavedData.get(sp.serverLevel());

        List<DepotSavedData.Entry> view = data.page(search, page, DepotMenu.PAGE_SIZE);

        List<S2C_SendPage.Entry> payload = view.stream()
                .map(e -> new S2C_SendPage.Entry(e.itemId(), e.count()))
                .toList();

        PacketDistributor.sendToPlayer(sp, new S2C_SendPage(payload));
    }

    public static void onRequestPage(final C2S_RequestPage msg, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;

            // Only allow if they have the depot menu open (prevents random spam)
            if (!(sp.containerMenu instanceof DepotMenu)) return;

            String search = (msg.search() == null) ? "" : msg.search();
            int page = Math.max(0, msg.page());

            // Remember the last view for instant updates
            LAST_VIEW.put(sp.getUUID(), new LastView(search, page));

            sendPage(sp, search, page);
        });
    }

    public static void onWithdraw(final C2S_Withdraw msg, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            if (!(sp.containerMenu instanceof DepotMenu)) return;

            ResourceLocation itemId = msg.itemId();
            int requested = msg.amount();

            // Treat very large requests (Ctrl) as "give as much as possible"
            if (requested <= 0) requested = 1;

            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == null || item == Items.AIR) return;

            ServerLevel level = sp.serverLevel();
            DepotSavedData data = DepotSavedData.get(level);

            long available = data.getCount(itemId);
            if (available <= 0) {
                // still push an update so the client doesn't look stale
                LastView view = LAST_VIEW.get(sp.getUUID());
                if (view != null) sendPage(sp, view.search(), view.page());
                return;
            }

            // Clamp to available and to int range
            long wantLong = (requested == Integer.MAX_VALUE) ? available : Math.min(available, (long) requested);
            int toGive = (int) Math.min(Integer.MAX_VALUE, wantLong);
            if (toGive <= 0) return;

            // Give items to player (MCreator/your build returns void here)
            ItemHandlerHelper.giveItemToPlayer(sp, new ItemStack(item, toGive));

            // Remove from depot (items may drop if inv is full; good enough for now)
            data.remove(itemId, toGive);

            // ✅ Push updated page instantly
            LastView view = LAST_VIEW.get(sp.getUUID());
            if (view != null) {
                sendPage(sp, view.search(), view.page());
            }
        });
    }
}
