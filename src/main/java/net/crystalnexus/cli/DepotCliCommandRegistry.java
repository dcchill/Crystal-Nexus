package net.crystalnexus.cli;

import net.crystalnexus.block.entity.DepotControllerBlockEntity;
import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.util.DepotNetwork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;

public final class DepotCliCommandRegistry {
    public static final int MAX_COMMAND_LENGTH = 256;
    public static final int MAX_QUANTITY = 4096;
    public static final int PAGE_SIZE = 20;
    public static final DepotCliCommandRegistry INSTANCE = new DepotCliCommandRegistry();

    private final Map<String, DepotCliCommand> commands = new LinkedHashMap<>();
    private final List<DepotCliCommand> canonical = new ArrayList<>();

    private DepotCliCommandRegistry() {
        register(command("help", List.of("?"), "help [command]", "Show command help", DepotCliCommand.Permission.VIEW, false, this::help));
        register(command("status", List.of(), "status", "Show depot network status", DepotCliCommand.Permission.STATUS, false, this::status));
        register(command("find", List.of("search"), "find <text|filters>", "Search stored items", DepotCliCommand.Permission.VIEW, true, this::find));
        register(command("list", List.of("ls"), "list [--sort name|amount|amount-desc] [--page N]", "List stored item types", DepotCliCommand.Permission.VIEW, true, this::list));
        register(command("take", List.of("retrieve", "withdraw"), "take <item> <amount>", "Move items to your inventory", DepotCliCommand.Permission.WITHDRAW, true, this::take));
        register(command("deposit", List.of("put"), "deposit <item> <amount>|held|inventory", "Move items into the depot", DepotCliCommand.Permission.DEPOSIT, true, this::deposit));
        register(command("craft", List.of(), "craft [--machine <id>] <item> <amount>", "Craft into depot storage", DepotCliCommand.Permission.CRAFT, true, this::craft));
        register(command("recipe", List.of("recipes"), "recipe list <item>|prefer <item> <number>|clear <item>", "Manage recursive crafting recipe preferences", DepotCliCommand.Permission.CRAFT, true, this::recipe));
        register(command("machine", List.of("machines"), "machine list <item>|prefer <item> <number>|clear <item>", "Choose the machine used for an item", DepotCliCommand.Permission.CRAFT, true, this::machine));
        register(command("process", List.of("processing"), "process list|add <output_id> <count> <input_id> <count>... [--byproduct <id> <count>...]|remove <output_id>", "Manage machine-processing patterns", DepotCliCommand.Permission.CRAFT, true, this::process));
        register(command("queue", List.of(), "queue [cancel <id>]", "Show crafting jobs", DepotCliCommand.Permission.VIEW, true, this::queue));
        register(command("jei", List.of("show"), "jei [--machine <id>] <item> [<amount>]", "Open JEI recipes for an item, or autofill craft command", DepotCliCommand.Permission.VIEW, false, this::jeiCmd));
        register(command("clear", List.of("cls"), "clear", "Clear local terminal output", DepotCliCommand.Permission.VIEW, false, (ctx, args) -> DepotCliCommandResult.ok("Output cleared.")));
        register(command("history", List.of(), "history", "Show local command history", DepotCliCommand.Permission.VIEW, false, (ctx, args) -> DepotCliCommandResult.info("Command history is stored locally.")));
    }

    private static DepotCliCommand command(String name, List<String> aliases, String usage, String description,
            DepotCliCommand.Permission permission, boolean requiresNetwork,
            BiFunction<DepotCliCommandContext, List<String>, DepotCliCommandResult> executor) {
        return new DepotCliCommand() {
            public String name() { return name; }
            public List<String> aliases() { return aliases; }
            public String usage() { return usage; }
            public String description() { return description; }
            public Permission permission() { return permission; }
            public boolean requiresNetwork() { return requiresNetwork; }
            public DepotCliCommandResult execute(DepotCliCommandContext context, List<String> arguments) { return executor.apply(context, arguments); }
        };
    }

    private void register(DepotCliCommand command) {
        canonical.add(command);
        commands.put(command.name(), command);
        command.aliases().forEach(alias -> commands.put(alias, command));
    }

    public DepotCliCommandResult execute(DepotCliCommandContext context, String input) {
        if (input == null || input.isBlank()) return startup(context);
        if (input.length() > MAX_COMMAND_LENGTH) return DepotCliCommandResult.error("Command is too long.");
        List<String> tokens = DepotCliParser.parse(input);
        if (tokens.isEmpty()) return DepotCliCommandResult.info();
        DepotCliCommand command = commands.get(tokens.getFirst().toLowerCase(Locale.ROOT));
        if (command == null) {
            String suggestion = commands.keySet().stream().min(Comparator.comparingInt(name -> distance(tokens.getFirst(), name))).orElse("help");
            return new DepotCliCommandResult(List.of("[ERROR] Unknown command: " + tokens.getFirst(), "Did you mean: " + suggestion));
        }
        if (!context.hasPermission(command.permission())) return DepotCliCommandResult.error("You do not have permission to use this command.");
        if (command.requiresNetwork() && !context.connected()) return DepotCliCommandResult.error("Depot network disconnected or offline.");
        return command.execute(context, tokens.subList(1, tokens.size()));
    }

    public List<String> suggest(DepotCliCommandContext context, String input) {
        String raw = input == null ? "" : input;
        List<String> tokens = DepotCliParser.parse(raw);
        boolean trailingSpace = !raw.isEmpty() && Character.isWhitespace(raw.charAt(raw.length() - 1));
        if (tokens.isEmpty() || tokens.size() == 1 && !trailingSpace) {
            String prefix = tokens.isEmpty() ? "" : tokens.getFirst().toLowerCase(Locale.ROOT);
            return commands.keySet().stream().filter(name -> name.startsWith(prefix)).distinct().limit(12).toList();
        }

        DepotCliCommand command = commands.get(tokens.getFirst().toLowerCase(Locale.ROOT));
        if (command == null || !context.connected()) return List.of();
        String current = trailingSpace ? "" : tokens.getLast().toLowerCase(Locale.ROOT);
        String base = trailingSpace ? raw : raw.substring(0, Math.max(0, raw.lastIndexOf(' ') + 1));
        if (command.name().equals("take") || command.name().equals("find")) {
            List<String> filters = command.name().equals("find")
                    ? List.of("mod:", "tag:", "amount>", "amount<") : List.of();
            List<String> suggestions = new ArrayList<>(filters.stream().filter(value -> value.startsWith(current)).map(base::concat).toList());
            context.depot().entries().stream().flatMap(entry -> java.util.stream.Stream.of(
                            entry.itemId().toString(),
                            "\"" + new ItemStack(BuiltInRegistries.ITEM.get(entry.itemId())).getHoverName().getString() + "\""))
                    .filter(value -> value.toLowerCase(Locale.ROOT).contains(current))
                    .distinct().limit(12 - suggestions.size()).map(base::concat).forEach(suggestions::add);
            return suggestions;
        }
        if (command.name().equals("deposit")) {
            List<String> suggestions = new ArrayList<>(List.of("held", "inventory").stream()
                    .filter(value -> value.startsWith(current)).map(base::concat).toList());
            context.player().getInventory().items.stream().filter(stack -> !stack.isEmpty())
                    .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).distinct()
                    .filter(id -> id.contains(current)).limit(12 - suggestions.size()).map(base::concat).forEach(suggestions::add);
            return suggestions;
        }
        if (command.name().equals("craft")) {
            return DepotCraftingService.availableRecipes(context.player()).stream()
                    .map(DepotCraftingService.AvailableRecipe::output)
                    .filter(stack -> !stack.isEmpty()).map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).distinct()
                    .filter(id -> id.contains(current)).limit(12).map(base::concat).toList();
        }
        if (command.name().equals("jei") || command.name().equals("show")) {
            if (tokens.size() == 1 || tokens.size() == 2 && !trailingSpace) {
                return List.of("--machine ").stream().filter(value -> value.startsWith(current)).map(base::concat).toList();
            }
            // Suggest item names after jei, or after jei --machine <id>
            int itemTokenIndex = tokens.getLast().equals("--machine") || tokens.get(tokens.size() - 2).equals("--machine") ? -1 : tokens.size() - 1;
            if (itemTokenIndex < 0) return List.of();
            return DepotCraftingService.availableRecipes(context.player()).stream()
                    .map(DepotCraftingService.AvailableRecipe::output)
                    .filter(stack -> !stack.isEmpty()).map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).distinct()
                    .filter(id -> id.contains(current)).limit(12).map(base::concat).toList();
        }
        if (command.name().equals("recipe")) {
            if (tokens.size() == 1 || tokens.size() == 2 && !trailingSpace) {
                return List.of("list", "prefer", "clear").stream().filter(value -> value.startsWith(current)).map(base::concat).toList();
            }
            String action = tokens.get(1).toLowerCase(Locale.ROOT);
            if (tokens.size() == 2 && trailingSpace || tokens.size() == 3 && !trailingSpace) {
                return DepotCraftingService.availableRecipes(context.player()).stream()
                        .map(DepotCraftingService.AvailableRecipe::output)
                        .filter(stack -> !stack.isEmpty()).map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).distinct()
                        .filter(id -> id.contains(current)).limit(12).map(base::concat).toList();
            }
            if (action.equals("prefer") && tokens.size() == 3 && trailingSpace) {
                ResourceLocation itemId = ResourceLocation.tryParse(tokens.get(2));
                if (itemId == null) return List.of();
                return java.util.stream.IntStream.rangeClosed(1,
                                DepotCraftingService.recipeChoices(context.player(), BuiltInRegistries.ITEM.get(itemId)).size())
                        .limit(12).mapToObj(Integer::toString).map(base::concat).toList();
            }
        }
        if (command.name().equals("machine")) {
            if (tokens.size() == 1 || tokens.size() == 2 && !trailingSpace) {
                return List.of("list", "prefer", "clear").stream().filter(value -> value.startsWith(current)).map(base::concat).toList();
            }
            String action = tokens.get(1).toLowerCase(Locale.ROOT);
            if (tokens.size() == 2 && trailingSpace || tokens.size() == 3 && !trailingSpace) {
                return DepotJeiRecipeCache.recipes(context.player()).stream().flatMap(recipe -> recipe.outputs().stream())
                        .map(DepotJeiRecipeCache.StackRef::itemId).distinct().map(ResourceLocation::toString)
                        .filter(id -> id.contains(current)).limit(12).map(base::concat).toList();
            }
            if (action.equals("prefer") && tokens.size() == 3 && trailingSpace) {
                ResourceLocation itemId = ResourceLocation.tryParse(tokens.get(2));
                if (itemId == null) return List.of();
                long count = DepotCraftingService.recipeChoices(context.player(), BuiltInRegistries.ITEM.get(itemId)).stream()
                        .flatMap(choice -> choice.machineTypes().stream()).distinct().count();
                return java.util.stream.IntStream.rangeClosed(1, (int) Math.min(12, count))
                        .mapToObj(Integer::toString).map(base::concat).toList();
            }
        }
        if (command.name().equals("process")) {
            return List.of("list", "add ", "remove ").stream().filter(value -> value.startsWith(current))
                    .map(base::concat).toList();
        }
        if (command.name().equals("list")) return List.of("--sort name", "--sort amount", "--sort amount-desc", "--page ").stream()
                .filter(value -> value.startsWith(current)).map(base::concat).toList();
        return List.of();
    }

    private DepotCliCommandResult startup(DepotCliCommandContext context) {
        List<String> lines = new ArrayList<>(List.of("Crystal Nexus Depot OS", ""));
        if (!context.connected()) {
            lines.add("Network: Disconnected");
            lines.add("");
            lines.add("[WARN] Connect this terminal to your powered Depot Controller with Depot Cable.");
        } else {
            lines.add("Network: Connected");
            lines.add("Stored Types: " + format(context.depot().countEntries("")));
            lines.add("Stored Items: " + format(context.depot().getUsed()));
            int processors = DepotNetwork.craftingProcessorCount(context.player());
            lines.add("Crafting Processors: " + processors);
            lines.add("Processing Machines: " + DepotNetwork.processingMachines(context.player()).size());
            lines.add("JEI Machine Recipes: " + DepotJeiRecipeCache.recipes(context.player()).size());
            lines.add("Processing Patterns: " + context.depot().getProcessingPatterns().size());
            lines.add("Crafting Service: " + (processors > 0 ? "Available" : "Unavailable"));
        }
        lines.add("");
        lines.add("Type \"help\" for a list of commands.");
        return new DepotCliCommandResult(lines);
    }

    private DepotCliCommandResult help(DepotCliCommandContext context, List<String> args) {
        if (!args.isEmpty()) {
            DepotCliCommand command = commands.get(args.getFirst().toLowerCase(Locale.ROOT));
            return command == null ? DepotCliCommandResult.error("Unknown command: " + args.getFirst())
                    : DepotCliCommandResult.info(command.usage(), command.description());
        }
        List<String> lines = new ArrayList<>();
        lines.add("Available commands:");
        canonical.forEach(command -> lines.add(command.usage() + " - " + command.description()));
        return new DepotCliCommandResult(lines);
    }

    private DepotCliCommandResult status(DepotCliCommandContext context, List<String> args) {
        if (!args.isEmpty()) return syntax("status");
        if (!context.connected()) return DepotCliCommandResult.warn("Network: Disconnected or offline");
        DepotControllerBlockEntity controller = DepotSavedData.getController(context.player().serverLevel(), context.player().getUUID());
        List<String> lines = new ArrayList<>();
        lines.add("Network: Connected");
        lines.add("Network ID: " + context.player().getUUID());
        lines.add("Stored Items: " + format(context.depot().getUsed()));
        lines.add("Unique Types: " + format(context.depot().countEntries("")));
        lines.add("Capacity: " + format(context.depot().getFree()) + " free / " + format(context.depot().getCapacity()));
        if (controller != null) lines.add("Power: " + format(controller.getEnergyStorage().getEnergyStored()) + " / " + format(controller.getEnergyStorage().getMaxEnergyStored()) + " FE");
        int processors = DepotNetwork.craftingProcessorCount(context.player());
        lines.add("Crafting Processors: " + processors);
        lines.add("Processing Machines: " + DepotNetwork.processingMachines(context.player()).size());
        lines.add("JEI Machine Recipes: " + DepotJeiRecipeCache.recipes(context.player()).size());
        lines.add("Processing Patterns: " + context.depot().getProcessingPatterns().size());
        lines.add("Crafting Service: " + (processors > 0 ? "Available" : "Unavailable"));
        lines.add("Active Crafting Jobs: " + (context.depot().getCraftingJob() == null ? 0 : 1));
        return new DepotCliCommandResult(lines);
    }

    private DepotCliCommandResult find(DepotCliCommandContext context, List<String> args) {
        if (args.isEmpty()) return syntax("find <text|mod:namespace|tag:id|amount>N>");
        String text = args.stream().filter(value -> !value.startsWith("mod:") && !value.startsWith("tag:")
                && !value.startsWith("amount>") && !value.startsWith("amount<")).collect(java.util.stream.Collectors.joining(" ")).toLowerCase(Locale.ROOT);
        String mod = valueAfter(args, "mod:");
        String tag = valueAfter(args, "tag:");
        String moreValue = valueAfter(args, "amount>");
        String lessValue = valueAfter(args, "amount<");
        Long more = parseLong(moreValue);
        Long less = parseLong(lessValue);
        if (moreValue != null && more == null || lessValue != null && less == null) return DepotCliCommandResult.error("Invalid amount filter.");
        ResourceLocation tagId = tag == null ? null : ResourceLocation.tryParse(tag);
        if (tag != null && tagId == null) return DepotCliCommandResult.error("Invalid tag identifier: " + tag);
        TagKey<net.minecraft.world.item.Item> tagKey = tagId == null ? null : TagKey.create(Registries.ITEM, tagId);
        List<DepotSavedData.Entry> matches = context.depot().entries().stream().filter(entry -> {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(entry.itemId()));
            String haystack = (stack.getHoverName().getString() + " " + entry.itemId()).toLowerCase(Locale.ROOT);
            return (text.isBlank() || haystack.contains(text))
                    && (mod == null || entry.itemId().getNamespace().equals(mod))
                    && (tagKey == null || stack.is(tagKey))
                    && (more == null || entry.count() > more)
                    && (less == null || entry.count() < less);
        }).toList();
        List<String> lines = new ArrayList<>();
        lines.add("Found " + matches.size() + " result" + (matches.size() == 1 ? ":" : "s:"));
        matches.stream().limit(PAGE_SIZE).forEach(entry -> addItem(lines, entry));
        if (matches.size() > PAGE_SIZE) lines.add("...and " + (matches.size() - PAGE_SIZE) + " more. Refine the search.");
        return new DepotCliCommandResult(lines);
    }

    private DepotCliCommandResult list(DepotCliCommandContext context, List<String> args) {
        int page = optionInt(args, "--page", 1);
        if (page < 1) return DepotCliCommandResult.error("Page must be at least 1.");
        String sort = option(args, "--sort", "amount-desc");
        Comparator<DepotSavedData.Entry> comparator = switch (sort) {
            case "name" -> Comparator.comparing(entry -> new ItemStack(BuiltInRegistries.ITEM.get(entry.itemId())).getHoverName().getString());
            case "amount" -> Comparator.comparingLong(DepotSavedData.Entry::count);
            case "amount-desc" -> Comparator.comparingLong(DepotSavedData.Entry::count).reversed();
            default -> null;
        };
        if (comparator == null) return DepotCliCommandResult.error("Unknown sort: " + sort);
        List<DepotSavedData.Entry> entries = context.depot().entries().stream().sorted(comparator).toList();
        int start = (page - 1) * PAGE_SIZE;
        if (start >= entries.size() && !entries.isEmpty()) return DepotCliCommandResult.error("Page does not exist.");
        List<String> lines = new ArrayList<>();
        lines.add("Stored item types - page " + page + "/" + Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE));
        entries.stream().skip(start).limit(PAGE_SIZE).forEach(entry -> addItem(lines, entry));
        return new DepotCliCommandResult(lines);
    }

    private DepotCliCommandResult take(DepotCliCommandContext context, List<String> args) {
        ParsedItemAmount parsed = itemAmount(args);
        if (parsed == null) return itemSyntax("take <item> <amount>");
        DepotItemResolver.Result resolved = DepotItemResolver.stored(context.depot(), parsed.query());
        if (!resolved.found()) return DepotItemResolver.unresolved(parsed.query(), resolved);
        DepotItemResolver.Candidate candidate = resolved.match();
        long available = context.depot().getCount(candidate.id());
        long removed = context.depot().remove(candidate.id(), Math.min(parsed.amount(), available));
        ItemStack stack = new ItemStack(candidate.item(), (int) removed);
        context.player().getInventory().add(stack);
        long overflow = stack.getCount();
        if (overflow > 0) context.depot().deposit(candidate.id(), overflow);
        long retrieved = removed - overflow;
        if (retrieved == parsed.amount()) return DepotCliCommandResult.ok("Retrieved " + retrieved + " " + candidate.name() + ".");
        String reason = available < parsed.amount() ? "Insufficient stored items" : "Player inventory is full";
        return new DepotCliCommandResult(List.of("[WARN] Requested: " + parsed.amount(), "Retrieved: " + retrieved, "Reason: " + reason));
    }

    private DepotCliCommandResult deposit(DepotCliCommandContext context, List<String> args) {
        if (args.size() == 1 && (args.getFirst().equalsIgnoreCase("held") || args.getFirst().equalsIgnoreCase("hand"))) {
            ItemStack held = context.player().getMainHandItem();
            int before = held.getCount();
            long accepted = context.depot().tryDepositAll(held);
            return accepted > 0 ? DepotCliCommandResult.ok("Deposited " + accepted + " item(s).")
                    : DepotCliCommandResult.warn(before == 0 ? "Your hand is empty." : "Item rejected or depot is full.");
        }
        if (args.size() == 1 && args.getFirst().equalsIgnoreCase("inventory")) {
            long attempted = 0, accepted = 0;
            for (ItemStack stack : context.player().getInventory().items) {
                attempted += stack.getCount();
                accepted += context.depot().tryDepositAll(stack);
            }
            return new DepotCliCommandResult(List.of("[OK] Deposited: " + accepted, "Rejected: " + (attempted - accepted)));
        }
        ParsedItemAmount parsed = itemAmount(args);
        if (parsed == null) return itemSyntax("deposit <item> <amount>|held|inventory");
        DepotItemResolver.Result resolved = DepotItemResolver.inventory(context.player(), parsed.query());
        if (!resolved.found()) return DepotItemResolver.unresolved(parsed.query(), resolved);
        long remaining = parsed.amount();
        long accepted = 0;
        for (ItemStack stack : context.player().getInventory().items) {
            if (remaining <= 0 || !stack.is(resolved.match().item())) continue;
            int offered = (int) Math.min(remaining, stack.getCount());
            long inserted = context.depot().deposit(resolved.match().id(), offered);
            stack.shrink((int) inserted);
            accepted += inserted;
            remaining -= inserted;
            if (inserted < offered) break;
        }
        return new DepotCliCommandResult(List.of("[OK] Deposited: " + accepted, "Rejected: " + (parsed.amount() - accepted)));
    }

    private DepotCliCommandResult craft(DepotCliCommandContext context, List<String> args) {
        // Parse optional --machine <id> argument
        ResourceLocation targetMachine = null;
        int machineIndex = args.indexOf("--machine");
        if (machineIndex >= 0 && machineIndex + 1 < args.size()) {
            targetMachine = ResourceLocation.tryParse(args.get(machineIndex + 1));
            if (targetMachine == null) return DepotCliCommandResult.error("Invalid machine id: " + args.get(machineIndex + 1));
            List<String> filtered = new ArrayList<>(args);
            filtered.remove(machineIndex + 1);
            filtered.remove(machineIndex);
            args = filtered;
        }
        ParsedItemAmount parsed = itemAmount(args);
        if (parsed == null) return itemSyntax("craft [--machine <id>] <item> <amount>");
        DepotItemResolver.Result resolved = DepotItemResolver.registry(parsed.query());
        if (!resolved.found()) return DepotItemResolver.unresolved(parsed.query(), resolved);
        // Temporarily set preferred machine if specified
        ResourceLocation previousMachine = targetMachine != null
                ? context.depot().getPreferredMachine(resolved.match().id()) : null;
        if (targetMachine != null) context.depot().setPreferredMachine(resolved.match().id(), targetMachine);
        DepotCraftingService.Result result = DepotCraftingService.craft(context.player(), context.depot(), resolved.match().item(), parsed.amount());
        // Restore previous machine preference if we changed it
        if (targetMachine != null) {
            if (previousMachine != null) context.depot().setPreferredMachine(resolved.match().id(), previousMachine);
            else context.depot().clearPreferredMachine(resolved.match().id());
        }
        if (result.success()) {
            int processors = DepotNetwork.craftingProcessorCount(context.player());
            long ticks = DepotCraftingService.estimatedTicks(result.job().remainingWork(), processors);
            boolean processing = result.job().steps().stream().anyMatch(DepotSavedData.CraftingStep::processing);
            return new DepotCliCommandResult(List.of(
                    "[OK] Queued crafting job #" + result.job().id() + ": " + result.output().getCount() + " "
                            + result.output().getHoverName().getString() + ".",
                    processing ? "Estimated time: machine dependent."
                            : "Estimated time: " + duration(ticks) + " with " + processors + " processor" + (processors == 1 ? "" : "s") + "."));
        }
        List<String> lines = new ArrayList<>();
        lines.add("[ERROR] Unable to craft " + resolved.match().name() + " x" + parsed.amount() + ".");
        result.details().forEach(line -> lines.add("[WARN] " + line));
        return new DepotCliCommandResult(lines);
    }

    private DepotCliCommandResult recipe(DepotCliCommandContext context, List<String> args) {
        if (args.size() < 2) return syntax("recipe list <item>|prefer <item> <number>|clear <item>");
        String action = args.getFirst().toLowerCase(Locale.ROOT);
        int itemEnd = action.equals("prefer") ? args.size() - 1 : args.size();
        if (itemEnd <= 1) return syntax("recipe list <item>|prefer <item> <number>|clear <item>");
        String query = String.join(" ", args.subList(1, itemEnd));
        DepotItemResolver.Result resolved = DepotItemResolver.registry(query);
        if (!resolved.found()) return DepotItemResolver.unresolved(query, resolved);
        ResourceLocation itemId = resolved.match().id();
        List<DepotCraftingService.RecipeChoice> recipes = DepotCraftingService.recipeChoices(
                context.player(), resolved.match().item());

        if (action.equals("list")) {
            if (recipes.isEmpty()) return DepotCliCommandResult.warn("No crafting recipes produce " + resolved.match().name() + ".");
            ResourceLocation preferred = context.depot().getPreferredRecipe(itemId);
            List<String> lines = new ArrayList<>(List.of("Recipes for " + resolved.match().name() + ":"));
            for (int index = 0; index < recipes.size(); index++) {
                DepotCraftingService.RecipeChoice choice = recipes.get(index);
                lines.add((choice.id().equals(preferred) ? "* " : "  ") + (index + 1) + ". ["
                        + (choice.processing() ? choice.category() : "Crafting") + "] " + recipeInputs(choice));
            }
            lines.add("* = preferred");
            lines.add("Use: recipe prefer " + itemId + " <number>");
            return new DepotCliCommandResult(lines);
        }
        if (action.equals("prefer")) {
            if (args.size() < 3) return syntax("recipe prefer <item> <number>");
            ResourceLocation recipeId = numberedChoice(args.getLast(), recipes);
            if (recipeId == null || recipes.stream().noneMatch(candidate -> candidate.id().equals(recipeId))) {
                return DepotCliCommandResult.error("Choose a recipe number from: recipe list " + itemId);
            }
            context.depot().setPreferredRecipe(itemId, recipeId);
            int number = java.util.stream.IntStream.range(0, recipes.size())
                    .filter(index -> recipes.get(index).id().equals(recipeId)).findFirst().orElse(0) + 1;
            return DepotCliCommandResult.ok("Preferred recipe for " + resolved.match().name() + " set to #" + number + ".");
        }
        if (action.equals("clear")) {
            return context.depot().clearPreferredRecipe(itemId)
                    ? DepotCliCommandResult.ok("Cleared the preferred recipe for " + resolved.match().name() + ".")
                    : DepotCliCommandResult.warn("No preferred recipe was set for " + resolved.match().name() + ".");
        }
        return syntax("recipe list <item>|prefer <item> <number>|clear <item>");
    }

    private DepotCliCommandResult machine(DepotCliCommandContext context, List<String> args) {
        if (args.size() < 2) return syntax("machine list <item>|prefer <item> <number>|clear <item>");
        String action = args.getFirst().toLowerCase(Locale.ROOT);
        int itemEnd = action.equals("prefer") ? args.size() - 1 : args.size();
        if (itemEnd <= 1) return syntax("machine list <item>|prefer <item> <number>|clear <item>");
        String query = String.join(" ", args.subList(1, itemEnd));
        DepotItemResolver.Result resolved = DepotItemResolver.registry(query);
        if (!resolved.found()) return DepotItemResolver.unresolved(query, resolved);
        ResourceLocation itemId = resolved.match().id();
        List<ResourceLocation> machines = DepotCraftingService.recipeChoices(context.player(), resolved.match().item())
                .stream().flatMap(choice -> choice.machineTypes().stream()).distinct()
                .sorted(Comparator.comparing(ResourceLocation::toString)).toList();
        if (action.equals("list")) {
            if (machines.isEmpty()) return DepotCliCommandResult.warn("JEI lists no item-handling machine for " + resolved.match().name() + ".");
            ResourceLocation preferred = context.depot().getPreferredMachine(itemId);
            List<String> lines = new ArrayList<>(List.of("Machines for " + resolved.match().name() + ":"));
            for (int index = 0; index < machines.size(); index++) {
                ResourceLocation machineId = machines.get(index);
                net.minecraft.world.level.block.Block block = BuiltInRegistries.BLOCK.get(machineId);
                String name = block == null ? machineId.toString() : new ItemStack(block).getHoverName().getString();
                lines.add((machineId.equals(preferred) ? "* " : "  ") + (index + 1) + ". " + name);
            }
            lines.add("Use: machine prefer " + itemId + " <number>");
            return new DepotCliCommandResult(lines);
        }
        if (action.equals("prefer")) {
            int number;
            try { number = Integer.parseInt(args.getLast()); }
            catch (NumberFormatException ignored) { return DepotCliCommandResult.error("Choose a machine number from: machine list " + itemId); }
            if (number < 1 || number > machines.size()) return DepotCliCommandResult.error("Choose a machine number from: machine list " + itemId);
            context.depot().setPreferredMachine(itemId, machines.get(number - 1));
            return DepotCliCommandResult.ok("Preferred machine for " + resolved.match().name() + " set to #" + number + ".");
        }
        if (action.equals("clear")) {
            return context.depot().clearPreferredMachine(itemId)
                    ? DepotCliCommandResult.ok("Cleared the preferred machine for " + resolved.match().name() + ".")
                    : DepotCliCommandResult.warn("No preferred machine was set for " + resolved.match().name() + ".");
        }
        return syntax("machine list <item>|prefer <item> <number>|clear <item>");
    }

    private DepotCliCommandResult jeiCmd(DepotCliCommandContext context, List<String> args) {
        // JEI is client-only; this server-side handler acknowledges the command
        // while the actual JEI opening happens client-side in DepotCliScreen.
        return DepotCliCommandResult.info("Use the JEI command on the client terminal to open JEI recipes.");
    }

    private static String recipeInputs(DepotCraftingService.RecipeChoice choice) {
        return choice.inputs().stream().map(slot -> slot.alternatives().stream().limit(3).map(stack -> {
            ItemStack item = new ItemStack(BuiltInRegistries.ITEM.get(stack.itemId()));
            return stack.count() + "x " + item.getHoverName().getString();
        }).collect(java.util.stream.Collectors.joining(" / "))).collect(java.util.stream.Collectors.joining(" + "));
    }

    private static ResourceLocation numberedChoice(String value, List<DepotCraftingService.RecipeChoice> choices) {
        try {
            int number = Integer.parseInt(value);
            return number >= 1 && number <= choices.size() ? choices.get(number - 1).id() : null;
        } catch (NumberFormatException ignored) {
            return ResourceLocation.tryParse(value);
        }
    }

    private DepotCliCommandResult process(DepotCliCommandContext context, List<String> args) {
        if (args.isEmpty()) return syntax("process list|add <output_id> <count> <input_id> <count>...|remove <output_id>");
        String action = args.getFirst().toLowerCase(Locale.ROOT);
        if (action.equals("list")) {
            if (args.size() != 1) return syntax("process list");
            List<DepotSavedData.ProcessingPattern> patterns = context.depot().getProcessingPatterns();
            if (patterns.isEmpty()) return DepotCliCommandResult.info("No processing patterns are configured.");
            List<String> lines = new ArrayList<>(List.of("Machine-processing patterns:"));
            patterns.forEach(pattern -> lines.add(pattern.outputs().entrySet().stream()
                    .map(entry -> entry.getValue() + " x " + entry.getKey())
                    .collect(java.util.stream.Collectors.joining(", ")) + " <- "
                    + pattern.inputs().entrySet().stream().map(entry -> entry.getValue() + " x " + entry.getKey())
                    .collect(java.util.stream.Collectors.joining(", "))));
            return new DepotCliCommandResult(lines);
        }
        if (action.equals("remove")) {
            if (args.size() != 2) return syntax("process remove <output_id>");
            ResourceLocation outputId = validItemId(args.get(1));
            if (outputId == null) return DepotCliCommandResult.error("Unknown output item: " + args.get(1));
            return context.depot().removeProcessingPattern(outputId)
                    ? DepotCliCommandResult.ok("Removed processing pattern for " + outputId + ".")
                    : DepotCliCommandResult.warn("No processing pattern exists for " + outputId + ".");
        }
        if (action.equals("add")) {
            int byproduct = args.indexOf("--byproduct");
            int inputEnd = byproduct < 0 ? args.size() : byproduct;
            if (inputEnd < 5 || (inputEnd - 3) % 2 != 0
                    || byproduct >= 0 && (args.size() - byproduct - 1 < 2
                    || (args.size() - byproduct - 1) % 2 != 0)) {
                return syntax("process add <output_id> <count> <input_id> <count>... [--byproduct <id> <count>...]");
            }
            ResourceLocation outputId = validItemId(args.get(1));
            java.util.OptionalInt outputCount = DepotCliParser.positiveQuantity(args.get(2), MAX_QUANTITY);
            if (outputId == null || outputCount.isEmpty() || !context.depot().accepts(outputId)) {
                return DepotCliCommandResult.error("Invalid processing output or amount.");
            }
            Map<ResourceLocation, Long> inputs = new LinkedHashMap<>();
            for (int i = 3; i < inputEnd; i += 2) {
                ResourceLocation inputId = validItemId(args.get(i));
                java.util.OptionalInt inputCount = DepotCliParser.positiveQuantity(args.get(i + 1), MAX_QUANTITY);
                if (inputId == null || inputCount.isEmpty() || !context.depot().accepts(inputId)) {
                    return DepotCliCommandResult.error("Invalid processing input or amount near: " + args.get(i));
                }
                inputs.merge(inputId, (long) inputCount.getAsInt(), Long::sum);
            }
            Map<ResourceLocation, Long> outputs = new LinkedHashMap<>();
            outputs.put(outputId, (long) outputCount.getAsInt());
            for (int i = byproduct + 1; byproduct >= 0 && i < args.size(); i += 2) {
                ResourceLocation byproductId = validItemId(args.get(i));
                java.util.OptionalInt count = DepotCliParser.positiveQuantity(args.get(i + 1), MAX_QUANTITY);
                if (byproductId == null || count.isEmpty() || !context.depot().accepts(byproductId)) {
                    return DepotCliCommandResult.error("Invalid byproduct or amount near: " + args.get(i));
                }
                if (byproductId.equals(outputId)) {
                    return DepotCliCommandResult.error("The primary output cannot also be a byproduct.");
                }
                outputs.merge(byproductId, (long) count.getAsInt(), Long::sum);
            }
            context.depot().setProcessingPattern(outputId, outputCount.getAsInt(), inputs, outputs);
            return DepotCliCommandResult.ok("Processing pattern saved: " + outputCount.getAsInt() + " x "
                    + outputId + " from " + inputs.size() + " input type(s).");
        }
        return syntax("process list|add <output_id> <count> <input_id> <count>...|remove <output_id>");
    }

    private DepotCliCommandResult queue(DepotCliCommandContext context, List<String> args) {
        DepotSavedData.CraftingJob job = context.depot().getCraftingJob();
        if (args.isEmpty()) {
            if (job == null) return DepotCliCommandResult.info("No active crafting jobs.");
            int processors = DepotNetwork.craftingProcessorCount(context.player());
            long ticks = DepotCraftingService.estimatedTicks(job.remainingWork(), processors);
            ItemStack output = new ItemStack(BuiltInRegistries.ITEM.get(job.targetId()));
            long complete = job.totalWork() - job.remainingWork();
            long percent = job.totalWork() <= 0 ? 0 : Math.min(100, Math.round(complete * 100.0 / job.totalWork()));
            List<String> lines = new ArrayList<>();
            lines.add("Job #" + job.id() + ": " + job.amount() + " " + output.getHoverName().getString());
            lines.add("Progress: " + percent + "%");
            DepotSavedData.CraftingStep step = job.currentStep();
            if (step != null) {
                ItemStack stepOutput = new ItemStack(BuiltInRegistries.ITEM.get(step.outputId()));
                lines.add((step.processing() ? "Machine processing " : "Current step ")
                        + (job.currentStepIndex() + 1) + "/" + job.steps().size() + ": "
                        + step.outputAmount() + " " + stepOutput.getHoverName().getString()
                        + " (" + job.currentStepPercent() + "%)");
            }
            lines.add(step != null && step.processing() ? "Waiting for the connected machine output."
                    : processors <= 0 ? "Paused: connect a Crafting Processor."
                    : "Remaining: " + duration(ticks) + " with " + processors + " processor" + (processors == 1 ? "" : "s") + ".");
            return new DepotCliCommandResult(lines);
        }
        if (args.size() == 2 && args.getFirst().equalsIgnoreCase("cancel")) {
            if (!context.hasPermission(DepotCliCommand.Permission.CANCEL)) return DepotCliCommandResult.error("You do not have permission to cancel crafting jobs.");
            int id;
            try {
                id = Integer.parseInt(args.get(1));
            } catch (NumberFormatException ignored) {
                return syntax("queue [cancel <id>]");
            }
            DepotSavedData.CraftingJob cancelled = context.depot().cancelCraftingJob(id);
            return cancelled == null ? DepotCliCommandResult.warn("No queued job with ID " + id + ".")
                    : DepotCliCommandResult.ok("Cancelled crafting job #" + id + ". Current materials were returned to storage.");
        }
        return syntax("queue [cancel <id>]");
    }

    private static ParsedItemAmount itemAmount(List<String> args) {
        if (args.size() < 2) return null;
        java.util.OptionalInt amount = DepotCliParser.positiveQuantity(args.getLast(), MAX_QUANTITY);
        return amount.isEmpty() ? null : new ParsedItemAmount(String.join(" ", args.subList(0, args.size() - 1)), amount.getAsInt());
    }

    private record ParsedItemAmount(String query, int amount) {}

    private static DepotCliCommandResult syntax(String usage) {
        return DepotCliCommandResult.error("Invalid syntax. Usage: " + usage);
    }

    private static DepotCliCommandResult itemSyntax(String usage) {
        return DepotCliCommandResult.error("Invalid syntax or quantity. Usage: " + usage + " (amount 1-" + MAX_QUANTITY + ")");
    }

    private static void addItem(List<String> lines, DepotSavedData.Entry entry) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(entry.itemId()));
        lines.add(stack.getHoverName().getString() + " | " + entry.itemId() + " | Stored: " + format(entry.count()));
    }

    private static String valueAfter(List<String> args, String prefix) {
        return args.stream().filter(value -> value.startsWith(prefix)).map(value -> value.substring(prefix.length())).findFirst().orElse(null);
    }

    private static Long parseLong(String value) {
        if (value == null) return null;
        try { return Long.parseLong(value); } catch (NumberFormatException ignored) { return null; }
    }

    private static ResourceLocation validItemId(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        return id != null && BuiltInRegistries.ITEM.get(id) != net.minecraft.world.item.Items.AIR ? id : null;
    }

    private static String option(List<String> args, String flag, String fallback) {
        int index = args.indexOf(flag);
        return index >= 0 && index + 1 < args.size() ? args.get(index + 1) : fallback;
    }

    private static int optionInt(List<String> args, String flag, int fallback) {
        try { return Integer.parseInt(option(args, flag, Integer.toString(fallback))); }
        catch (NumberFormatException ignored) { return -1; }
    }

    private static String format(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private static String duration(long ticks) {
        if (ticks == Long.MAX_VALUE) return "paused";
        long seconds = Math.max(1, 1 + (ticks - 1) / 20);
        return seconds < 60 ? seconds + "s" : seconds / 60 + "m " + seconds % 60 + "s";
    }

    private static int distance(String left, String right) {
        int[] costs = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) costs[j] = j;
        for (int i = 1; i <= left.length(); i++) {
            int previous = costs[0];
            costs[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int old = costs[j];
                costs[j] = Math.min(Math.min(costs[j] + 1, costs[j - 1] + 1), previous + (left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1));
                previous = old;
            }
        }
        return costs[right.length()];
    }
}
