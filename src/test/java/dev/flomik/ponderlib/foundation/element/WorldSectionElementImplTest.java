package dev.flomik.ponderlib.foundation.element;

import dev.flomik.ponderlib.foundation.SimpleSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// getSectionTransform() is pure math (no Minecraft/capture() dependency) - used both by render()
// and by foundation.ui.PonderUI's identify-mode hover picking (forward-projects block centers
// through this same matrix), so it's worth locking its behavior down directly.
class WorldSectionElementImplTest {

    private static final double EPSILON = 1e-4;

    @Test
    void isIdentityWhenNothingIsAnimated() {
        WorldSectionElementImpl element = new WorldSectionElementImpl(
            new SimpleSelection(List.of(BlockPos.ZERO, new BlockPos(2, 0, 0)))
        );

        assertTransforms(element.getSectionTransform(), new Vec3(3, 1, 1), new Vec3(3, 1, 1));
    }

    @Test
    void animatedOffsetTranslatesEveryPointByExactlyThatMuchRegardlessOfSelectionCenter() {
        WorldSectionElementImpl element = new WorldSectionElementImpl(
            new SimpleSelection(List.of(BlockPos.ZERO, new BlockPos(2, 0, 0)))
        );
        element.setAnimatedOffset(new Vec3(1, 2, 3));

        assertTransforms(element.getSectionTransform(), new Vec3(0, 0, 0), new Vec3(1, 2, 3));
    }

    @Test
    void animatedRotationTurnsTheSectionAroundItsSelectionCenter() {
        WorldSectionElementImpl element = new WorldSectionElementImpl(
            new SimpleSelection(List.of(BlockPos.ZERO, new BlockPos(2, 0, 0)))
        );
        element.setAnimatedRotation(new Vec3(0, 180, 0));
        Vec3 center = new Vec3(1.5, 0.5, 0.5);

        // The pivot itself doesn't move...
        assertTransforms(element.getSectionTransform(), center, center);
        // ...but a point one block away along X ends up one block away on the opposite side.
        assertTransforms(element.getSectionTransform(), center.add(1, 0, 0), center.subtract(1, 0, 0));
    }

    private static void assertTransforms(Matrix4f transform, Vec3 input, Vec3 expected) {
        Vector4f result = new Vector4f((float) input.x, (float) input.y, (float) input.z, 1F).mul(transform);
        assertEquals(expected.x, result.x(), EPSILON);
        assertEquals(expected.y, result.y(), EPSILON);
        assertEquals(expected.z, result.z(), EPSILON);
    }
}
