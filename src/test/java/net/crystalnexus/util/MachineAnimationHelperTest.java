package net.crystalnexus.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MachineAnimationHelperTest {
    @Test void keepsContinuousOperationsActiveAndThenSettlesIdle() {
        MachineAnimationState.Decision betweenJobs = MachineAnimationState.decide(0, true, false);
        assertFalse(betweenJobs.idle());
        assertTrue(betweenJobs.grace());

        MachineAnimationState.Decision nextJob = MachineAnimationState.decide(1, true, betweenJobs.grace());
        assertFalse(nextJob.idle());
        assertFalse(nextJob.grace());

        assertTrue(MachineAnimationState.decide(0, true, true).idle());
        assertTrue(MachineAnimationState.decide(0, false, false).idle());
    }
}
