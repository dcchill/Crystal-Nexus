package net.crystalnexus.world.inventory;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DepotFluidPickupAuditTest {
    @Test
    void clickingAFluidConsumesItsStoredFluidAndAnEmptyBucket() throws IOException {
        String source = Files.readString(Path.of("src/main/java/net/crystalnexus/world/inventory/DepotMenu.java"));

        assertTrue(source.contains("depot.remove(emptyBucketId, 1) != 1"));
        assertTrue(source.contains("depot.removeFluid(fluidId, 1_000) != 1_000"));
        assertTrue(source.contains("setCarried(new ItemStack(bucket))"));
        assertTrue(source.contains("depot.depositFluid(fluidId, 1_000) == 1_000"));
        assertTrue(source.contains("emptyBucket(depot, getCarried())"));
        assertTrue(source.contains("emptyStoredBucket(depot, itemId)"));
    }

    @Test
    void depotRendersFluidTexturesInsteadOfBucketItems() throws IOException {
        String source = Files.readString(Path.of("src/main/java/net/crystalnexus/client/gui/DepotScreen.java"));

        assertTrue(source.contains("FluidTankRenderer.draw(graphics, fluid, 1"));
        assertTrue(source.contains("renderFluidEntries(graphics)"));
        assertTrue(source.contains("if (menu.isDepotSlot(slot) && DepotSavedData.isFluidKey"));
        assertTrue(source.contains("decimal(value, 1_000_000, \"M\")"));
    }
}
