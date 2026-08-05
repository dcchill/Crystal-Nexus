package net.crystalnexus.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

public final class DepotCliParser {
    private DepotCliParser() {
    }

    public static List<String> parse(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (escaped) {
                token.append(c);
                escaped = false;
            } else if (c == '\\' && quoted) {
                escaped = true;
            } else if (c == '"') {
                quoted = !quoted;
            } else if (Character.isWhitespace(c) && !quoted) {
                if (!token.isEmpty()) {
                    tokens.add(token.toString());
                    token.setLength(0);
                }
            } else {
                token.append(c);
            }
        }
        if (escaped) token.append('\\');
        if (!token.isEmpty()) tokens.add(token.toString());
        return tokens;
    }

    public static String normalize(String value) {
        return value.replaceAll("§.", "").trim().toLowerCase(java.util.Locale.ROOT)
                .replace(' ', '_').replace('-', '_');
    }

    public static OptionalInt positiveQuantity(String value, int maximum) {
        try {
            int amount = Integer.parseInt(value);
            return amount > 0 && amount <= maximum ? OptionalInt.of(amount) : OptionalInt.empty();
        } catch (NumberFormatException ignored) {
            return OptionalInt.empty();
        }
    }

    public static boolean mayExecute(long lastCommandTick, long currentTick) {
        return lastCommandTick == Long.MIN_VALUE || currentTick - lastCommandTick >= 2;
    }
}
