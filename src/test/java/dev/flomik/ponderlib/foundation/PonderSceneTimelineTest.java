package dev.flomik.ponderlib.foundation;

import dev.flomik.ponderlib.api.scene.CollisionMode;
import dev.flomik.ponderlib.api.scene.Easing;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PonderSceneTimelineTest {
    @Test
    void nonBlockingInstructionThatOutlivesIdleExtendsTimelineExactly() {
        PonderScene scene = compile((builder, util) -> {
            builder.overlay().showText(50, "Long explanation");
            builder.idle(20);
        });

        // TextInstruction includes its 5-tick entrance and exit around the authored 50 ticks.
        assertEquals(60, scene.getTotalTime());
        assertEquals(60, scene.measureRuntimeTicks(100));
    }

    @Test
    void completedBlockingInstructionDoesNotCreateAPhantomTailTick() {
        PonderScene scene = compile((builder, util) -> {
            builder.idle(20);
            builder.addInstruction(ignored -> { });
        });

        assertEquals(20, scene.getTotalTime());
        assertEquals(20, scene.measureRuntimeTicks(100));
    }

    @Test
    void seekingCanReachParallelTailPastFinishedMarker() {
        PonderScene scene = compile((builder, util) -> {
            builder.overlay().showText(50, "Long explanation");
            builder.idle(20);
            builder.markAsFinished();
        });
        scene.seekToTime(scene.getTotalTime());
        assertEquals(60, scene.getCurrentTime());
    }

    @Test
    void nativeEntityMovementRunsInParallelAndExtendsTheTimelineToItsOwnDuration() {
        PonderLevel level = mock(PonderLevel.class);
        Entity entity = mock(Entity.class);
        when(entity.position()).thenReturn(Vec3.ZERO);
        when(entity.getDeltaMovement()).thenReturn(Vec3.ZERO);
        PonderScene scene = new PonderScene(level);
        PonderSceneBuilder builder = new PonderSceneBuilder(scene);
        var link = builder.world().createEntity(ignored -> entity);
        builder.world().moveEntity(link, new Vec3(6, 0, 0), 6, Easing.LINEAR, CollisionMode.IGNORE);
        builder.idle(2);
        scene.begin();

        assertEquals(6, scene.getTotalTime(), "non-blocking movement should outlive the shorter idle");
        assertEquals(6, scene.measureRuntimeTicks(20));
    }

    private static PonderScene compile(dev.flomik.ponderlib.api.scene.PonderStoryBoard board) {
        PonderScene scene = new PonderScene(mock(PonderLevel.class));
        PonderSceneBuilder builder = new PonderSceneBuilder(scene);
        board.program(builder, new SimpleSceneBuildingUtil());
        scene.begin();
        return scene;
    }
}
