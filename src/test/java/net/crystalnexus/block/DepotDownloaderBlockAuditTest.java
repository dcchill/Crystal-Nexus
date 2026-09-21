package net.crystalnexus.block;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DepotDownloaderBlockAuditTest {
    @Test
    void opensItsMenuWhenThePlayerIsHoldingAnItem() throws IOException {
        String source = Files.readString(Path.of("src/main/java/net/crystalnexus/block/DepotDownloaderBlock.java"));

        assertTrue(source.contains("protected ItemInteractionResult useItemOn"));
        assertTrue(source.contains("useWithoutItem(state, world, pos, player, hit)"));
    }
}
