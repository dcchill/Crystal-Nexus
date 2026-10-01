package net.crystalnexus.item;

import java.util.Locale;

public final class EnchantmentLevelFormat {
    private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] NUMERALS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private EnchantmentLevelFormat() {}

    public static String format(int level) {
        if (level < 1 || level > 1000) return String.format(Locale.ROOT, "%,d", level);
        var result = new StringBuilder();
        for (int i = 0; i < VALUES.length; i++) {
            while (level >= VALUES[i]) {
                result.append(NUMERALS[i]);
                level -= VALUES[i];
            }
        }
        return result.toString();
    }
}
