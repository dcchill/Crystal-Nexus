package net.crystalnexus.block;

public final class ConveyerBeltRenderClock {
    private double serverOffset = Double.NaN;

    public double startTime(int segment, double serverMoveTime, double serverIncomingTime, long clientTime) {
        // Keep one clock offset so packet jitter does not restart each movement.
        double offset = clientTime - serverMoveTime;
        serverOffset = Double.isNaN(serverOffset) ? offset : Math.min(serverOffset, offset);
        return ConveyerBeltMovement.renderStartTime(segment, serverMoveTime, serverIncomingTime) + serverOffset;
    }
}
