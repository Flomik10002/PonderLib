package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.api.element.WorldSectionElement;
import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.foundation.SimpleElementLink;
import dev.flomik.ponderlib.foundation.SimpleSelection;
import dev.flomik.ponderlib.foundation.element.WorldSectionElementImpl;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// PonderScene has no public constructor - real usage only builds one via Minecraft.getInstance()
// .level, which isn't available either in a bare JUnit run or in a dedicated-server GameTest.
// Mocking it instead lets this test exercise the instruction's actual tick()/firstTick() logic.
class AnimateElementInstructionTest {

    @Test
    void animatesLinearlyAndSnapsExactlyToTargetOnTheLastTick() {
        WorldSectionElementImpl element = new WorldSectionElementImpl(new SimpleSelection(List.of()));
        SimpleElementLink<WorldSectionElement> link = new SimpleElementLink<>(WorldSectionElement.class);
        PonderScene scene = mock(PonderScene.class);
        when(scene.resolve(link)).thenReturn(element);

        AnimateElementInstruction<WorldSectionElement> instruction = new AnimateElementInstruction<>(
            link, new Vec3(0, 90, 0), 3, WorldSectionElement::setAnimatedRotation, WorldSectionElement::getAnimatedRotation
        );

        instruction.tick(scene);
        assertEquals(new Vec3(0, 30, 0), element.getAnimatedRotation());

        instruction.tick(scene);
        assertEquals(new Vec3(0, 60, 0), element.getAnimatedRotation());

        instruction.tick(scene);
        assertEquals(new Vec3(0, 90, 0), element.getAnimatedRotation());
        assertEquals(true, instruction.isComplete());
    }

    @Test
    void clampsZeroOrNegativeTicksToOneToAvoidDividingByZero() {
        WorldSectionElementImpl element = new WorldSectionElementImpl(new SimpleSelection(List.of()));
        SimpleElementLink<WorldSectionElement> link = new SimpleElementLink<>(WorldSectionElement.class);
        PonderScene scene = mock(PonderScene.class);
        when(scene.resolve(link)).thenReturn(element);

        AnimateElementInstruction<WorldSectionElement> instruction = new AnimateElementInstruction<>(
            link, new Vec3(1, 0, 0), 0, WorldSectionElement::setAnimatedOffset, WorldSectionElement::getAnimatedOffset
        );

        instruction.tick(scene);
        assertEquals(new Vec3(1, 0, 0), element.getAnimatedOffset());
        assertEquals(true, instruction.isComplete());
    }

    @Test
    void doesNothingIfTheLinkedElementIsNoLongerResolvable() {
        SimpleElementLink<WorldSectionElement> link = new SimpleElementLink<>(WorldSectionElement.class);
        PonderScene scene = mock(PonderScene.class);
        when(scene.resolve(link)).thenReturn(null);

        AnimateElementInstruction<WorldSectionElement> instruction = new AnimateElementInstruction<>(
            link, new Vec3(0, 90, 0), 2, WorldSectionElement::setAnimatedRotation, WorldSectionElement::getAnimatedRotation
        );

        instruction.tick(scene);
        instruction.tick(scene);
        assertEquals(true, instruction.isComplete());
    }
}
