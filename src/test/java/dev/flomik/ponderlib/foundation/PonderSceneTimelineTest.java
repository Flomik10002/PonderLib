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
    void completedBlockingInstructionStillConsumesTheRestOfItsOwnTick() {
        PonderScene scene = compile((builder, util) -> {
            builder.idle(20);
            builder.addInstruction(ignored -> { });
        });

        // getTotalTime() is a schedule-time estimate (see PonderInstruction#onScheduled) and is
        // unaffected by this - only the actual tick-by-tick walk in PonderScene#tick() changed.
        // A blocking instruction that completes on its final tick used to let the next instruction
        // in the schedule also tick that same call (see idle(n) previously not truly blocking for
        // n ticks); now the walk always stops once a blocking instruction has been ticked, whether
        // or not it just finished, so the following instruction starts one real tick later.
        assertEquals(20, scene.getTotalTime());
        assertEquals(21, scene.measureRuntimeTicks(100));
    }

    /**
     * Regression guard: pre-fix, each 1-tick idle() completed on its own first tick() call and the
     * walk in {@link PonderScene#tick()} would continue() straight into the next one within that
     * same call, collapsing this whole chain into a single real tick instead of four - a shape a
     * future, well-meaning cleanup could easily reintroduce without this pinned down.
     */
    @Test
    void chainedOneTickDelaysEachConsumeTheirOwnRealTick() {
        PonderScene scene = compile((builder, util) -> {
            builder.idle(1);
            builder.idle(1);
            builder.idle(1);
            builder.idle(1);
        });

        assertEquals(4, scene.getTotalTime());
        assertEquals(4, scene.measureRuntimeTicks(100));
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
