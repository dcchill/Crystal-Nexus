package net.crystalnexus.client.gui;

import net.crystalnexus.world.inventory.MachineGuiLayout;
import net.crystalnexus.world.inventory.MachineGuiLayout.Rect;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Progress-reading counts, overlay anchoring and the Matter Transmutation octagon. */
class MachineGuiCleanupTest {
    private static final int OUTPUT = MachineGuiLayout.TRANSMUTATION_OUTPUT;

    @Test void roundedCornersStayInsideTheFrameAndWidenTowardsItsCentre() {
        assertEquals(0, MachineGuiTheme.cornerInset(0, 0));
        assertEquals(3, MachineGuiTheme.cornerInset(3, 0));
        assertEquals(1, MachineGuiTheme.cornerInset(3, 1));
        assertEquals(1, MachineGuiTheme.cornerInset(3, 2));
        for (int radius = 1; radius <= 6; radius++) {
            int previous = radius;
            for (int row = 0; row < radius; row++) {
                int inset = MachineGuiTheme.cornerInset(radius, row);
                assertTrue(inset > 0 && inset <= previous);
                previous = inset;
            }
            assertEquals(0, MachineGuiTheme.cornerInset(radius, radius));
        }
    }

    @Test void machinesKeepOneHeaderReading() {
        for (String menu : List.of("IronSmelterGuiMenu", "CrusherGuiMenu", "CircuitPressGUIMenu", "CrystalPurifierGUIMenu",
                "FluidChemicalReactionChamberGUIMenu", "MatterTransmutationGUIMenu", "CraftingFactoryGUIMenu",
                "OreProGUIMenu", "SingularityCompressorGUIMenu", "BioMCGuiMenu", "BioMGuiMenu", "BioSIMGuiMenu"))
            assertEquals(List.of("progress"), MachineGuiLayout.readings(menu), menu);
    }

    @Test void upgradeBayWrapsItsSlotsWithEvenMarginsAndGrowsWithTheCount() {
        int m = MachineGuiLayout.SIDE_MARGIN, bayX = MachineGuiLayout.BODY_WIDTH - 1;
        int previousHeight = 0;
        for (int count = 1; count <= 5; count++) {
            List<Integer> gaps = new ArrayList<>();
            int previous = 0, left = 0;
            for (int i = 0; i < count; i++) {
                int[] s = MachineGuiLayout.upgradeSlot(i);
                Rect frame = MachineGuiLayout.slot(s[0], s[1]);
                gaps.add(frame.y() - previous); previous = frame.bottom();
                left = frame.x();
            }
            int height = previous + m;
            gaps.add(height - previous);
            assertTrue(gaps.stream().allMatch(g -> g == m), "Bay gaps must all equal the side margin: " + gaps);
            assertEquals(m, left - bayX, "Left margin");
            assertEquals(m, bayX + MachineGuiLayout.SIDE_PANEL_WIDTH - (left + 18), "Right margin");
            assertTrue(height > previousHeight, "Bay grows with each slot");
            previousHeight = height;
        }
        assertEquals(MachineGuiLayout.SIDE_PANEL_WIDTH, MachineGuiLayout.energy(137).width(), "Matches FE panel width");
    }

    @Test void adjacentInputsShareOneArrowUnlessTheMachineCombinesThem() {
        Rect a = MachineGuiLayout.slot(44, 30), b = MachineGuiLayout.slot(62, 30), out = MachineGuiLayout.slot(116, 30);
        Rect[] ends = {a, b, out};
        int[][] routes = {{0, 2}, {1, 2}};
        var merged = MachineGuiFlow.mergeAdjacent(routes, i -> ends[i], true);
        assertEquals(1, merged.size());
        assertEquals(a.union(b), merged.getFirst().from());
        assertEquals(2, MachineGuiFlow.mergeAdjacent(routes, i -> ends[i], false).size());
        Rect apart = MachineGuiLayout.slot(44, 66);
        Rect[] spaced = {a, apart, out};
        assertEquals(2, MachineGuiFlow.mergeAdjacent(routes, i -> spaced[i], true).size(), "Separated inputs keep their feeds");
        for (String menu : List.of("CircuitPressGUIMenu", "CrystalPurifierGUIMenu", "FluidChemicalReactionChamberGUIMenu"))
            assertTrue(MachineGuiRoutes.combining(menu), menu);
        assertFalse(MachineGuiRoutes.combining("ArcFurnaceMenu"));
    }

    @Test void readingLabelsAreTranslated() throws Exception {
        try (var stream = getClass().getResourceAsStream("/assets/crystalnexus/lang/en_us.json")) {
            assertNotNull(stream);
            var lang = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,
                    java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for (String menu : List.of("IronSmelterGuiMenu", "OreProGUIMenu", "SingularityCompressorGUIMenu"))
                for (String key : MachineGuiLayout.readings(menu))
                    assertTrue(lang.has("gui.crystalnexus.machine.reading." + key), key);
        }
    }

    @Test void overlayFramesCoverTheirSlotsExactly() {
        // Crafting Factory filter (133,35) and Smart Splitter (input + three filters + overflow), as snapped by their menus.
        int[][] slots = {{133, 35}, {52, 53}, {25, 26}, {79, 26}, {133, 26}, {106, 53}};
        List<Rect> frames = new ArrayList<>();
        for (int[] s : slots) {
            int x = MachineGuiLayout.grid(s[0], 8), y = MachineGuiLayout.grid(s[1], 84);
            Rect frame = MachineGuiLayout.slot(x, y);
            assertEquals(new Rect(x - 1, y - 1, 18, 18), frame);
            assertTrue(frame.contains(x, y) && frame.contains(x + 15, y + 15), "Overlay must cover the 16px item area");
            assertFalse(frame.contains(x - 2, y - 2) || frame.contains(x + 17, y + 17));
            frames.add(frame);
        }
        for (int i = 1; i < frames.size(); i++) for (int j = i + 1; j < frames.size(); j++)
            assertFalse(frames.get(i).intersects(frames.get(j)), "Splitter overlays must not bleed into neighbours");
    }

    @Test void transmutationRestoresTheOriginalClockwiseOctagonAroundItsOutput() {
        int[] centre = MachineGuiLayout.transmutationSlot(OUTPUT), original = MachineGuiLayout.transmutationOriginal(OUTPUT);
        assertEquals(0, Math.floorMod(centre[0] - 8, 18), "Output snaps to the inventory column grid");
        assertEquals(0, Math.floorMod(centre[1] - MachineGuiLayout.TRANSMUTATION_INVENTORY_Y, 18), "Output snaps to the inventory row grid");
        double previous = -1;
        for (int i = 0; i < OUTPUT; i++) {
            int[] slot = MachineGuiLayout.transmutationSlot(i), source = MachineGuiLayout.transmutationOriginal(i);
            assertEquals(source[0] - original[0], slot[0] - centre[0], "Input " + i + " keeps its original x offset");
            assertEquals(source[1] - original[1], slot[1] - centre[1], "Input " + i + " keeps its original y offset");
            // Clockwise from the top in screen space (y grows downwards).
            double angle = Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(slot[0] - centre[0], centre[1] - slot[1]))), 360);
            assertEquals(i * 45, angle, 1, "Input " + i + " sits on its octagon vertex");
            assertTrue(angle > previous);
            previous = angle;
        }
    }

    @Test void transmutationSlotsStayClearOfEachOtherAndTheInventory() {
        List<Rect> machine = new ArrayList<>();
        for (int i = 0; i <= OUTPUT; i++) {
            int[] s = MachineGuiLayout.transmutationSlot(i);
            machine.add(MachineGuiLayout.slot(s[0], s[1]));
        }
        List<Rect> inventory = new ArrayList<>();
        int y = MachineGuiLayout.TRANSMUTATION_INVENTORY_Y;
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) inventory.add(MachineGuiLayout.slot(8 + col * 18, y + row * 18));
        for (int col = 0; col < 9; col++) inventory.add(MachineGuiLayout.slot(8 + col * 18, y + 58));
        for (int i = 0; i < machine.size(); i++) {
            assertTrue(machine.get(i).y() >= 0, "Input " + i + " stays out of the header");
            for (int j = i + 1; j < machine.size(); j++) assertFalse(machine.get(i).intersects(machine.get(j)), i + " overlaps " + j);
            for (Rect slot : inventory) assertFalse(machine.get(i).intersects(slot), i + " overlaps the player inventory");
        }
        Rect lowest = machine.get(4);
        assertTrue(lowest.bottom() < inventory.getFirst().y(), "Lower inputs leave working space above the inventory");

        assertEquals(0, MachineGuiRoutes.connections("MatterTransmutationGUIMenu").length);
        for (int i = 0; i < OUTPUT; i++) assertEquals("input", MachineGuiRoutes.role("MatterTransmutationGUIMenu", i));
        assertEquals("output", MachineGuiRoutes.role("MatterTransmutationGUIMenu", OUTPUT));
        assertTrue(inventory.getFirst().y() - lowest.bottom() >= MachineGuiLayout.INVENTORY_GAP);
    }

    @Test void onlyTransmutationInputsReceiveClockwiseRainbowBorders() {
        var colors = new HashSet<Integer>();
        for (int i = 0; i < OUTPUT; i++) {
            int color = MachineGuiTheme.slotColor("MatterTransmutationGUIMenu", i, false, 3);
            assertEquals(net.minecraft.util.Mth.hsvToRgb(i / 8f, 0.75f, 1f), color);
            assertTrue(colors.add(color), "Each input receives its own hue");
            assertEquals(0x52788F, MachineGuiTheme.slotColor("MatterTransmutationGUIMenu", i, true, 3));
            assertEquals(MachineGuiTheme.accent(3), MachineGuiTheme.slotColor("PistonGenGUIMenu", i, false, 3));
        }
        int red = MachineGuiTheme.slotColor("MatterTransmutationGUIMenu", 0, false, 3);
        assertTrue((red >> 16 & 255) > (red >> 8 & 255) && (red >> 16 & 255) > (red & 255));
        for (int i = OUTPUT; i < OUTPUT + 6; i++)
            assertEquals(MachineGuiTheme.accent(3), MachineGuiTheme.slotColor("MatterTransmutationGUIMenu", i, false, 3));
    }
}
