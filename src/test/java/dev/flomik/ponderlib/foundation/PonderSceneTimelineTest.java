package dev.flomik.ponderlib.foundation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

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

    private static PonderScene compile(dev.flomik.ponderlib.api.scene.PonderStoryBoard board) {
        PonderScene scene = new PonderScene(mock(PonderLevel.class));
        PonderSceneBuilder builder = new PonderSceneBuilder(scene);
        board.program(builder, new SimpleSceneBuildingUtil());
        scene.begin();
        return scene;
    }
}
