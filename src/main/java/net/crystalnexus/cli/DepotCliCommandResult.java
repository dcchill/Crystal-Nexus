package net.crystalnexus.cli;

import java.util.List;

public record DepotCliCommandResult(List<String> lines) {
    public DepotCliCommandResult {
        lines = List.copyOf(lines.stream().limit(80).map(line -> line.length() > 512 ? line.substring(0, 512) : line).toList());
    }

    public static DepotCliCommandResult info(String... lines) {
        return new DepotCliCommandResult(List.of(lines));
    }

    public static DepotCliCommandResult ok(String line) {
        return info("[OK] " + line);
    }

    public static DepotCliCommandResult warn(String... lines) {
        return new DepotCliCommandResult(java.util.Arrays.stream(lines).map(line -> "[WARN] " + line).toList());
    }

    public static DepotCliCommandResult error(String line) {
        return info("[ERROR] " + line);
    }
}
