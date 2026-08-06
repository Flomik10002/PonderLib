package dev.flomik.ponderlib.foundation;

import net.minecraft.client.Camera;

/**
 * A {@link Camera} that's never actually attached to a level/entity ({@code position} stays
 * {@code Vec3.ZERO}, {@code setup()} is never called) - it exists purely to hold a {@code
 * rotation()} quaternion for {@code SingleQuadParticle.FacingCameraMode.LOOKAT_XYZ} to copy onto a
 * particle's own quad, so real vanilla particles billboard to face this scene's fixed isometric
 * viewpoint instead of whatever the real game camera happens to be doing. {@code position() ==
 * Vec3.ZERO} also means {@code SingleQuadParticle#renderRotatedQuad} subtracts nothing from the
 * particle's own scene-local coordinates, so the emitted vertices come out as plain local
 * coordinates - exactly what {@link PonderSceneParticles#render} needs, since it pushes this
 * scene's own pose matrix into {@code RenderSystem.getModelViewStack()} itself rather than
 * transforming each vertex by hand the way this codebase's other custom quads (shadow/flash/
 * outline) do.
 */
public class PonderSceneCamera extends Camera {

    /**
     * Matches the same two rotations, in the same order, that {@code
     * foundation.ui.PonderUI#applySceneTransform} applies with its own fixed {@code
     * CAMERA_X_ROTATION}/{@code CAMERA_Y_ROTATION} constants - call this with {@code
     * set(-CAMERA_X_ROTATION, CAMERA_Y_ROTATION + 180)} so a billboarded particle's quad faces the
     * same fixed isometric viewpoint the rest of the scene renders from. The extra mirror/scale/
     * focus translation {@code applySceneTransform} also applies needs no separate compensation
     * here - those are uniform transforms applied to every vertex equally after this rotation
     * already runs, including a correctly billboarded particle's.
     */
    public void set(float xRotation, float yRotation) {
        setRotation(yRotation, xRotation);
    }
}
