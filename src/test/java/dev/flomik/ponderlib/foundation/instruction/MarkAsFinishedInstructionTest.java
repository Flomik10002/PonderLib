package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.foundation.PonderScene;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MarkAsFinishedInstructionTest {

    @Test
    void isAlwaysCompleteAndNonBlockingSoItNeverHoldsUpTheSchedule() {
        MarkAsFinishedInstruction instruction = new MarkAsFinishedInstruction();

        assertTrue(instruction.isComplete());
        assertFalse(instruction.isBlocking());
    }

    @Test
    void tickingItMarksTheSceneAsFinished() {
        MarkAsFinishedInstruction instruction = new MarkAsFinishedInstruction();
        PonderScene scene = mock(PonderScene.class);

        instruction.tick(scene);

        verify(scene).setFinished(true);
    }
}
