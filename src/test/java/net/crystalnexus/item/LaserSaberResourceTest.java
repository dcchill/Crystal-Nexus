package net.crystalnexus.item;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaserSaberResourceTest {
    private static final Path MODELS = Path.of("src/main/resources/assets/crystalnexus/models/item");

    @Test
    void bladeIsTintedAndAbsentFromTheOffModel() throws IOException {
        String powered = compact(MODELS.resolve("laser_saber.json"));
        String off = compact(MODELS.resolve("laser_saber_off.json"));
        String glow = compact(MODELS.resolve("laser_saber_glow.json"));

        assertEquals(6, occurrences(powered, "\"texture\":\"#2\",\"tintindex\":0"));
        assertEquals(2, occurrences(powered, "\"block_light\":15,\"sky_light\":15"));
        assertTrue(powered.contains("\"crystalnexus:off\":1.0"));
        assertFalse(off.contains("laser_saber_blade"));
        assertEquals(3, occurrences(off, "\"from\":"));
        assertEquals(2, occurrences(glow, "\"block_light\":15,\"sky_light\":15"));
        assertTrue(glow.contains("laser_saber_blade"));
        assertTrue(glow.contains("crystalnexus:item/outline"));
        assertTrue(compact(Path.of("src/main/resources/data/minecraft/tags/item/dyeable.json"))
                .contains("crystalnexus:laser_saber"));
        assertFalse(Files.exists(Path.of("src/main/resources/data/crystalnexus/recipe/laser_saber.json")));
    }

    private static String compact(Path path) throws IOException {
        return Files.readString(path).replaceAll("\\s", "");
    }

    private static int occurrences(String value, String target) {
        return (value.length() - value.replace(target, "").length()) / target.length();
    }
}
