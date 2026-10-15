package net.crystalnexus.multiblock;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Shared deterministic geometry for the server's blocks and the client's descending meteor. */
public final class HeavenshatterMeteor {
    public static final int MIN_DIAMETER = 7, MAX_DIAMETER = 20, DURATION = 600, MAX_DURATION = 2400, MIN_DISTANCE = 1_000, MAX_DISTANCE = 10_000;

    public record Cell(int x, int y, int z, int material) {}

    private HeavenshatterMeteor() {}

    public static long energyCost(int diameter) {
        checkDiameter(diameter);
        return 100_000L * diameter * diameter * diameter;
    }

    public static int duration(int distance) {
        return DURATION + (MAX_DURATION - DURATION) * (Math.clamp(distance, MIN_DISTANCE, MAX_DISTANCE) - MIN_DISTANCE) / (MAX_DISTANCE - MIN_DISTANCE);
    }

    public static int bearingDistance(int a, int b) {
        int distance = Math.abs(a - b) % 360;
        return Math.min(distance, 360 - distance);
    }

    public static boolean aligned(int frequency, int bearing, int targetFrequency, int targetBearing) {
        return Math.abs(frequency - targetFrequency) <= 2 && bearingDistance(bearing, targetBearing) <= 5;
    }

    public static List<Cell> cells(int diameter, long seed, int materials) {
        checkDiameter(diameter);
        if (materials < 2 || materials > 5) throw new IllegalArgumentException("Meteor needs 2–5 materials");
        Random random = new Random(seed);
        List<Cell> cells = new ArrayList<>();
        double center = (diameter - 1) / 2.0;
        double radius = diameter / 2.0;
        for (int y = 0; y < diameter; y++) {
            double dy = y - center;
            double layerRadius = Math.sqrt(radius * radius - dy * dy) * (0.94 + random.nextDouble() * 0.06);
            for (int x = 0; x < diameter; x++) for (int z = 0; z < diameter; z++) {
                double dx = x - center, dz = z - center;
                if (dx * dx + dz * dz > layerRadius * layerRadius) continue;
                int material = random.nextDouble() < 0.5 ? 1 + random.nextInt(materials - 1) : 0;
                cells.add(new Cell(x, y, z, material));
            }
        }
        // Ensure every selected material is actually present, even with unlucky ore rolls.
        for (int i = 0; i < materials; i++) {
            Cell cell = cells.get(i);
            cells.set(i, new Cell(cell.x(), cell.y(), cell.z(), i));
        }
        return List.copyOf(cells);
    }

    public static List<Integer> materialCounts(int diameter, long seed, int materials) {
        int[] counts = new int[materials];
        for (Cell cell : cells(diameter, seed, materials)) counts[cell.material()]++;
        return java.util.Arrays.stream(counts).boxed().toList();
    }

    public static int weightedMaterial(List<Integer> weights, int roll) {
        if (weights.isEmpty() || weights.stream().anyMatch(weight -> weight <= 0)
            || roll < 0 || roll >= weights.stream().mapToInt(Integer::intValue).sum())
            throw new IllegalArgumentException("Invalid composition or roll");
        for (int i = 0; i < weights.size(); i++) {
            if (roll < weights.get(i)) return i;
            roll -= weights.get(i);
        }
        throw new AssertionError("Missing meteor material");
    }

    private static void checkDiameter(int diameter) {
        if (diameter < MIN_DIAMETER || diameter > MAX_DIAMETER) throw new IllegalArgumentException("Meteor diameter must be 7–20");
    }
}
