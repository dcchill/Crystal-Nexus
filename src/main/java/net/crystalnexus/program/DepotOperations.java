package net.crystalnexus.program;

import net.crystalnexus.cli.DepotCraftingService;
import net.crystalnexus.data.DepotSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Typed depot mutations used by the program runner; all quantities and registry IDs are revalidated here. */
public final class DepotOperations {
    public enum Kind { OK, WARNING, ERROR, JOB }
    public record Result(Kind kind, String message, int jobId) {
        public boolean success() { return kind != Kind.ERROR; }
        public static Result ok(String message) { return new Result(Kind.OK, message, 0); }
        public static Result warning(String message) { return new Result(Kind.WARNING, message, 0); }
        public static Result error(String message) { return new Result(Kind.ERROR, message, 0); }
        public static Result job(String message, int id) { return new Result(Kind.JOB, message, id); }
    }
    private DepotOperations() {}

    public static Item item(ResourceLocation id) {
        Item item = id == null ? null : BuiltInRegistries.ITEM.get(id);
        return item == null || item == Items.AIR ? null : item;
    }

    public static Result take(ServerPlayer player, DepotSavedData depot, ResourceLocation id, int amount) {
        Item item = item(id);
        if (player == null) return Result.error("Player inventory is unavailable while the owner is offline.");
        if (item == null || amount < 1 || amount > 4096) return Result.error("Invalid item or quantity.");
        long removed = depot.remove(id, Math.min(amount, depot.getCount(id)));
        ItemStack stack = new ItemStack(item, (int) removed);
        player.getInventory().add(stack);
        int overflow = stack.getCount();
        if (overflow > 0) depot.deposit(id, overflow);
        long moved = removed - overflow;
        return moved == amount ? Result.ok("Retrieved " + moved + " item(s).")
                : Result.warning("Retrieved " + moved + " of " + amount + " item(s).");
    }

    public static Result depositItem(ServerPlayer player, DepotSavedData depot, ResourceLocation id, int amount) {
        Item item = item(id);
        if (player == null) return Result.error("Player inventory is unavailable while the owner is offline.");
        if (item == null || amount < 1 || amount > 4096) return Result.error("Invalid item or quantity.");
        long remaining = amount, accepted = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (remaining <= 0 || !stack.is(item)) continue;
            int offered = (int) Math.min(remaining, stack.getCount());
            long inserted = depot.deposit(id, offered);
            stack.shrink((int) inserted);
            accepted += inserted;
            remaining -= inserted;
            if (inserted < offered) break;
        }
        return accepted == amount ? Result.ok("Deposited " + accepted + " item(s).")
                : Result.warning("Deposited " + accepted + " of " + amount + " item(s).");
    }

    public static Result depositHeld(ServerPlayer player, DepotSavedData depot) {
        if (player == null) return Result.error("Player inventory is unavailable while the owner is offline.");
        long accepted = depot.tryDepositAll(player.getMainHandItem());
        return accepted > 0 ? Result.ok("Deposited " + accepted + " item(s).") : Result.warning("Nothing was deposited.");
    }

    public static Result depositInventory(ServerPlayer player, DepotSavedData depot) {
        if (player == null) return Result.error("Player inventory is unavailable while the owner is offline.");
        long accepted = 0;
        for (ItemStack stack : player.getInventory().items) accepted += depot.tryDepositAll(stack);
        return Result.ok("Deposited " + accepted + " item(s).");
    }

    public static Result craft(ServerPlayer player, DepotSavedData depot, ResourceLocation id, int amount, String mode) {
        if (player == null) return Result.error("Recipe data is unavailable until the owner reconnects.");
        Item item = item(id);
        if (item == null || amount < 1 || amount > 4096) return Result.error("Invalid item or quantity.");
        DepotCraftingService.Result result = switch (mode) {
            case "smelt" -> DepotCraftingService.smelt(player, depot, item, amount);
            case "process" -> DepotCraftingService.process(player, depot, item, amount);
            default -> DepotCraftingService.craftVisual(player, depot, item, amount);
        };
        if (!result.success()) return Result.error(String.join(" ", result.details()));
        return Result.job("Started job #" + result.job().id() + ".", result.job().id());
    }

    public static Result cancel(DepotSavedData depot) {
        DepotSavedData.CraftingJob job = depot.getCraftingJob();
        return job != null && depot.cancelCraftingJob(job.id()) != null
                ? Result.ok("Cancelled job #" + job.id() + ".") : Result.warning("No crafting job is active.");
    }
}
