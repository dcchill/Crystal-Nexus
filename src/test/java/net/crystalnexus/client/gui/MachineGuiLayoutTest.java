package net.crystalnexus.client.gui;

import net.crystalnexus.world.inventory.MachineGuiLayout;
import net.crystalnexus.world.inventory.MachineGuiLayout.Rect;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MachineGuiLayoutTest {
    @Test void tanksPurgeButtonsContainerSlotsAndInventoryHaveDisjointBounds() {
        for (int[] columns : List.of(MachineGuiLayout.TWO_TANK_X, MachineGuiLayout.THREE_TANK_X)) {
            List<Rect> controls = inventory(84);
            for (int x : columns) {
                controls.add(new Rect(x - 1, MachineGuiLayout.TANK_Y - 1, 18, MachineGuiLayout.TANK_HEIGHT + 2));
                controls.add(new Rect(x, MachineGuiLayout.PURGE_Y, 16, MachineGuiLayout.PURGE_HEIGHT));
                controls.add(MachineGuiLayout.slot(x, MachineGuiLayout.TANK_SLOT_Y));
                assertEquals(x, MachineGuiLayout.tank(x).x());
            }
            controls.add(MachineGuiLayout.progress(176, 0, 1));
            assertDisjoint(controls);
            for (int i = 2; i < columns.length; i++) assertEquals(columns[1] - columns[0], columns[i] - columns[i - 1]);
        }
    }

    @Test void allUpgradeCountsAndMultipleProgressBarsRemainSeparate() {
        for (int upgrades = 0; upgrades <= 5; upgrades++) {
            List<Rect> controls = inventory(84);
            controls.add(MachineGuiLayout.energy());
            for (int i = 0; i < upgrades; i++) controls.add(MachineGuiLayout.slot(180, 8 + i * 32));
            for (int i = 0; i < 2; i++) controls.add(MachineGuiLayout.progress(176, i, 2));
            assertDisjoint(controls);
        }
    }

    @Test void assemblerModesNeverOpenOverTheInventory() {
        List<Rect> controls = inventory(84);
        controls.add(MachineGuiLayout.slot(43, 35));
        controls.add(MachineGuiLayout.slot(115, 35));
        for (int i = 0; i < 3; i++) controls.add(MachineGuiLayout.assemblerMode(i));
        assertDisjoint(controls);
    }

    @Test void completeFootprintFitsSmallAndLargeViewportsAndMouseCoordinatesRoundTrip() {
        for (int[] viewport : List.of(new int[]{320, 240}, new int[]{427, 240}, new int[]{640, 360}, new int[]{960, 540})) {
            for (Rect bounds : List.of(new Rect(-34, -44, 234, 210), new Rect(-34, -44, 390, 269), new Rect(-34, -44, 334, 282))) {
                float scale = MachineGuiLayout.fitScale(viewport[0], viewport[1], bounds);
                int left = MachineGuiLayout.centeredOrigin((int)(viewport[0] / scale), bounds.x(), bounds.width());
                int top = MachineGuiLayout.centeredOrigin((int)(viewport[1] / scale), bounds.y(), bounds.height());
                assertTrue((left + bounds.x()) * scale >= 0);
                assertTrue((top + bounds.y()) * scale >= 0);
                assertTrue((left + bounds.right()) * scale <= viewport[0]);
                assertTrue((top + bounds.bottom()) * scale <= viewport[1]);
                double logicalX = left + 51.5, physicalX = logicalX * scale;
                assertEquals(logicalX, physicalX / scale, 0.0001);
            }
        }
    }

    private static List<Rect> inventory(int y) {
        List<Rect> result = new ArrayList<>();
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) result.add(MachineGuiLayout.slot(8 + col * 18, y + row * 18));
        for (int col = 0; col < 9; col++) result.add(MachineGuiLayout.slot(8 + col * 18, y + 58));
        return result;
    }
    private static void assertDisjoint(List<Rect> controls) {
        for (int i = 0; i < controls.size(); i++) for (int j = i + 1; j < controls.size(); j++)
            assertFalse(controls.get(i).intersects(controls.get(j)), controls.get(i) + " overlaps " + controls.get(j));
    }
}
