package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.foundation.SimpleSelection;
import dev.flomik.ponderlib.foundation.element.WorldSectionElementImpl;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

// Regression coverage for a real bug: PonderSceneBuilder's first cut of hideSection/
// hideIndependentSection resolved the target element up front and then called addInstruction(...)
// from INSIDE an already-running instruction's tick callback - which appends to PonderScene's master
// schedule list, not the activeSchedule list PonderScene#tick() is actually iterating, so the fade
// silently never ran during that playthrough. Resolving lazily in firstTick (mirroring
// AnimateElementInstruction) is what actually fixes it - these tests exercise the instruction
// directly, the same level AnimateElementInstructionTest/RevealSectionInstructionTest already do.
class HideSectionInstructionTest {

    @Test
    void fadesFromOneToZeroOverExactlyItsConfiguredDurationThenHides() {
        WorldSectionElementImpl element = new WorldSectionElementImpl(new SimpleSelection(List.of()));
        element.setVisible(true);
        element.setFade(1F);
        PonderScene scene = mock(PonderScene.class);

        HideSectionInstruction instruction = new HideSectionInstruction(s -> element, Direction.UP, 2);

        instruction.tick(scene);
        assertTrue(element.isVisible());
        assertTrue(element.getFade() > 0 && element.getFade() < 1);

        instruction.tick(scene);
        assertEquals(0F, element.getFade());
        assertFalse(element.isVisible());
        assertTrue(instruction.isComplete());
    }

    @Test
    void resolvesTheTargetLazilyOnFirstTickNotAtConstruction() {
        WorldSectionElementImpl element = new WorldSectionElementImpl(new SimpleSelection(List.of()));
        element.setVisible(true);
        element.setFade(1F);
        PonderScene scene = mock(PonderScene.class);
        List<PonderScene> resolverCalls = new java.util.ArrayList<>();

        HideSectionInstruction instruction = new HideSectionInstruction(s -> {
            resolverCalls.add(s);
            return element;
        }, Direction.DOWN, 3);

        assertTrue(resolverCalls.isEmpty(), "constructing the instruction must not resolve anything yet");
        instruction.tick(scene);
        assertEquals(List.of(scene), resolverCalls, "the resolver runs exactly once, on the first tick");
    }

    @Test
    void doesNothingForItsWholeDurationWhenNothingResolves() {
        PonderScene scene = mock(PonderScene.class);
        HideSectionInstruction instruction = new HideSectionInstruction(s -> null, Direction.UP, 2);

        instruction.tick(scene);
        instruction.tick(scene);

        assertTrue(instruction.isComplete(), "a resolver returning null still lets the instruction complete normally");
    }

    @Test
    void isNonBlockingSoTheTimelineKeepsAdvancingWhileFadingOut() {
        WorldSectionElementImpl element = new WorldSectionElementImpl(new SimpleSelection(List.of()));
        HideSectionInstruction instruction = new HideSectionInstruction(s -> element, Direction.UP, 5);

        assertFalse(instruction.isBlocking());
    }

    @Test
    void clampsZeroOrNegativeFadeTicksToOneToAvoidDividingByZero() {
        WorldSectionElementImpl element = new WorldSectionElementImpl(new SimpleSelection(List.of()));
        element.setVisible(true);
        PonderScene scene = mock(PonderScene.class);

        HideSectionInstruction instruction = new HideSectionInstruction(s -> element, Direction.UP, 0);
        instruction.tick(scene);

        assertEquals(0F, element.getFade());
        assertFalse(element.isVisible());
        assertTrue(instruction.isComplete());
    }
}
