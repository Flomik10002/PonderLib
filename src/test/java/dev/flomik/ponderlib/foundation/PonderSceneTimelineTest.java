package dev.flomik.ponderlib.foundation;
import dev.flomik.ponderlib.api.element.EntityElement;
import dev.flomik.ponderlib.api.scene.CollisionMode;
import dev.flomik.ponderlib.api.scene.Easing;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
class PonderSceneTimelineTest {
    @Test void parallelDurationAndRuntimeMatch(){PonderScene s=compile((b,u)->{b.overlay().showText(50,"Long");b.idle(20);});assertEquals(60,s.getTotalTime());assertEquals(60,s.measureRuntimeTicks(100));}
    @Test void completedDelayStillConsumesTheRestOfItsOwnTick(){PonderScene s=compile((b,u)->{b.idle(20);b.addInstruction(x->{});});assertEquals(20,s.getTotalTime());assertEquals(21,s.measureRuntimeTicks(100));}
    @Test void seekingReachesParallelTailPastFinishedMarker(){PonderScene s=compile((b,u)->{b.overlay().showText(50,"Long");b.idle(20);b.markAsFinished();});s.seekToTime(s.getTotalTime());assertEquals(60,s.getCurrentTime());}
    @Test void entityMovementUsesOneParallelDurationWithoutBlockingLaterInstructions(){PonderScene s=compile((b,u)->{b.world().moveEntity(new SimpleElementLink<>(EntityElement.class),new Vec3(1,1,1),24,Easing.QUAD_IN,CollisionMode.IGNORE);b.idle(5);});assertEquals(24,s.getTotalTime());assertEquals(24,s.measureRuntimeTicks(100));}
    private static PonderScene compile(dev.flomik.ponderlib.api.scene.PonderStoryBoard board){PonderScene s=new PonderScene(mock(PonderLevel.class));board.program(new PonderSceneBuilder(s),new SimpleSceneBuildingUtil());s.begin();return s;}
}
