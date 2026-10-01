package net.crystalnexus.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnchantmentLevelFormatTest {
    @Test void formatsRomanNumeralsAndLargeLevels() {
        int[] levels = {1, 4, 9, 40, 90, 255, 271, 400, 500, 900, 999, 1000, 1001, 10000, Integer.MAX_VALUE, 0};
        String[] expected = {"I", "IV", "IX", "XL", "XC", "CCLV", "CCLXXI", "CD", "D", "CM", "CMXCIX", "M", "1,001", "10,000", "2,147,483,647", "0"};
        for (int i = 0; i < levels.length; i++) assertEquals(expected[i], EnchantmentLevelFormat.format(levels[i]));
    }
}
