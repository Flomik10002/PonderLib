package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.foundation.element.TextWindowElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

// TextInstruction became a FadeInOutInstruction when the text window was ported from upstream, so its
// contract changed on purpose: the caller's `duration` is now the FULLY VISIBLE time, and the
// instruction quietly runs for duration + 2 * FADE_TIME to ease in and out around it (upstream does
// exactly this). These assertions pin that down, including the fade curve, since the leader line's
// grow-out animation is driven by the same value.
class TextInstructionTest {

    private static final int DURATION = 4;

    @Test
    void isNonBlockingAndRunsForTheDurationPlusBothFades() {
        TextInstruction instruction = new TextInstruction(new TextWindowElement(), DURATION);

        assertFalse(instruction.isBlocking());

        PonderScene scene = mock(PonderScene.class);
        int ticked = 0;
        while (!instruction.isComplete()) {
            instruction.tick(scene);
            ticked++;
            assertTrue(ticked <= 100, "instruction never completed");
        }
        assertEquals(DURATION + 2 * FadeInOutInstruction.FADE_TIME, ticked);
    }

    @Test
    void addsAndShowsItsElementOnTheFirstTickThenHidesItOnceComplete() {
        TextWindowElement element = new TextWindowElement();
        PonderScene scene = mock(PonderScene.class);
        TextInstruction instruction = new TextInstruction(element, DURATION);

        instruction.tick(scene);
        verify(scene).addElement(element);
        assertTrue(element.isVisible());
        assertFalse(instruction.isComplete());

        while (!instruction.isComplete()) {
            instruction.tick(scene);
        }
        assertFalse(element.isVisible());
    }

    @Test
    void easesTheFadeInHoldsItThenEasesOut() {
        TextWindowElement element = new TextWindowElement();
        PonderScene scene = mock(PonderScene.class);
        TextInstruction instruction = new TextInstruction(element, DURATION);

        // The very first tick both shows the element AND already applies the first step of the ramp
        // (firstTick's applyFade(0) is overwritten later in that same tick) - so a window never appears
        // at full opacity for a frame, but it doesn't sit at a dead 0 either. Upstream behaves the same
        // way; the exact value is the squared ramp, (1/FADE_TIME)^2.
        instruction.tick(scene);
        float step = 1F / FadeInOutInstruction.FADE_TIME;
        assertEquals(step * step, element.getFade(), 1e-6);

        // Rises monotonically until it reaches full opacity, and gets there within the fade window.
        int risingTicks = 1;
        float previous = element.getFade();
        while (element.getFade() < 1F) {
            instruction.tick(scene);
            assertTrue(element.getFade() > previous, "fade should keep rising while fading in");
            previous = element.getFade();
            risingTicks++;
        }
        assertEquals(FadeInOutInstruction.FADE_TIME, risingTicks);

        // Held at exactly 1 for the caller's requested duration.
        for (int i = 0; i < DURATION; i++) {
            instruction.tick(scene);
            assertEquals(1F, element.getFade(), 1e-6, "fade should be held at 1 during the duration");
        }

        // ...then back down, ending at exactly 0 on the tick it completes.
        previous = element.getFade();
        while (!instruction.isComplete()) {
            instruction.tick(scene);
            assertTrue(element.getFade() < previous, "fade should keep falling while fading out");
            previous = element.getFade();
        }
        assertEquals(0F, element.getFade(), 1e-6);
    }
}
