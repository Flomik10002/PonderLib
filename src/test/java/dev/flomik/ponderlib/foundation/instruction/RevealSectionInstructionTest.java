package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.api.element.WorldSectionElement;
import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.foundation.SimpleElementLink;
import dev.flomik.ponderlib.foundation.SimpleSelection;
import dev.flomik.ponderlib.foundation.element.WorldSectionElementImpl;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

// An empty selection keeps element.capture(level) a no-op on tick (see WorldSectionElementImpl),
// so PonderScene can be safely mocked here the same way AnimateElementInstructionTest does.
class RevealSectionInstructionTest {

    @Test
    void fadesFromZeroToOneOverExactlyItsConfiguredDurationThenStaysAtOne() {
        WorldSectionElementImpl element = new WorldSectionElementImpl(new SimpleSelection(List.of()));
        SimpleElementLink<WorldSectionElement> link = new SimpleElementLink<>(WorldSectionElement.class);
        PonderScene scene = mock(PonderScene.class);

        RevealSectionInstruction instruction = new RevealSectionInstruction(
            element, link, Direction.UP, 2, new Vec3(0.5, 0.5, 0.5), 0, 1, 0, 1
        );

        instruction.tick(scene);
        assertTrue(element.isVisible());
        assertTrue(element.getFade() > 0 && element.getFade() < 1);

        instruction.tick(scene);
        assertEquals(1.0F, element.getFade());
        assertTrue(instruction.isComplete());
    }

    @Test
    void linksAddsAndPinsTheFootprintOnTheFirstTick() {
        WorldSectionElementImpl element = new WorldSectionElementImpl(new SimpleSelection(List.of()));
        SimpleElementLink<WorldSectionElement> link = new SimpleElementLink<>(WorldSectionElement.class);
        PonderScene scene = mock(PonderScene.class);

        RevealSectionInstruction instruction = new RevealSectionInstruction(
            element, link, Direction.DOWN, 5, new Vec3(1, 1, 1), 0, 2, 0, 2
        );

        instruction.tick(scene);

        verify(scene).addElement(element);
        verify(scene).linkElement(element, link);
        verify(scene).setFocusPoint(new Vec3(1, 1, 1));
        verify(scene).setBasePlateBounds(0, 2, 0, 2);
    }

    @Test
    void isNonBlockingSoTheTimelineKeepsAdvancingWhileFading() {
        WorldSectionElementImpl element = new WorldSectionElementImpl(new SimpleSelection(List.of()));
        SimpleElementLink<WorldSectionElement> link = new SimpleElementLink<>(WorldSectionElement.class);

        RevealSectionInstruction instruction = new RevealSectionInstruction(
            element, link, null, 10, Vec3.ZERO, 0, 1, 0, 1
        );

        assertFalse(instruction.isBlocking());
    }

    // Regression test: PonderScene#begin() reuses the same WorldSectionElementImpl instance when
    // replaying after a rewind (see seekToTime), and its firstTick used to leave animatedRotation/
    // animatedOffset at whatever they were left at the end of the previous playthrough - so a
    // replayed rotate/move would compute its target from an already-animated pose instead of
    // neutral, making the section jump to a wrong pose and animate further from there.
    @Test
    void firstTickResetsAnyLeftoverRotationOrOffsetFromAPreviousPlaythrough() {
        WorldSectionElementImpl element = new WorldSectionElementImpl(new SimpleSelection(List.of()));
        element.setAnimatedRotation(new Vec3(0, 360, 0));
        element.setAnimatedOffset(new Vec3(1, 0, 0));
        SimpleElementLink<WorldSectionElement> link = new SimpleElementLink<>(WorldSectionElement.class);
        PonderScene scene = mock(PonderScene.class);

        RevealSectionInstruction instruction = new RevealSectionInstruction(
            element, link, Direction.UP, 5, Vec3.ZERO, 0, 1, 0, 1
        );
        instruction.tick(scene);

        assertEquals(Vec3.ZERO, element.getAnimatedRotation());
        assertEquals(Vec3.ZERO, element.getAnimatedOffset());
    }

    @Test
    void clampsZeroOrNegativeFadeTicksToOneToAvoidDividingByZero() {
        WorldSectionElementImpl element = new WorldSectionElementImpl(new SimpleSelection(List.of()));
        SimpleElementLink<WorldSectionElement> link = new SimpleElementLink<>(WorldSectionElement.class);
        PonderScene scene = mock(PonderScene.class);

        RevealSectionInstruction instruction = new RevealSectionInstruction(
            element, link, Direction.UP, 0, Vec3.ZERO, 0, 1, 0, 1
        );

        instruction.tick(scene);

        assertEquals(1.0F, element.getFade());
        assertTrue(instruction.isComplete());
    }
}
