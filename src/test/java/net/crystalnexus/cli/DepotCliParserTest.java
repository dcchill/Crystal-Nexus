package net.crystalnexus.cli;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DepotCliParserTest {
    @Test
    void parsesQuotedNamesAndFlags() {
        assertEquals(List.of("take", "Iron Ingot", "64"), DepotCliParser.parse("take \"Iron Ingot\" 64"));
        assertEquals(List.of("list", "--sort", "amount-desc", "--page", "2"),
                DepotCliParser.parse("list --sort amount-desc --page 2"));
    }

    @Test
    void rejectsInvalidQuantities() {
        assertTrue(DepotCliParser.positiveQuantity("64", 4096).isPresent());
        assertTrue(DepotCliParser.positiveQuantity("0", 4096).isEmpty());
        assertTrue(DepotCliParser.positiveQuantity("-1", 4096).isEmpty());
        assertTrue(DepotCliParser.positiveQuantity("4097", 4096).isEmpty());
        assertTrue(DepotCliParser.positiveQuantity("many", 4096).isEmpty());
    }

    @Test
    void boundsCommandOutput() {
        DepotCliCommandResult result = new DepotCliCommandResult(java.util.stream.IntStream.range(0, 100)
                .mapToObj(i -> "x".repeat(600)).toList());
        assertEquals(80, result.lines().size());
        assertTrue(result.lines().stream().allMatch(line -> line.length() == 512));
    }

    @Test
    void rateLimitsCommandPackets() {
        assertTrue(DepotCliParser.mayExecute(Long.MIN_VALUE, 10));
        assertFalse(DepotCliParser.mayExecute(10, 11));
        assertTrue(DepotCliParser.mayExecute(10, 12));
    }

}
