package net.crystalnexus.cli;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepotMachineRecipeCatalogTest {
    @Test
    void connectedMachineRecipesParticipateInCraftableCatalogPlanning() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/crystalnexus/cli/DepotCraftingService.java"));

        assertTrue(source.contains("index.processing().forEach((output, candidates) ->"));
        assertTrue(source.contains("DepotJeiRecipeCache.recipes(player).forEach(candidate ->"));
        assertTrue(source.contains("if (processingAvailable) {"));
        assertFalse(source.contains("if (mode != PlanMode.VISUAL && processingAvailable)"));
    }
}
