package net.crystalnexus.energy;

/** Standalone check: compile with PlasmaGrid.java, then run with java -ea. */
public final class HighFlowPlasmaInjectorCheck {
    public static void main(String[] args) {
        int[] components = new int[PlasmaGrid.SIZE];
        components[10] = PlasmaGrid.INJECTOR;
        components[9] = components[11] = PlasmaGrid.COIL;
        var normal = PlasmaGrid.calculate(components);
        components[10] = PlasmaGrid.HIGH_FLOW_INJECTOR;
        var high = PlasmaGrid.calculate(components);
        assert high.argonPerTick() == 2 * normal.argonPerTick();
        assert high.cells().get(10).throughput() == 2 * normal.cells().get(10).throughput();
        assert high.cells().get(10).heatGenerated() == 2 * normal.cells().get(10).heatGenerated();
        assert high.outputPerTick() == 2 * normal.outputPerTick();
        assert java.util.Arrays.equals(PlasmaGrid.affectedSlots(10, PlasmaGrid.INJECTOR),
            PlasmaGrid.affectedSlots(10, PlasmaGrid.HIGH_FLOW_INJECTOR));

        components[1] = PlasmaGrid.INJECTOR;
        components[0] = PlasmaGrid.HEATSINK;
        high = PlasmaGrid.calculate(components);
        assert high.cells().get(10).throughput() == 10;
        assert high.cells().get(1).throughput() == 6;
        assert high.cells().get(10).cooling() == PlasmaGrid.HEATSINK_COOLING;
        assert high.cells().get(1).cooling() == PlasmaGrid.HEATSINK_COOLING;
        components[1] = PlasmaGrid.HIGH_FLOW_INJECTOR;
        high = PlasmaGrid.calculate(components);
        assert high.cells().get(1).throughput() == 12;
        assert high.cells().get(10).throughput() == 10;

        components = new int[PlasmaGrid.SIZE];
        components[10] = PlasmaGrid.COIL;
        components[9] = components[11] = PlasmaGrid.INJECTOR;
        normal = PlasmaGrid.calculate(components);
        components[9] = components[11] = PlasmaGrid.HIGH_FLOW_INJECTOR;
        high = PlasmaGrid.calculate(components);
        assert high.outputPerTick() == 2 * normal.outputPerTick();
        assert high.outputPerTick() == 1_280_000;
        assert high.argonPerTick() == 2 * normal.argonPerTick();
        assert high.cells().get(10).plasmaReceived() == 16;
        assert high.cells().get(10).plasmaProcessed() == PlasmaGrid.COIL_CAPACITY;
        assert high.outputPerTick() == PlasmaGrid.COIL_CAPACITY * PlasmaGrid.FE_PER_PLASMA;
        components[1] = components[19] = PlasmaGrid.HIGH_FLOW_INJECTOR;
        high = PlasmaGrid.calculate(components);
        assert high.cells().get(10).plasmaReceived() == 32;
        assert high.cells().get(10).plasmaProcessed() == PlasmaGrid.COIL_CAPACITY;
        assert high.outputPerTick() == 1_280_000;
        double[] heat = new double[PlasmaGrid.SIZE];
        PlasmaGrid.advanceHeat(heat, high, true);
        assert heat[9] > 0;
        double previous = heat[9];
        PlasmaGrid.advanceHeat(heat, high, false);
        assert heat[9] < previous;
        System.out.println("High Flow Plasma Injector checks passed");
    }
}
