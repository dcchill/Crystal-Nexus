package net.crystalnexus.network;

import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.network.payload.C2S_RequestPage;
import net.crystalnexus.network.payload.C2S_Withdraw;
import net.crystalnexus.network.payload.S2C_SendPage;
import net.crystalnexus.world.inventory.DepotMenu;
import net.crystalnexus.world.inventory.DepotCliMenu;
import net.crystalnexus.cli.DepotCliCommandContext;
import net.crystalnexus.cli.DepotCliCommandRegistry;
import net.crystalnexus.network.payload.C2S_DepotCliRequest;
import net.crystalnexus.network.payload.C2S_DepotJeiRecipes;
import net.crystalnexus.network.payload.S2C_DepotCliResponse;
import net.crystalnexus.cli.DepotJeiRecipeCache;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public class ServerHandlers {

    public static void onDepotJeiRecipes(C2S_DepotJeiRecipes msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                DepotJeiRecipeCache.accept(player, msg.generation(), msg.reset(), msg.recipes());
            }
        });
    }

    public static void onDepotCliRequest(C2S_DepotCliRequest msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof DepotCliMenu menu)
                    || menu.containerId != msg.menuId()) return;
            if (!menu.stillValid(player)) {
                player.closeContainer();
                return;
            }
            if (!menu.allowCommand(player)) {
                sendCli(player, menu, List.of("[ERROR] Rate limit exceeded."), List.of());
                return;
            }
            if (msg.input() == null || msg.input().length() > DepotCliCommandRegistry.MAX_COMMAND_LENGTH) {
                sendCli(player, menu, List.of("[ERROR] Command is too long."), List.of());
                return;
            }
            DepotCliCommandContext commandContext = new DepotCliCommandContext(player, menu, DepotSavedData.get(player));
            try {
                if (msg.suggestions()) {
                    sendCli(player, menu, List.of(), DepotCliCommandRegistry.INSTANCE.suggest(commandContext, msg.input()));
                } else {
                    sendCli(player, menu, DepotCliCommandRegistry.INSTANCE.execute(commandContext, msg.input()).lines(), List.of());
                }
            } catch (RuntimeException exception) {
                sendCli(player, menu, List.of("[ERROR] Command failed safely: " + exception.getClass().getSimpleName()), List.of());
            }
        });
    }

    private static void sendCli(ServerPlayer player, DepotCliMenu menu, List<String> lines, List<String> suggestions) {
        PacketDistributor.sendToPlayer(player,
                new S2C_DepotCliResponse(menu.containerId, menu.isConnected(player), lines, suggestions));
    }

    // Signature MUST be (payload, context) for playToServer(...)
    public static void onRequestPage(C2S_RequestPage msg, IPayloadContext ctx) {
        // Ensure this runs on the server thread
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            if (!(sp.containerMenu instanceof DepotMenu menu)) return;
            if (!menu.canAccessDepot(sp)) {
                sp.closeContainer();
                return;
            }

            DepotSavedData depot = DepotSavedData.get(sp);

            List<DepotSavedData.Entry> page = depot.page(msg.search(), msg.page(), DepotMenu.PAGE_SIZE);
            menu.setDepotPage(msg.search(), msg.page(), page);

            List<S2C_SendPage.Entry> payload = page.stream()
                    .map(e -> new S2C_SendPage.Entry(e.itemId(), e.count()))
                    .toList();

            PacketDistributor.sendToPlayer(
                    sp,
                    new S2C_SendPage(
                            payload,
                            depot.countEntries(msg.search()),
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
            if (!(sp.containerMenu instanceof DepotMenu menu)) return;
            if (!menu.canAccessDepot(sp)) {
                sp.closeContainer();
                return;
            }

            DepotSavedData depot = DepotSavedData.get(sp);

            ResourceLocation itemId = msg.itemId();
            int requested = msg.amount();
            if (requested <= 0) return;

            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId);
            if (item == null || item == net.minecraft.world.item.Items.AIR) return;

            long taken = depot.remove(itemId, requested);
            if (taken <= 0) return;

            int max = Math.max(1, item.getDefaultInstance().getMaxStackSize());
            long left = taken;

            while (left > 0) {
                int give = (int) Math.min(left, max);
                left -= give;

                var stack = new net.minecraft.world.item.ItemStack(item, give);

                if (!sp.getInventory().add(stack)) {
                    depot.add(itemId, left + stack.getCount());
                    return;
                }
            }
        });
    }
}
