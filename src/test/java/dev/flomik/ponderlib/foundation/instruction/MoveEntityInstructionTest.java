package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.api.element.EntityElement;
import dev.flomik.ponderlib.api.scene.CollisionMode;
import dev.flomik.ponderlib.api.scene.Easing;
import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.foundation.SimpleElementLink;
import dev.flomik.ponderlib.foundation.element.EntityElementImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MoveEntityInstructionTest {

    @Test
    void ignoreModeFollowsAbsoluteQuadraticProgressAndSnapsExactlyToTarget() {
        Vec3 start = new Vec3(2, 3, 4);
        Vec3 target = new Vec3(10, 7, 0);
        Vec3 originalVelocity = new Vec3(0.2, -0.1, 0.3);
        Entity entity = mock(Entity.class);
        when(entity.position()).thenReturn(start);
        when(entity.getDeltaMovement()).thenReturn(originalVelocity);
        when(entity.isNoGravity()).thenReturn(false);
        entity.noPhysics = false;
        TestFixture fixture = fixture(entity, target, 4, Easing.QUAD_IN, CollisionMode.IGNORE);

        fixture.instruction.tick(fixture.scene);
        assertTrue(entity.noPhysics, "IGNORE should bypass collision while the instruction is active");
        entity.noPhysics = false; // Simulate entity tick/mod code changing it between samples.
        fixture.instruction.tick(fixture.scene);
        assertTrue(entity.noPhysics, "IGNORE should reassert noPhysics after every entity tick");
        fixture.instruction.tick(fixture.scene);
        fixture.instruction.tick(fixture.scene);

        ArgumentCaptor<Vec3> positions = ArgumentCaptor.forClass(Vec3.class);
        verify(entity, times(4)).setPos(positions.capture());
        assertEquals(List.of(
            start.lerp(target, 0.0625),
            start.lerp(target, 0.25),
            start.lerp(target, 0.5625),
            target
        ), positions.getAllValues());
        assertTrue(fixture.instruction.isComplete());
        assertFalse(entity.noPhysics, "the original collision flag must be restored at completion");
        verify(entity, times(4)).setNoGravity(true);
        verify(entity).setNoGravity(false);
        verify(entity, times(4)).setDeltaMovement(Vec3.ZERO);
        verify(entity).setDeltaMovement(originalVelocity);
        verify(entity, never()).move(any(), any());
    }

    @Test
    void respectModeUsesCurrentPositionToRequestTheAbsoluteDesiredDeltaWithoutSnappingThroughBlocks() {
        AtomicReference<Vec3> position = new AtomicReference<>(Vec3.ZERO);
        Entity entity = mock(Entity.class);
        when(entity.position()).thenAnswer(ignored -> position.get());
        when(entity.getDeltaMovement()).thenReturn(Vec3.ZERO);
        when(entity.isNoGravity()).thenReturn(true);
        entity.noPhysics = true;
        doAnswer(invocation -> {
            Vec3 requested = invocation.getArgument(1);
            // Simulate a collision that permits only one block of X travel per tick.
            position.updateAndGet(current -> current.add(Math.min(requested.x, 1.0), requested.y, requested.z));
            return null;
        }).when(entity).move(eq(MoverType.SELF), any(Vec3.class));
        TestFixture fixture = fixture(entity, new Vec3(8, 0, 0), 2, Easing.LINEAR, CollisionMode.RESPECT);

        fixture.instruction.tick(fixture.scene);
        assertFalse(entity.noPhysics, "RESPECT must enable vanilla collision while movement is active");
        fixture.instruction.tick(fixture.scene);

        ArgumentCaptor<Vec3> deltas = ArgumentCaptor.forClass(Vec3.class);
        verify(entity, times(2)).move(eq(MoverType.SELF), deltas.capture());
        assertEquals(List.of(new Vec3(4, 0, 0), new Vec3(7, 0, 0)), deltas.getAllValues(),
            "the second delta should aim at absolute eased position 8 from the blocked position 1");
        assertEquals(new Vec3(2, 0, 0), position.get(), "RESPECT must not teleport to the target when blocked");
        verify(entity, never()).setPos(any(Vec3.class));
        assertTrue(entity.noPhysics, "the entity's original noPhysics=true state must be restored");
        // Twice while moving plus the original true value restored at completion.
        verify(entity, times(3)).setNoGravity(true);
    }

    @Test
    void nonPositiveDurationIsClampedToOneTick() {
        Entity entity = mockEntityAt(new Vec3(1, 1, 1));
        Vec3 target = new Vec3(5, 6, 7);
        TestFixture fixture = fixture(entity, target, 0, Easing.QUAD_IN, CollisionMode.IGNORE);

        fixture.instruction.tick(fixture.scene);

        assertTrue(fixture.instruction.isComplete());
        verify(entity).setPos(target);
    }

    @Test
    void customEasingReceivesNormalizedAbsoluteProgress() {
        Entity entity = mockEntityAt(Vec3.ZERO);
        TestFixture fixture = fixture(entity, new Vec3(10, 0, 0), 2,
            progress -> progress * progress * progress, CollisionMode.IGNORE);

        fixture.instruction.tick(fixture.scene);

        verify(entity).setPos(new Vec3(1.25, 0, 0));
    }

    @Test
    void capturesStartAndPhysicsStateOnFirstTickRatherThanConstruction() {
        AtomicReference<Vec3> position = new AtomicReference<>(Vec3.ZERO);
        Entity entity = mock(Entity.class);
        when(entity.position()).thenAnswer(ignored -> position.get());
        when(entity.getDeltaMovement()).thenReturn(Vec3.ZERO);
        when(entity.isNoGravity()).thenReturn(false);
        TestFixture fixture = fixture(entity, new Vec3(9, 0, 0), 2, Easing.LINEAR, CollisionMode.IGNORE);
        verify(entity, never()).position();

        position.set(new Vec3(5, 0, 0));
        fixture.instruction.tick(fixture.scene);

        verify(entity).setPos(new Vec3(7, 0, 0));
    }

    @Test
    void finalSampleUsesTheRequestedTargetEvenWhenCustomEasingHasAWrongEndpoint() {
        Entity entity = mockEntityAt(Vec3.ZERO);
        Vec3 target = new Vec3(10, 0, 0);
        TestFixture fixture = fixture(entity, target, 2, ignored -> 0.25, CollisionMode.IGNORE);

        fixture.instruction.tick(fixture.scene);
        fixture.instruction.tick(fixture.scene);

        ArgumentCaptor<Vec3> positions = ArgumentCaptor.forClass(Vec3.class);
        verify(entity, times(2)).setPos(positions.capture());
        assertEquals(List.of(new Vec3(2.5, 0, 0), target), positions.getAllValues());
    }

    @Test
    void unresolvedEntityLinkStillConsumesItsConfiguredDurationWithoutSideEffects() {
        SimpleElementLink<EntityElement> link = new SimpleElementLink<>(EntityElement.class);
        PonderScene scene = mock(PonderScene.class);
        when(scene.resolve(link)).thenReturn(null);
        MoveEntityInstruction instruction = new MoveEntityInstruction(
            link, Vec3.ZERO, 2, Easing.LINEAR, CollisionMode.IGNORE);

        instruction.tick(scene);
        assertFalse(instruction.isComplete());
        instruction.tick(scene);
        assertTrue(instruction.isComplete());
    }

    @Test
    void resettingAnInterruptedInstructionRestoresEntityState() {
        Vec3 originalVelocity = new Vec3(0.1, 0.2, 0.3);
        Entity entity = mock(Entity.class);
        when(entity.position()).thenReturn(Vec3.ZERO);
        when(entity.getDeltaMovement()).thenReturn(originalVelocity);
        when(entity.isNoGravity()).thenReturn(false);
        entity.noPhysics = false;
        TestFixture fixture = fixture(entity, new Vec3(5, 0, 0), 5, Easing.LINEAR, CollisionMode.IGNORE);

        fixture.instruction.tick(fixture.scene);
        fixture.instruction.reset(fixture.scene);

        assertFalse(entity.noPhysics);
        verify(entity).setNoGravity(false);
        verify(entity).setDeltaMovement(originalVelocity);
        assertFalse(fixture.instruction.isComplete(), "reset should make the full duration runnable again");
    }

    private static Entity mockEntityAt(Vec3 position) {
        Entity entity = mock(Entity.class);
        when(entity.position()).thenReturn(position);
        when(entity.getDeltaMovement()).thenReturn(Vec3.ZERO);
        when(entity.isNoGravity()).thenReturn(false);
        return entity;
    }

    private static TestFixture fixture(Entity entity, Vec3 target, int ticks, Easing easing,
                                       CollisionMode collisionMode) {
        SimpleElementLink<EntityElement> link = new SimpleElementLink<>(EntityElement.class);
        PonderScene scene = mock(PonderScene.class);
        when(scene.resolve(link)).thenReturn(new EntityElementImpl(entity));
        return new TestFixture(scene, new MoveEntityInstruction(link, target, ticks, easing, collisionMode));
    }

    private record TestFixture(PonderScene scene, MoveEntityInstruction instruction) {
    }
}
