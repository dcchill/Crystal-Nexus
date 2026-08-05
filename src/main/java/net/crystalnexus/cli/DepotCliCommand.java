package net.crystalnexus.cli;

import java.util.List;

public interface DepotCliCommand {
    enum Permission { VIEW, WITHDRAW, DEPOSIT, CRAFT, CANCEL, STATUS }

    String name();

    default List<String> aliases() { return List.of(); }

    String usage();

    String description();

    Permission permission();

    default boolean requiresNetwork() { return true; }

    DepotCliCommandResult execute(DepotCliCommandContext context, List<String> arguments);

    default List<String> suggest(DepotCliCommandContext context, List<String> arguments, String input) {
        return List.of();
    }
}
