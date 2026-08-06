package dev.flomik.ponderlib.foundation.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Pins down the two pure formulas behind the scene-switch slide (see PonderUI#tickLazyIndex/
// #slideOffset), the same way PonderTimelineProgressTest pins down the scrubber's easing - both are
// ported from upstream's LerpedFloat chase and its renderScene(int) offset formula respectively.
class PonderSceneSlideTest {

    @Test
    void lazyIndexMovesGraduallyTowardsTheTargetIndex() {
        float value = 0F;
        int target = 1;

        value = PonderUI.chaseLazyIndex(value, target);
        assertTrue(value > 0F && value < target, "first step should land strictly between, got " + value);

        int steps = 1;
        float previous = value;
        while (value < target) {
            value = PonderUI.chaseLazyIndex(value, target);
            assertTrue(value > previous, "should keep advancing, got " + value + " after " + previous);
            previous = value;
            steps++;
            assertTrue(steps < 100, "never converged");
        }
        assertTrue(steps >= 5, "expected a gradual slide, converged in only " + steps + " steps");
    }

    @Test
    void lazyIndexConvergesExactlyOntoTheTargetIndex() {
        float value = 0F;
        for (int i = 0; i < 100; i++) {
            value = PonderUI.chaseLazyIndex(value, 1);
        }
        assertEquals(1F, value, 0F, "must land exactly on the target index, not just near it");
    }

    @Test
    void lazyIndexHoldsStillWhenAlreadyAtTheTargetIndex() {
        assertEquals(2F, PonderUI.chaseLazyIndex(2F, 2), 0F);
    }

    @Test
    void slideOffsetIsZeroExactlyWhenTheSceneIsTheOneBeingChasedTo() {
        assertEquals(0.0, PonderUI.slideOffset(3, 3F), 0.0);
    }

    @Test
    void slideOffsetPointsTowardsTheSceneBeingLeftBehind() {
        // Paging forward from 0 to 1: while lazyIndexValue is still trailing behind at 0, scene 0 (the
        // one being left) should get pushed in the negative direction and scene 1 (the one being
        // chased to) in the positive direction - matching upstream's diff = i - lazyIndexValue sign.
        double leavingScene = PonderUI.slideOffset(0, 0.5F);
        double enteringScene = PonderUI.slideOffset(1, 0.5F);

        assertTrue(leavingScene < 0, "the scene being left should slide towards negative offset, got " + leavingScene);
        assertTrue(enteringScene > 0, "the scene being entered should slide towards positive offset, got " + enteringScene);
    }

    @Test
    void slideOffsetGrowsInMagnitudeAsTheSceneFallsFurtherBehind() {
        // Same upstream curve as Mth.lerp(diff * diff, 200, 600) * diff: the offset accelerates rather
        // than growing linearly, so a scene barely starting to slide moves less per unit of diff than
        // one already most of the way off screen.
        double atQuarter = Math.abs(PonderUI.slideOffset(1, 0.75F));
        double atHalf = Math.abs(PonderUI.slideOffset(1, 0.5F));
        double atFull = Math.abs(PonderUI.slideOffset(1, 0F));

        assertTrue(atQuarter < atHalf && atHalf < atFull,
            "offset magnitude should keep growing as the scene falls further behind, got "
                + atQuarter + ", " + atHalf + ", " + atFull);
    }
}
