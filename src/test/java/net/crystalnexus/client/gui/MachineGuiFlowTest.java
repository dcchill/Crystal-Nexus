package net.crystalnexus.client.gui;

import net.crystalnexus.world.inventory.MachineGuiLayout;
import net.crystalnexus.world.inventory.MachineGuiLayout.Rect;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MachineGuiFlowTest {
    @Test void groupedMachinesUseOneConnectionAndKeepAllSlotRoles() {
        for (String menu : List.of("AcceleratorGuiMenu", "ExtractinatorGuiMenu", "NodeMinerGUIMenu", "NodeExtractorGUIMenu"))
            assertEquals(1, MachineGuiRoutes.connections(menu).length, menu);
        for (int input : new int[]{0, 2, 3, 4}) assertEquals("input", MachineGuiRoutes.role("AcceleratorGuiMenu", input));
        for (int output : new int[]{1, 5, 6, 7}) assertEquals("output", MachineGuiRoutes.role("AcceleratorGuiMenu", output));
        for (int output = 1; output <= 6; output++) assertEquals("output", MachineGuiRoutes.role("ExtractinatorGuiMenu", output));
        for (int output = 0; output < 3; output++) assertEquals("output", MachineGuiRoutes.role("NodeMinerGUIMenu", output));
        assertEquals("set_plant", MachineGuiRoutes.role("BioMCGuiMenu", 3));
        assertEquals("input", MachineGuiRoutes.role("BioMCGuiMenu", 0));
        assertEquals("output", MachineGuiRoutes.role("BioMCGuiMenu", 1));
    }

    @Test void controlsShareTankAndSlotGridBorders() {
        for (int y : new int[]{84, 99, 111, 140, 266}) {
            var grid = new MachineGuiLayout.Grid(8, y);
            Rect button = grid.control(60,54,46,20);
            assertEquals(0, Math.floorMod(button.x() - 7,18));
            assertEquals(0, Math.floorMod(button.y() - (y - 1),18));
            assertEquals(0, button.width() % 18);
            assertEquals(0, button.height() % 18);
            assertEquals(button, grid.control(button.x(),button.y(),button.width(),button.height()));
        }
    }
    @Test void tanksUseWholeInventoryCellsAndStopBeforeTheInventory() {
        for (int y : new int[]{57, 84, 99, 111, 140, 143}) {
            var grid = new MachineGuiLayout.Grid(8, y);
            for (Rect input : List.of(new Rect(115,16,16,34), new Rect(121,16,32,56), new Rect(134,26,15,33))) {
                Rect tank = grid.tank(input.x(), input.y(), input.width(), input.height());
                assertEquals(0, Math.floorMod(tank.x() - 8, 18));
                assertEquals(0, Math.floorMod(tank.y() - y, 18));
                assertEquals(0, (tank.width() + 2) % 18);
                assertEquals(0, (tank.height() + 2) % 18);
                assertTrue(tank.bottom() + 1 <= y - 1);
            }
        }
    }

    @Test void smelterArrowFitsBetweenSlotsWithoutCrossingTheirFrames() {
        Rect input = MachineGuiLayout.slot(44,30), output = MachineGuiLayout.slot(116,30);
        var path = MachineGuiFlow.path(input, output, List.of(input, output), new Rect(3,2,170,77));
        assertFalse(path.isEmpty());
        assertTrue(path.getFirst().x() < path.getLast().x());
        assertEquals(path.getFirst().y(), path.getLast().y());
        assertTrue(path.getFirst().x() > input.right());
        assertTrue(path.getLast().x() < output.x());
        assertEquals(2, path.size(), "Aligned slots need one straight segment");
    }

    @Test void solidArrowheadsHaveTheSameSymmetricProfileInEveryDirection() {
        var end = new MachineGuiFlow.Point(50, 50);
        for (var before : List.of(new MachineGuiFlow.Point(30, 50), new MachineGuiFlow.Point(70, 50),
                new MachineGuiFlow.Point(50, 30), new MachineGuiFlow.Point(50, 70))) {
            var head = MachineGuiFlow.arrowHead(before, end);
            assertEquals(4, head.size());
            boolean horizontal = before.y() == end.y();
            for (int i = 0; i < head.size(); i++) {
                Rect row = head.get(i);
                assertEquals(1, horizontal ? row.width() : row.height());
                assertEquals((i + 1) * 2, horizontal ? row.height() : row.width());
                assertEquals(100, horizontal ? row.y() + row.bottom() : row.x() + row.right(), "Head stays centred on the shaft");
            }
        }
    }

    @Test void obstructedTankFeedsStayOrthogonalAndEnterTheTargetSquarely() {
        Rect first = new Rect(25, 29, 18, 36), middle = new Rect(79, 29, 18, 36), output = new Rect(133, 29, 18, 36);
        var obstacles = List.of(first, middle, output, new Rect(25, 11, 18, 18), new Rect(79, 11, 18, 18), new Rect(133, 11, 18, 18),
            MachineGuiLayout.slot(26, 66), MachineGuiLayout.slot(80, 66), MachineGuiLayout.slot(134, 66));
        Rect area = new Rect(3, 2, 170, 97);
        var path = MachineGuiFlow.path(first, output, obstacles, area);
        assertFalse(path.isEmpty(), "A blocked middle tank must still leave a route");
        for (int i = 1; i < path.size(); i++) {
            var a = path.get(i - 1); var b = path.get(i);
            assertTrue(a.x() == b.x() ^ a.y() == b.y(), "Each segment is straight and nonzero");
            Rect line = new Rect(Math.min(a.x(), b.x()) - 1, Math.min(a.y(), b.y()) - 1,
                Math.abs(a.x() - b.x()) + 2, Math.abs(a.y() - b.y()) + 2);
            for (Rect obstacle : obstacles) assertFalse(line.intersects(obstacle));
        }
        var tip = path.getLast(); var previous = path.get(path.size() - 2);
        assertEquals(output.y() + output.height() / 2, tip.y());
        assertEquals(tip.y(), previous.y(), "Side entry has a horizontal final segment");
        assertTrue(tip.x() > previous.x());
        for (Rect slice : MachineGuiFlow.arrowHead(previous, tip))
            for (Rect obstacle : obstacles) assertFalse(slice.intersects(obstacle));
    }

    @Test void pressCombinesBothItemsAndFluidAtTheSameJunction() {
        Rect input = MachineGuiLayout.slot(62,12), die = MachineGuiLayout.slot(98,12);
        Rect tank = new Rect(115,11,18,36), output = MachineGuiLayout.slot(80,48);
        var paths = MachineGuiFlow.press(input, die, tank, output);
        assertEquals(4, paths.size());
        assertEquals(paths.get(0).getLast(), paths.get(1).getLast());
        assertEquals(paths.get(0).getLast(), paths.get(2).getLast());
        assertEquals(paths.get(0).getLast(), paths.get(3).getFirst());
        assertTrue(paths.get(3).getFirst().y() < paths.get(3).getLast().y());
        for (var path : paths) for (var point : path) for (Rect obstacle : List.of(input,die,tank,output))
            assertFalse(obstacle.contains(point.x(),point.y()));
    }

    @Test void chemicalInputsMergeIntoOneStraightDirectionArrow() {
        Rect[] slots = {MachineGuiLayout.slot(44,12), MachineGuiLayout.slot(44,30),
                MachineGuiLayout.slot(44,48), MachineGuiLayout.slot(116,30)};
        var routes = MachineGuiFlow.mergeAdjacent(MachineGuiRoutes.connections("ChemicalReactionChamberGUIMenu"), i -> slots[i], true);
        assertEquals(1, routes.size());
        var path = MachineGuiFlow.path(routes.getFirst().from(), routes.getFirst().to(), List.of(slots), new Rect(3,2,170,77));
        assertFalse(path.isEmpty());
        assertTrue(path.stream().allMatch(point -> point.y() == path.getFirst().y()));
        assertTrue(path.getFirst().x() < path.getLast().x());
        assertEquals(3, MachineGuiFlow.press(slots[0], slots[1], null, slots[3]).size(), "Tankless press has no fluid arrow");
        assertArrayEquals(new int[][]{{0, 1}}, MachineGuiRoutes.connections("SeparatorGuiMenu"));
    }

    @Test void progressHandlesIdleZeroCapacityAndInvalidValues() {
        assertEquals(0, MachineGuiLayout.fraction(0,0));
        assertEquals(0, MachineGuiLayout.fraction(Double.NaN,100));
        assertEquals(0, MachineGuiLayout.fraction(-1,100));
        assertEquals(.5, MachineGuiLayout.fraction(50,100));
        assertEquals(1, MachineGuiLayout.fraction(200,100));
        assertEquals(0, MachineGuiLayout.fraction(1,Double.POSITIVE_INFINITY));
    }
}
