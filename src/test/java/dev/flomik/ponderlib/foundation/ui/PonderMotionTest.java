package dev.flomik.ponderlib.foundation.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PonderMotionTest {

    @Test
    void backEchoShiftClampsItsEndpointsAndMovesMonotonicallyTowardsRest() {
        assertEquals(-8, PonderStreak.backEchoShift(-1F));
        assertEquals(-8, PonderStreak.backEchoShift(0F));
        assertEquals(0, PonderStreak.backEchoShift(1F));
        assertEquals(0, PonderStreak.backEchoShift(2F));

        int previous = PonderStreak.backEchoShift(0F);
        for (int step = 1; step <= 100; step++) {
            int current = PonderStreak.backEchoShift(step / 100F);
            assertTrue(current >= previous,
                "echo must never move away from rest: " + previous + " -> " + current);
            previous = current;
        }
    }

    @Test
    void controlEntranceHonoursDelayClampsEndpointsAndRisesMonotonically() {
        int delay = 3;
        assertEquals(0F, PonderButton.controlEntrance(-1, delay), 0F);
        assertEquals(0F, PonderButton.controlEntrance(delay, delay), 0F);
        assertEquals(1F, PonderButton.controlEntrance(delay + 6, delay), 0F);
        assertEquals(1F, PonderButton.controlEntrance(delay + 20, delay), 0F);
        assertEquals(PonderButton.controlEntrance(2, 0),
            PonderButton.controlEntrance(2, -4), 0F, "negative delays clamp to zero");

        float previous = PonderButton.controlEntrance(0, delay);
        for (int age = 1; age <= delay + 10; age++) {
            float current = PonderButton.controlEntrance(age, delay);
            assertTrue(current >= previous,
                "entrance must be monotonic at age " + age + ": " + previous + " -> " + current);
            previous = current;
        }
    }

    @Test
    void entranceFadeCanResumeAtScreenAge() {
        assertEquals(0F, PonderButton.entranceFadeAfterTicks(-3));
        assertEquals(0F, PonderButton.entranceFadeAfterTicks(0));
        assertEquals(.1F, PonderButton.entranceFadeAfterTicks(1), 1E-6F);
        assertTrue(PonderButton.entranceFadeAfterTicks(20) > PonderButton.entranceFadeAfterTicks(10));
        assertTrue(PonderButton.entranceFadeAfterTicks(100) > .999F);
    }

    @Test
    void shortcutGlyphAlwaysFitsTheInnerButtonWidth() {
        assertEquals(1F, PonderButton.shortcutScale(0), 0F);
        assertEquals(1F, PonderButton.shortcutScale(10), 0F);
        assertEquals(.5F, PonderButton.shortcutScale(20), 0F);

        for (int width = 1; width <= 200; width++) {
            assertTrue(width * PonderButton.shortcutScale(width) <= 10.0001F);
        }
    }

    @Test
    void controlPressDecaysThroughIntermediateValuesAndSnapsToZero() {
        assertEquals(0F, PonderButton.decayControlPress(0F), 0F);
        assertEquals(.55F, PonderButton.decayControlPress(1F), 1E-6F);

        float value = 1F;
        for (int step = 0; step < 100 && value > 0F; step++) {
            float next = PonderButton.decayControlPress(value);
            assertTrue(next >= 0F && next < value,
                "press must decay without undershooting: " + value + " -> " + next);
            value = next;
        }
        assertEquals(0F, value, 0F, "press decay must eventually snap exactly to rest");
    }

    @Test
    void keyframeMotionApproachesEitherTargetAndSnapsExactly() {
        assertEquals(.35F, PonderUI.chaseKeyframeMotion(0F, 1F), 1E-6F);
        assertEquals(.65F, PonderUI.chaseKeyframeMotion(1F, 0F), 1E-6F);
        assertEquals(1F, PonderUI.chaseKeyframeMotion(.999F, 1F), 0F);
        assertEquals(0F, PonderUI.chaseKeyframeMotion(.001F, 0F), 0F);

        float rising = 0F;
        float falling = 1F;
        for (int step = 0; step < 100 && (rising < 1F || falling > 0F); step++) {
            float nextRising = PonderUI.chaseKeyframeMotion(rising, 1F);
            float nextFalling = PonderUI.chaseKeyframeMotion(falling, 0F);
            assertTrue(nextRising >= rising && nextRising <= 1F);
            assertTrue(nextFalling <= falling && nextFalling >= 0F);
            rising = nextRising;
            falling = nextFalling;
        }
        assertEquals(1F, rising, 0F);
        assertEquals(0F, falling, 0F);
    }

    @Test
    void keyframeProximityHasSymmetricLinearEndpoints() {
        assertEquals(1F, PonderUI.keyframeProximity(40, 40), 0F);
        assertEquals(.5F, PonderUI.keyframeProximity(36, 40), 0F);
        assertEquals(.5F, PonderUI.keyframeProximity(44, 40), 0F);
        assertEquals(0F, PonderUI.keyframeProximity(32, 40), 0F);
        assertEquals(0F, PonderUI.keyframeProximity(48, 40), 0F);
        assertEquals(0F, PonderUI.keyframeProximity(60, 40), 0F);

        for (int distance = 0; distance <= 12; distance++) {
            assertEquals(PonderUI.keyframeProximity(40 - distance, 40),
                PonderUI.keyframeProximity(40 + distance, 40), 0F);
        }
    }

    @Test
    void keyframeActiveStripUnfoldsContinuouslyFromTopToBottom() {
        assertEquals(5, PonderUI.keyframeActiveBottom(0F),
            "inactive mark must stay attached to the timeline");
        assertEquals(11, PonderUI.keyframeActiveBottom(.5F));
        assertEquals(17, PonderUI.keyframeActiveBottom(1F));

        int previousBottom = PonderUI.keyframeActiveBottom(0F);
        for (int step = 1; step <= 10; step++) {
            int bottom = PonderUI.keyframeActiveBottom(step / 10F);
            assertTrue(bottom >= previousBottom, "active strip must only extend downward");
            previousBottom = bottom;
        }

        assertEquals(0F, PonderUI.keyframeChevronMotion(0F), 0F);
        assertEquals(0F, PonderUI.keyframeChevronMotion(.5F), 0F);
        assertEquals(1F, PonderUI.keyframeChevronMotion(1F), 0F);
    }
}
