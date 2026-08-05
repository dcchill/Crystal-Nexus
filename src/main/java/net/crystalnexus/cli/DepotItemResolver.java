package net.crystalnexus.cli;

import net.crystalnexus.data.DepotSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DepotItemResolver {
    public record Candidate(ResourceLocation id, Item item, long count) {
        public String name() { return new ItemStack(item).getHoverName().getString(); }
    }

    public record Result(Candidate match, List<Candidate> ambiguous) {
        public boolean found() { return match != null; }
    }

    private DepotItemResolver() {
    }

    public static Result stored(DepotSavedData depot, String query) {
        return resolve(query, depot.entries().stream().map(entry -> candidate(entry.itemId(), entry.count())).filter(java.util.Objects::nonNull).toList());
    }

    public static Result inventory(ServerPlayer player, String query) {
        Map<ResourceLocation, Long> counts = new LinkedHashMap<>();
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) counts.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()), (long) stack.getCount(), Long::sum);
        }
        return resolve(query, counts.entrySet().stream().map(entry -> candidate(entry.getKey(), entry.getValue()))
                .filter(java.util.Objects::nonNull).toList());
    }

    public static Result registry(String query) {
        List<Candidate> candidates = new ArrayList<>();
        BuiltInRegistries.ITEM.entrySet().forEach(entry -> {
            if (entry.getValue() != Items.AIR) candidates.add(new Candidate(entry.getKey().location(), entry.getValue(), 0));
        });
        return resolve(query, candidates);
    }

    private static Candidate candidate(ResourceLocation id, long count) {
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == null || item == Items.AIR ? null : new Candidate(id, item, count);
    }

    private static Result resolve(String raw, List<Candidate> candidates) {
        String query = raw == null ? "" : raw.trim();
        ResourceLocation exactId = query.contains(":") ? ResourceLocation.tryParse(query) : null;
        if (exactId != null) {
            Candidate exact = candidates.stream().filter(candidate -> candidate.id().equals(exactId)).findFirst().orElse(null);
            return new Result(exact, List.of());
        }

        String normalized = DepotCliParser.normalize(query);
        List<Candidate> exactNames = candidates.stream().filter(candidate ->
                candidate.name().equalsIgnoreCase(query)
                        || DepotCliParser.normalize(candidate.name()).equals(normalized)
                        || DepotCliParser.normalize(candidate.id().getPath()).equals(normalized)).toList();
        if (exactNames.size() == 1) return new Result(exactNames.getFirst(), List.of());
        if (exactNames.size() > 1) return new Result(null, sorted(exactNames));

        List<Candidate> partial = candidates.stream().filter(candidate ->
                DepotCliParser.normalize(candidate.name()).contains(normalized)
                        || candidate.id().toString().toLowerCase(java.util.Locale.ROOT).contains(normalized)).toList();
        return partial.size() == 1 ? new Result(partial.getFirst(), List.of()) : new Result(null, sorted(partial));
    }

    private static List<Candidate> sorted(List<Candidate> candidates) {
        return candidates.stream().sorted(Comparator.comparing(candidate -> candidate.id().toString())).limit(12).toList();
    }

    public static DepotCliCommandResult unresolved(String query, Result result) {
        if (result.ambiguous().isEmpty()) return DepotCliCommandResult.error("Unknown item: " + query);
        List<String> lines = new ArrayList<>();
        lines.add("[ERROR] \"" + query + "\" matched multiple items:");
        for (int i = 0; i < result.ambiguous().size(); i++) {
            Candidate candidate = result.ambiguous().get(i);
            lines.add((i + 1) + ". " + candidate.name() + " (" + candidate.id() + ")");
        }
        lines.add("Use the registry identifier to select one.");
        return new DepotCliCommandResult(lines);
    }
}
