package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.foundation.PonderScene;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KeyframeInstructionTest {

    @Test
    void isAlwaysCompleteAndNonBlockingSoItNeverHoldsUpTheSchedule() {
        assertTrue(KeyframeInstruction.IMMEDIATE.isComplete());
        assertFalse(KeyframeInstruction.IMMEDIATE.isBlocking());
    }

    @Test
    void immediateMarksAKeyframeAtExactlyTheCurrentSceneTime() {
        PonderScene scene = mock(PonderScene.class);

        KeyframeInstruction.IMMEDIATE.onScheduled(scene);

        verify(scene).markKeyframe(0);
    }

    @Test
    void delayedMarksAKeyframeSixTicksLater() {
        PonderScene scene = mock(PonderScene.class);

        KeyframeInstruction.DELAYED.onScheduled(scene);

        verify(scene).markKeyframe(6);
    }
}
