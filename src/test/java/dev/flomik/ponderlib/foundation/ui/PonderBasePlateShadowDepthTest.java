package dev.flomik.ponderlib.foundation.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the user-reported bug this shadow has caused twice ("какая-то белесая тень на боковой части
 * блока" / a pale smear climbing up a perimeter block's visible side face), but against the CURRENT
 * geometry rather than the one that caused it.
 * <p>
 * The original hand-rolled shadow was a flat quad lying on the ground at y=0.02 and extending
 * outward, i.e. occupying the exact same 3D points as the bottom of every perimeter block's own
 * outward-facing wall (walls span y=0..1). That is genuinely coincident geometry, which depth-tests
 * unpredictably - hence the smear - and it needed an invented {@code SHADOW_WALL_GAP} to push it
 * clear. The ported upstream geometry can't hit that failure mode at all: the skirt is a VERTICAL
 * plane spanning local y {@code 0..4}, and with local +Y pointing down it lives entirely at or below
 * the plate's base, never inside the y range where block walls exist. That's what this asserts -
 * a structural property, not a depth-ordering coincidence that happens to work out.
 */
class PonderBasePlateShadowDepthTest {

    // ChestFloorStoryboard's real layout: a 3x3 floor of blocks at y=0 (so walls span y=0..1).
    private static final double MIN_X = 0, MAX_X = 3, MIN_Z = 0, MAX_Z = 3;
    private static final double WALL_TOP = 1;

    @Test
    void theShadowSkirtNeverReachesUpIntoTheBlockWallsAboveThePlateBase() {
        PoseStack poseStack = new PoseStack();
        PonderUI.forEachPerimeterSide(poseStack, MIN_X, MAX_X, MIN_Z, MAX_Z, (pose, span) -> {
            // The shadow quad's own local corners, exactly as renderBasePlateShadowAndFlash emits
            // them: x 0..-span, y 0 (plate edge) .. 4 (fading end).
            for (float localY : new float[]{0F, 1F, 2F, 4F}) {
                for (float localX : new float[]{0F, -span}) {
                    double sceneY = transform(pose, localX, localY).y();
                    assertTrue(sceneY <= 1e-4,
                        "shadow corner at local y=" + localY + " landed at scene y=" + sceneY
                            + ", i.e. above the plate base - it can smear over a block's side face there");
                }
            }
        });
    }

    @Test
    void theFinishingFlashInsteadRisesFromThePlateBaseIntoTheWallItHugs() {
        PoseStack poseStack = new PoseStack();
        PonderUI.forEachPerimeterSide(poseStack, MIN_X, MAX_X, MIN_Z, MAX_Z, (pose, span) -> {
            // The flash quad spans local y -1..0 - the opposite direction, deliberately: it's a glow
            // rising up the plate's own edge, so it SHOULD overlap the wall's height range.
            double atBase = transform(pose, 0F, 0F).y();
            double atTop = transform(pose, 0F, -1F).y();
            assertTrue(Math.abs(atBase) < 1e-4, "flash should start exactly at the plate base, got " + atBase);
            assertTrue(atTop > 0 && atTop <= WALL_TOP + 1e-4,
                "flash should rise above the base without overshooting one block, got " + atTop);
        });
    }

    private static Vector4f transform(PoseStack pose, float x, float y) {
        return new Vector4f(x, y, 0F, 1F).mul(pose.last().pose());
    }
}
