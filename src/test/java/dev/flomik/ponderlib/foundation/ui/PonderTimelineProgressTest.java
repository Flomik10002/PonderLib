package dev.flomik.ponderlib.foundation.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The scrubber's fill is eased rather than snapped to the scene's raw progress (upstream uses
// LerpedFloat.chase(target, .5f, EXP)). The user reported the bar as "либо заполнен либо нет" - either
// full or empty, never moving - so these pin down that the easing genuinely produces intermediate
// values and converges, keeping that half of the complaint out of the realm of assumption. (The
// actually-visible cause turned out to be a z-ordering bug in renderTimeline, which hid the fill
// behind the frame's opaque background entirely.)
class PonderTimelineProgressTest {

    @Test
    void movesGraduallyTowardsTheTargetRatherThanJumpingToIt() {
        float value = 0F;
        float target = 1F;

        value = PonderUI.chaseTimelineProgress(value, target);
        assertTrue(value > 0F && value < target, "first step should land strictly between, got " + value);

        // Strictly increasing, and strictly below the target until it finally snaps.
        int steps = 1;
        float previous = value;
        while (value < target) {
            value = PonderUI.chaseTimelineProgress(value, target);
            assertTrue(value > previous, "should keep advancing, got " + value + " after " + previous);
            previous = value;
            steps++;
            assertTrue(steps < 100, "never converged");
        }
        // Several distinct intermediate positions, i.e. visibly animated rather than binary.
        assertTrue(steps >= 5, "expected a gradual ramp, converged in only " + steps + " steps");
    }

    @Test
    void convergesExactlyOntoTheTargetSoTheBarCanFillCompletely() {
        float value = 0F;
        for (int i = 0; i < 100; i++) {
            value = PonderUI.chaseTimelineProgress(value, 1F);
        }
        assertEquals(1F, value, 0F, "must land exactly on the target, not just near it");
    }

    @Test
    void easesBackDownAfterARewind() {
        // seekTo(0) drops the scene's progress to 0 while the bar is still full - it should ease down,
        // not stay stuck.
        float value = 1F;
        value = PonderUI.chaseTimelineProgress(value, 0F);
        assertTrue(value < 1F && value > 0F, "should ease down through intermediate values, got " + value);
    }

    @Test
    void holdsStillWhenAlreadyAtTheTarget() {
        assertEquals(0.5F, PonderUI.chaseTimelineProgress(0.5F, 0.5F), 0F);
    }
}
