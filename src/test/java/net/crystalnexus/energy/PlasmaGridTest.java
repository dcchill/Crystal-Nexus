package net.crystalnexus.energy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlasmaGridTest {
    private int[] grid(int... entries) {
        int[] grid = new int[PlasmaGrid.SIZE];
        for (int i = 0; i < entries.length; i += 2) grid[entries[i]] = entries[i + 1];
        return grid;
    }

    @Test void adjacencyDoesNotWrapOrIncludeDiagonals() {
        assertArrayEquals(new int[]{1, 9}, PlasmaGrid.neighbors(0));
        assertArrayEquals(new int[]{7, 17}, PlasmaGrid.neighbors(8));
        assertArrayEquals(new int[]{10, 0, 18}, PlasmaGrid.neighbors(9));
        assertArrayEquals(new int[]{43, 35}, PlasmaGrid.neighbors(44));
        assertEquals(9, PlasmaGrid.calculate(grid(8, 1, 9, 1)).cells().get(8).throughput());
        assertEquals(9, PlasmaGrid.calculate(grid(0, 1, 10, 1)).cells().get(0).throughput());
    }

    @Test void heatsinksReachEightNeighborsWithoutWrapping() {
        assertArrayEquals(new int[]{1, 9, 10}, PlasmaGrid.affectedSlots(0, PlasmaGrid.HEATSINK));
        assertArrayEquals(new int[]{7, 16, 17}, PlasmaGrid.affectedSlots(8, PlasmaGrid.HEATSINK));
        assertArrayEquals(new int[]{34, 35, 43}, PlasmaGrid.affectedSlots(44, PlasmaGrid.HEATSINK));
        assertArrayEquals(new int[]{0, 1, 2, 9, 11, 18, 19, 20}, PlasmaGrid.affectedSlots(10, PlasmaGrid.HEATSINK));
        assertArrayEquals(PlasmaGrid.neighbors(10), PlasmaGrid.affectedSlots(10, PlasmaGrid.INJECTOR));
        assertArrayEquals(PlasmaGrid.neighbors(10), PlasmaGrid.affectedSlots(10, PlasmaGrid.COIL));
        assertEquals(0, PlasmaGrid.affectedSlots(10, PlasmaGrid.EMPTY).length);
        var edge = PlasmaGrid.calculate(grid(8, 2, 9, 1));
        assertEquals(0, edge.cells().get(9).cooling());
    }

    @Test void eightNeighborCoolingSharesOneCapacityAndDiagonalInterferenceClamps() {
        int[] components = grid(10, PlasmaGrid.HEATSINK);
        for (int slot : PlasmaGrid.affectedSlots(10, PlasmaGrid.HEATSINK)) components[slot] = PlasmaGrid.INJECTOR;
        var result = PlasmaGrid.calculate(components);
        for (int slot : PlasmaGrid.affectedSlots(10, PlasmaGrid.HEATSINK))
            assertEquals(1.5, result.cells().get(slot).cooling());
        var diagonal = PlasmaGrid.calculate(grid(10, 3, 9, 1, 0, 2));
        assertEquals(0.8, diagonal.cells().get(10).efficiency());
        assertEquals(512_000, diagonal.outputPerTick());
        components = grid(10, PlasmaGrid.COIL, 9, PlasmaGrid.INJECTOR);
        for (int slot : PlasmaGrid.affectedSlots(10, PlasmaGrid.HEATSINK))
            if (slot != 9) components[slot] = PlasmaGrid.HEATSINK;
        result = PlasmaGrid.calculate(components);
        assertEquals(0, result.cells().get(10).efficiency());
        assertEquals(0, result.outputPerTick());
    }

    @Test void injectorBoostAndBackpressureIncreaseHeat() {
        var result = PlasmaGrid.calculate(grid(10, 1, 11, 1, 9, 3));
        assertEquals(11, result.cells().get(10).throughput());
        assertEquals(22, result.cells().get(10).heatGenerated());
        assertEquals(23, result.argonPerTick());
        assertEquals(640_000, result.outputPerTick());
    }

    @Test void heatsinkCoolingIsSharedAndUnusedCapacityIsLost() {
        var result = PlasmaGrid.calculate(grid(10, 2, 9, 1, 11, 1, 1, 1));
        assertEquals(4, result.cells().get(9).cooling());
        assertEquals(4, result.cells().get(11).cooling());
        assertEquals(4, result.cells().get(1).cooling());
        assertEquals(0, result.cells().get(19).cooling());
    }

    @Test void plasmaIsSplitAndCoilsSaturateWithoutDuplicatingIt() {
        var split = PlasmaGrid.calculate(grid(10, 1, 9, 3, 11, 3));
        assertEquals(7, split.argonPerTick());
        assertEquals(3.5, split.cells().get(9).plasmaReceived());
        assertEquals(560_000, split.outputPerTick());
        var saturated = PlasmaGrid.calculate(grid(10, 3, 9, 1, 11, 1));
        assertEquals(16, saturated.cells().get(10).plasmaReceived());
        assertEquals(8, saturated.cells().get(10).plasmaProcessed());
        assertEquals(640_000, saturated.outputPerTick());
    }

    @Test void heatsinksReduceCoilEfficiency() {
        var result = PlasmaGrid.calculate(grid(10, 3, 9, 1, 11, 2, 1, 2, 19, 2));
        assertEquals(0.4, result.cells().get(10).efficiency());
        assertEquals(256_000, result.outputPerTick());
    }

    @Test void eightSeparatedGroupsMatchReferenceAndStayCool() {
        int[] grid = new int[45];
        for (int row : new int[]{0, 2, 4}) for (int column : new int[]{0, 3, 6}) {
            if (row == 4 && column == 6) continue;
            int slot = row * 9 + column;
            grid[slot] = column == 3 ? 3 : 2; grid[slot + 1] = 1; grid[slot + 2] = column == 3 ? 2 : 3;
        }
        var result = PlasmaGrid.calculate(grid);
        assertEquals(64, result.argonPerTick());
        assertEquals(5_120_000, result.outputPerTick());
        double[] heat = new double[45];
        for (int tick = 0; tick < 1000; tick++) assertFalse(PlasmaGrid.advanceHeat(heat, result, true));
        assertEquals(0, java.util.Arrays.stream(heat).sum());
    }

    @Test void excessInjectionCostsFuelAndHeatWithoutMoreOutput() {
        var one = PlasmaGrid.calculate(grid(10, 3, 9, 1));
        var two = PlasmaGrid.calculate(grid(10, 3, 9, 1, 11, 1));
        assertEquals(one.outputPerTick(), two.outputPerTick());
        assertTrue(two.argonPerTick() > one.argonPerTick());
        double[] heat = new double[45];
        var unextracted = PlasmaGrid.calculate(grid(0, 1, 1, 1));
        assertEquals(0, unextracted.outputPerTick());
        for (int tick = 0; tick < 435; tick++) PlasmaGrid.advanceHeat(heat, unextracted, true);
        assertTrue(heat[0] >= PlasmaGrid.HEAT_LIMIT);
    }

    @Test void idleCoolingAndEditingKeepPositionHeat() {
        double[] heat = new double[45]; heat[10] = 100;
        PlasmaGrid.advanceHeat(heat, PlasmaGrid.calculate(grid(10, 1, 9, 2)), false);
        assertEquals(87, heat[10]);
        PlasmaGrid.advanceHeat(heat, PlasmaGrid.calculate(new int[45]), false);
        assertEquals(86, heat[10]);
    }
}
