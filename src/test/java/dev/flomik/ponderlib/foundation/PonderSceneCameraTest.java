package dev.flomik.ponderlib.foundation;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// PonderSceneParticles#render relies on PonderSceneCamera never being attached to a real
// position (setup() is never called on it) - Particle#renderRotatedQuad subtracts the camera's
// position from a particle's own scene-local coordinates, and only comes out as plain local
// coordinates (what the scene's own pose matrix, already pushed into the GL modelview stack,
// expects) if that subtraction is a no-op. Locks that assumption down directly, and that
// set(...) doesn't accidentally change it (Camera has no position-mutating logic in setRotation,
// but there's no harm double-checking the one invariant this whole rendering path depends on).
class PonderSceneCameraTest {

    @Test
    void positionStaysAtOriginBeforeSet() {
        PonderSceneCamera camera = new PonderSceneCamera();
        assertEquals(Vec3.ZERO, camera.getPosition());
    }

    @Test
    void positionStaysAtOriginAfterSet() {
        PonderSceneCamera camera = new PonderSceneCamera();
        camera.set(35F, 235F);
        assertEquals(Vec3.ZERO, camera.getPosition());
    }
}
