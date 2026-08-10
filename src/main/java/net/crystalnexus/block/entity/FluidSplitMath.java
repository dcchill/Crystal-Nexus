package net.crystalnexus.block.entity;

final class FluidSplitMath {
    private FluidSplitMath() {
    }

    static int[] fairShares(int available, int[] capacities, int startIndex) {
        int[] shares = new int[capacities.length];
        boolean[] active = new boolean[capacities.length];
        int remaining = Math.max(0, available);
        int activeCount = 0;
        for (int i = 0; i < capacities.length; i++) {
            if (capacities[i] > 0) {
                active[i] = true;
                activeCount++;
            }
        }

        while (remaining > 0 && activeCount > 0) {
            int evenShare = remaining / activeCount;
            boolean saturated = false;
            for (int i = 0; i < capacities.length; i++) {
                if (active[i] && capacities[i] <= evenShare) {
                    shares[i] = capacities[i];
                    remaining -= capacities[i];
                    active[i] = false;
                    activeCount--;
                    saturated = true;
                }
            }
            if (saturated) continue;

            for (int i = 0; i < capacities.length; i++) {
                if (active[i]) shares[i] = evenShare;
            }
            remaining -= evenShare * activeCount;
            for (int offset = 0; offset < capacities.length && remaining > 0; offset++) {
                int index = Math.floorMod(startIndex + offset, capacities.length);
                if (active[index]) {
                    shares[index]++;
                    remaining--;
                }
            }
            break;
        }
        return shares;
    }
}
