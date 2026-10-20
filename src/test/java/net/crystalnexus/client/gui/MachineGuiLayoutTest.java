package net.crystalnexus.client.gui;

import net.crystalnexus.world.inventory.MachineGuiLayout;
import net.crystalnexus.world.inventory.MachineGuiLayout.Rect;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MachineGuiLayoutTest {
    @Test void inventoryMovesOnlyEnoughWholeRowsToLeaveTheMinimumGap() {
        for (int original : new int[]{55, 63, 84, 104, 126, 140}) {
            for (int bottom = 0; bottom <= original + 36; bottom++) {
                int y = MachineGuiLayout.inventoryY(original, bottom);
                assertTrue(y >= original);
                assertEquals(0, (y - original) % 18);
                assertTrue(y - 1 - bottom >= MachineGuiLayout.INVENTORY_GAP);
                if (y > original) assertTrue(y - 19 - bottom < MachineGuiLayout.INVENTORY_GAP);
                assertEquals(y, MachineGuiLayout.inventoryY(y, bottom));
            }
        }
        assertEquals(102, MachineGuiLayout.inventoryY(84, 83), "Piston and three-tank chamber");
        assertEquals(126, MachineGuiLayout.inventoryY(126, 107), "Transmutation already has room");
    }

    @Test void spacingPreservesTankAndControlPositions() {
        var before = new MachineGuiLayout.Grid(8, 84);
        var after = new MachineGuiLayout.Grid(8, MachineGuiLayout.inventoryY(84, 83));
        for (Rect input : List.of(new Rect(121, 16, 32, 56), new Rect(18, 14, 32, 56),
                new Rect(116, 29, 16, 42), new Rect(26, 30, 16, 34))) {
            Rect tank = after.tank(input.x(), input.y(), input.width(), input.height());
            assertEquals(before.tank(input.x(), input.y(), input.width(), input.height()), tank);
            assertTrue(after.y() - 1 - (tank.bottom() + 1) >= MachineGuiLayout.INVENTORY_GAP);
        }
        assertEquals(before.control(65, 55, 46, 20), after.control(65, 55, 46, 20));
        var elevatorBefore = new MachineGuiLayout.Grid(8, 63);
        var elevatorAfter = new MachineGuiLayout.Grid(8, MachineGuiLayout.inventoryY(63, 62));
        assertEquals(elevatorBefore.control(33, 35, 108, 20), elevatorAfter.control(33, 35, 108, 20));
    }

    @Test void compactTankGroupsAreCentredAndClearOfTheInventory() {
        var grid = new MachineGuiLayout.Grid(8, 84);
        for (boolean purge : new boolean[]{false, true}) {
            List<Rect> controls = inventory(84);
            Rect group = null;
            for (int i = 0; i < 2; i++) {
                Rect tank = MachineGuiLayout.twoTank(i, purge);
                assertEquals(tank, grid.tank(tank.x(), tank.y(), tank.width(), tank.height()));
                Rect outer = new Rect(tank.x() - 1, tank.y() - 1, tank.width() + 2, tank.height() + 2);
                Rect slot = MachineGuiLayout.slot(tank.x(), MachineGuiLayout.TWO_TANK_SLOT_Y);
                controls.add(outer); controls.add(slot);
                Rect column = outer.union(slot);
                if (purge) {
                    Rect button = grid.control(tank.x() - 1, MachineGuiLayout.PURGE_Y, 18, 18);
                    controls.add(button); column = column.union(button);
                }
                group = group == null ? column : group.union(column);
            }
            assertEquals(88, group.x() + group.width() / 2);
            assertTrue(Math.abs(42 - (group.y() + group.height() / 2)) <= 9, "Centre stays within half a grid cell");
            assertTrue(group.y() >= 0 && group.bottom() < 83);
            assertDisjoint(controls);
        }
    }

    @Test void blueprintControlsStayInsideAfterGridAlignment() {
        var grid = new MachineGuiLayout.Grid(8, 84);
        List<Rect> controls = inventory(84);
        controls.add(new Rect(7, 17, 62, 62));
        controls.add(MachineGuiLayout.blueprintName());
        for (boolean clear : new boolean[]{false, true}) {
            Rect button = MachineGuiLayout.blueprintButton(clear);
            assertEquals(button, grid.control(button.x(), button.y(), button.width(), button.height()));
            assertTrue(button.x() >= 7 && button.right() <= 169 && button.bottom() < 83);
            controls.add(button);
        }
        assertDisjoint(controls);
    }

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
            controls.add(MachineGuiLayout.energy(137));
            for (int i = 0; i < upgrades; i++) {
                int[] s = MachineGuiLayout.upgradeSlot(i);
                controls.add(MachineGuiLayout.slot(s[0], s[1]));
            }
            for (int i = 0; i < 2; i++) controls.add(MachineGuiLayout.progress(176, i, 2));
            assertDisjoint(controls);
        }
    }

    @Test void independentReadingsUseFullWidthRows() {
        Rect first = MachineGuiLayout.progress(176, 0, 2);
        Rect second = MachineGuiLayout.progress(176, 1, 2);
        assertEquals(new Rect(8, -12, 160, 8), first);
        assertEquals(new Rect(8, 2, 160, 8), second);
        assertFalse(first.intersects(second));
        assertEquals(first, MachineGuiLayout.progress(176, 0, 1));
    }

    @Test void assemblerModesNeverOpenOverTheInventory() {
        List<Rect> controls = inventory(84);
        Rect input = MachineGuiLayout.slot(44, MachineGuiLayout.ASSEMBLER_SLOT_Y);
        controls.add(input);
        controls.add(MachineGuiLayout.slot(116, MachineGuiLayout.ASSEMBLER_SLOT_Y));
        for (int i = 0; i < 3; i++) controls.add(MachineGuiLayout.assemblerMode(i));
        assertDisjoint(controls);
        Rect modes = MachineGuiLayout.assemblerMode(0);
        int inventoryTop = MachineGuiLayout.slot(8, 84).y();
        assertEquals(modes.y() - input.bottom(), inventoryTop - modes.bottom(), "Even gaps between slots, modes and inventory");
        assertEquals(0, Math.floorMod(modes.y() - inventoryTop, 18), "Modes stay on the inventory grid");
    }

    @Test void completeFootprintFitsSmallAndLargeViewportsAndMouseCoordinatesRoundTrip() {
        for (int[] viewport : List.of(new int[]{320, 240}, new int[]{427, 240}, new int[]{640, 360}, new int[]{960, 540})) {
            for (Rect bounds : List.of(
                    new Rect(0, -44, 200, 210).union(MachineGuiLayout.energy(137)),
                    new Rect(0, -44, 356, 269).union(MachineGuiLayout.energy(195)),
                    new Rect(0, -44, 300, 282).union(MachineGuiLayout.energy(209)),
                    new Rect(0, -44, 500, 344))) {
                float scale = MachineGuiLayout.fitScale(viewport[0], viewport[1], bounds);
                int left = MachineGuiLayout.centeredOrigin((int)(viewport[0] / scale), bounds.x(), bounds.width());
                int top = MachineGuiLayout.centeredOrigin((int)(viewport[1] / scale), bounds.y(), bounds.height());
                assertTrue((left + bounds.x()) * scale >= 0);
                assertTrue((top + bounds.y()) * scale >= 0);
                assertTrue((left + bounds.right()) * scale <= viewport[0]);
                assertTrue((top + bounds.bottom()) * scale <= viewport[1]);
                double logicalX = left + bounds.x() + 8.5, logicalY = top + bounds.bottom() - 1.5;
                double physicalX = logicalX * scale, physicalY = logicalY * scale;
                assertEquals(logicalX, physicalX / scale, 0.0001);
                assertEquals(logicalY, physicalY / scale, 0.0001);
                assertTrue(bounds.contains(physicalX / scale - left, physicalY / scale - top));
            }
        }
    }

    @Test void energyPanelSharesTheMainBorderAndEndsAtTheInventoryRows() {
        for (int y : new int[]{57, 84, 142}) {
            Rect energy = MachineGuiLayout.energy(MachineGuiLayout.slot(8, y + 36).bottom());
            assertEquals(28, energy.width());
            assertEquals(1, energy.right());
            assertEquals(0, energy.y());
            assertEquals(y + 53, energy.bottom());
            assertTrue(energy.contains(-1, energy.bottom() - 1));
            assertFalse(energy.contains(-1, energy.bottom()));
            List<Rect> controls = inventory(y);
            controls.add(energy);
            assertDisjoint(controls);
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
