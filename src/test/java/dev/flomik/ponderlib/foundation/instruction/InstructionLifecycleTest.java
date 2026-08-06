package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.foundation.PonderScene;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class InstructionLifecycleTest {

    // Ported from real Ponder: PonderScene#getTotalTime() is built from onScheduled calls, not a
    // naive sum of every instruction's own duration - only blocking ones should extend it, since
    // non-blocking ones (fades/text/animation) run alongside whatever comes next instead of
    // holding up the schedule. Getting this wrong once already inflated the scrubber's range past
    // what seekToTime could ever reach - see docs/BUGS.md.
    @Test
    void onlyBlockingTickingInstructionsExtendTheScenesTotalTimeWhenScheduled() {
        PonderScene scene = mock(PonderScene.class);

        new DelayInstruction(20).onScheduled(scene);
        verify(scene).addToSceneTime(20);

        CountingInstruction nonBlocking = new CountingInstruction(50);
        nonBlocking.onScheduled(scene);
        verify(scene, never()).addToSceneTime(50);
    }

    @Test
    void markAsFinishedStopsTheSceneFromCountingAnyFurtherTimeWhenScheduled() {
        PonderScene scene = mock(PonderScene.class);

        new MarkAsFinishedInstruction().onScheduled(scene);

        verify(scene).stopCounting();
    }

    @Test
    void delayBlocksForExactlyItsConfiguredTicksAndCanReset() {
        DelayInstruction delay = new DelayInstruction(2);
        assertTrue(delay.isBlocking());
        assertFalse(delay.isComplete());

        delay.tick(null);
        assertFalse(delay.isComplete());
        delay.tick(null);
        assertTrue(delay.isComplete());

        delay.reset(null);
        assertFalse(delay.isComplete());
    }

    @Test
    void firstTickHookRunsOncePerLifecycle() {
        CountingInstruction instruction = new CountingInstruction(2);
        instruction.tick(null);
        instruction.tick(null);
        instruction.tick(null);
        assertEquals(1, instruction.firstTicks.get());

        instruction.reset(null);
        instruction.tick(null);
        assertEquals(2, instruction.firstTicks.get());
    }

    @Test
    void simpleInstructionRunsItsCallbackAndIsNonBlocking() {
        AtomicInteger calls = new AtomicInteger();
        PonderInstruction instruction = PonderInstruction.simple(scene -> calls.incrementAndGet());

        assertTrue(instruction.isComplete());
        assertFalse(instruction.isBlocking());
        instruction.tick(null);
        assertEquals(1, calls.get());
    }

    private static final class CountingInstruction extends TickingInstruction {
        private final AtomicInteger firstTicks = new AtomicInteger();

        private CountingInstruction(int ticks) {
            super(false, ticks);
        }

        @Override
        protected void firstTick(dev.flomik.ponderlib.foundation.PonderScene scene) {
            firstTicks.incrementAndGet();
        }
    }
}
