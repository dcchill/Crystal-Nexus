package net.crystalnexus.block;

public final class ConveyerBeltRenderClock {
    private long observedMoveTime = Long.MIN_VALUE;
    private long moveStartTime = Long.MIN_VALUE;
    private long observedIncomingTime = Long.MIN_VALUE;
    private long incomingStartTime = Long.MIN_VALUE;

    public long startTime(int segment, long serverMoveTime, long serverIncomingTime, long clientTime) {
        if (observedMoveTime != serverMoveTime) {
            observedMoveTime = serverMoveTime;
            moveStartTime = clientTime;
        }
        if (segment == 0 && serverIncomingTime != Long.MIN_VALUE) {
            if (observedIncomingTime != serverIncomingTime) {
                observedIncomingTime = serverIncomingTime;
                incomingStartTime = clientTime;
            }
            return incomingStartTime;
        }
        return moveStartTime;
    }
}
