package net.crystalnexus.util;

final class MachineAnimationState {
    private MachineAnimationState() {}

    static Decision decide(double progress, boolean active, boolean grace) {
        if (progress > 0) return new Decision(false, false);
        if (!active) return new Decision(true, false);
        return grace ? new Decision(true, false) : new Decision(false, true);
    }

    record Decision(boolean idle, boolean grace) {}
}
