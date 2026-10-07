package net.crystalnexus.energy;

/** Deterministic component interactions with eight-neighbor heatsinks; plasma is never stored. */
public final class PlasmaGrid {
    public static final int ROWS = 5, COLUMNS = 9, SIZE = ROWS * COLUMNS;
    public static final int EMPTY = 0, INJECTOR = 1, HEATSINK = 2, COIL = 3, HIGH_FLOW_INJECTOR = 4;
    public static final int BASE_THROUGHPUT = 9, INJECTOR_BOOST = 3, COIL_BACKPRESSURE = 1;
    public static final int HEATSINK_COOLING = 12, COIL_CAPACITY = 16, FE_PER_PLASMA = 80_000;
    public static final int HEATSINK_INTERFERENCE_PERCENT = 20;
    public static final double PASSIVE_COOLING = 1, PASSIVE_COOLING_FRACTION = 0.01;
    public static final double HEAT_PER_FE = 0.0001, HEAT_LIMIT = 10_000;

    private PlasmaGrid() {}

    public static boolean isInjector(int component) {
        return component == INJECTOR || component == HIGH_FLOW_INJECTOR;
    }

    public record Cell(int component, double throughput, double heatGenerated, double cooling,
                       double plasmaReceived, double plasmaProcessed, double efficiency,
                       double extractionMultiplier) {}
    public record Result(double argonPerTick, int outputPerTick, java.util.List<Cell> cells) {}

    public static int[] neighbors(int slot) {
        if (slot < 0 || slot >= SIZE) throw new IllegalArgumentException("Invalid grid slot");
        return java.util.stream.IntStream.of(slot % COLUMNS > 0 ? slot - 1 : -1,
            slot % COLUMNS < COLUMNS - 1 ? slot + 1 : -1,
            slot >= COLUMNS ? slot - COLUMNS : -1,
            slot < SIZE - COLUMNS ? slot + COLUMNS : -1).filter(i -> i >= 0).toArray();
    }

    /** Potential affected positions, shared by simulation and GUI hover previews. */
    public static int[] affectedSlots(int slot, int component) {
        if (slot < 0 || slot >= SIZE) throw new IllegalArgumentException("Invalid grid slot");
        if (component == EMPTY) return new int[0];
        if (component != HEATSINK) return neighbors(slot);
        int[] slots = new int[8];
        int size = 0, row = slot / COLUMNS, column = slot % COLUMNS;
        for (int r = Math.max(0, row - 1); r <= Math.min(ROWS - 1, row + 1); r++)
            for (int c = Math.max(0, column - 1); c <= Math.min(COLUMNS - 1, column + 1); c++)
                if (r != row || c != column) slots[size++] = r * COLUMNS + c;
        return java.util.Arrays.copyOf(slots, size);
    }

    private static int count(int[] grid, int[] neighbors, int component) {
        int count = 0;
        for (int neighbor : neighbors)
            if (component == INJECTOR ? isInjector(grid[neighbor]) : grid[neighbor] == component) count++;
        return count;
    }

    public static Result calculate(int[] grid) {
        if (grid.length != SIZE) throw new IllegalArgumentException("Expected 45 components");
        double[] throughput = new double[SIZE];
        double[] heat = new double[SIZE], cooling = new double[SIZE], incoming = new double[SIZE];
        double[] extractionWeight = new double[SIZE], injectorCoils = new double[SIZE];
        double argon = 0;
        for (int slot = 0; slot < SIZE; slot++) {
            if (grid[slot] < EMPTY || grid[slot] > HIGH_FLOW_INJECTOR) throw new IllegalArgumentException("Invalid component");
            int[] neighbors = affectedSlots(slot, grid[slot]);
            if (isInjector(grid[slot])) {
                int injectors = count(grid, neighbors, INJECTOR), coils = count(grid, neighbors, COIL);
                throughput[slot] = (BASE_THROUGHPUT + INJECTOR_BOOST * injectors - COIL_BACKPRESSURE * coils) / 2.0;
                if (grid[slot] == HIGH_FLOW_INJECTOR) throughput[slot] *= 2;
                injectorCoils[slot] = coils;
                argon += throughput[slot];
                heat[slot] = throughput[slot] * (1 + injectors);
                if (coils > 0) for (int neighbor : neighbors) if (grid[neighbor] == COIL) {
                    double share = throughput[slot] / coils;
                    incoming[neighbor] += share;
                    extractionWeight[neighbor] += share * coils;
                }
            } else if (grid[slot] == HEATSINK) {
                for (int neighbor : neighbors)
                    if (isInjector(grid[neighbor])) cooling[neighbor] += HEATSINK_COOLING;
            }
        }
        var cells = new java.util.ArrayList<Cell>(SIZE);
        double output = 0;
        for (int slot = 0; slot < SIZE; slot++) {
            double processed = Math.min(COIL_CAPACITY, incoming[slot]);
            double efficiency = grid[slot] == COIL ? Math.max(0, 100 - HEATSINK_INTERFERENCE_PERCENT * count(grid, affectedSlots(slot, HEATSINK), HEATSINK)) / 100.0 : 0;
            double extractionMultiplier = grid[slot] == COIL && incoming[slot] > 0
                ? extractionWeight[slot] / incoming[slot] : injectorCoils[slot];
            output += processed * extractionMultiplier * efficiency * FE_PER_PLASMA;
            if (grid[slot] == COIL && incoming[slot] > 0) {
                double processedFraction = processed / incoming[slot];
                for (int neighbor : neighbors(slot)) if (isInjector(grid[neighbor])) {
                    heat[neighbor] += throughput[neighbor] * processedFraction * efficiency * FE_PER_PLASMA * HEAT_PER_FE;
                }
            }
            cells.add(new Cell(grid[slot], throughput[slot], heat[slot], cooling[slot], incoming[slot], processed, efficiency, extractionMultiplier));
        }
        return new Result(argon, (int) Math.floor(output), java.util.List.copyOf(cells));
    }

    /** Heat remains attached to positions, even after their components are removed. */
    public static boolean advanceHeat(double[] heat, Result grid, boolean injecting) {
        if (heat.length != SIZE) throw new IllegalArgumentException("Expected 45 heat values");
        boolean rupture = false;
        for (int slot = 0; slot < SIZE; slot++) {
            Cell cell = grid.cells().get(slot);
            double passiveCooling = PASSIVE_COOLING + heat[slot] * PASSIVE_COOLING_FRACTION;
            heat[slot] = Math.max(0, heat[slot] + (injecting ? cell.heatGenerated() : 0) - cell.cooling() - passiveCooling);
            rupture |= heat[slot] >= HEAT_LIMIT;
        }
        return rupture;
    }
}
