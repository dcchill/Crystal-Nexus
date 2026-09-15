package net.crystalnexus.cli;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeCycleGuardTest {
    @Test
    void rejectsRecipeThatReturnsToAnAncestor() {
        assertTrue(RecipeCycleGuard.loopsIntoPath(Stream.of("copper_block"), "ee_matter",
                Set.of("copper_block")));
        assertFalse(RecipeCycleGuard.loopsIntoPath(Stream.of("copper_ingot"), "ee_matter",
                Set.of("copper_block")));
    }

    @Test
    void rejectsSelfReferentialRecipe() {
        assertTrue(RecipeCycleGuard.loopsIntoPath(Stream.of("ee_matter"), "ee_matter", Set.of()));
    }
}
