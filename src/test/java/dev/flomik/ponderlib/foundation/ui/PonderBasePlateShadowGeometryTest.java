package dev.flomik.ponderlib.foundation.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// PonderUI.forEachPerimeterSide is pure matrix bookkeeping around the base plate, ported from
// upstream Ponder's "kool shadow fx" loop, and its rotation sign is genuinely easy to get backwards:
// the enclosing scaling(1,-1,1) flip means it isn't obvious by inspection whether rotating -90 about
// Y walks around the plate or off the side of it. Getting it wrong would put the shadow/flash walls
// somewhere outside the footprint entirely - invisible in a unit test unless the landing points are
// asserted, and not something the author of this code could see for himself. So: run the real loop
// and check where each side's own local coordinates actually land in scene block space.
//
// Frame contract being verified (see PerimeterSideRenderer#draw): origin at the side's FAR end, +X
// running back along the side, +Y DOWN, -Z pointing out of the plate.
class PonderBasePlateShadowGeometryTest {

    private record Side(Vector4f farEnd, Vector4f nearEnd, Vector4f oneDown, Vector4f oneOutward) {
    }

    private static List<Side> walk(double minX, double maxX, double minZ, double maxZ) {
        List<Side> sides = new ArrayList<>();
        PoseStack poseStack = new PoseStack();
        PonderUI.forEachPerimeterSide(poseStack, minX, maxX, minZ, maxZ, (pose, span) -> sides.add(new Side(
            transform(pose, 0, 0, 0),
            transform(pose, -span, 0, 0),
            transform(pose, 0, 1, 0),
            transform(pose, 0, 0, -1)
        )));
        return sides;
    }

    private static Vector4f transform(PoseStack pose, float x, float y, float z) {
        return new Vector4f(x, y, z, 1F).mul(pose.last().pose());
    }

    private static void assertAt(double x, double y, double z, Vector4f actual, String what) {
        assertEquals(x, actual.x(), 1e-4, what + " x");
        assertEquals(y, actual.y(), 1e-4, what + " y");
        assertEquals(z, actual.z(), 1e-4, what + " z");
    }

    @Test
    void tracesExactlyTheFourEdgesOfASquareFootprint() {
        List<Side> sides = walk(0, 3, 0, 3);

        assertEquals(4, sides.size());
        // Each side runs from its far end back to its near end; together they close the square
        // (0,0) -> (3,0) -> (3,3) -> (0,3) -> back, i.e. every corner is hit exactly twice.
        assertAt(3, 0, 0, sides.get(0).farEnd(), "side 0 far end");
        assertAt(0, 0, 0, sides.get(0).nearEnd(), "side 0 near end");

        assertAt(3, 0, 3, sides.get(1).farEnd(), "side 1 far end");
        assertAt(3, 0, 0, sides.get(1).nearEnd(), "side 1 near end");

        assertAt(0, 0, 3, sides.get(2).farEnd(), "side 2 far end");
        assertAt(3, 0, 3, sides.get(2).nearEnd(), "side 2 near end");

        assertAt(0, 0, 0, sides.get(3).farEnd(), "side 3 far end");
        assertAt(0, 0, 3, sides.get(3).nearEnd(), "side 3 near end");
    }

    @Test
    void localPlusYIsDownwardInSceneSpaceOnEverySide() {
        // The shadow skirt is drawn at local y 0..4 and must hang BELOW the plate, and the flash at
        // local y -1..0 must rise ABOVE it - both rely on this one sign, which comes from the second
        // scaling(1,-1,1) flip inside the loop.
        for (Side side : walk(0, 3, 0, 3)) {
            assertEquals(side.farEnd().y() - 1, side.oneDown().y(), 1e-4, "local +Y must go down 1 block");
        }
    }

    @Test
    void localMinusZPointsAwayFromThePlateCentreOnEverySide() {
        // Upstream offsets the flash 1/1024 along local -Z and the shadow 1/1024 the other way, so
        // this axis has to genuinely point out of the plate for that split to mean what it means.
        double centreX = 1.5;
        double centreZ = 1.5;
        for (Side side : walk(0, 3, 0, 3)) {
            Vector4f mid = side.farEnd();
            double outwardX = side.oneOutward().x() - mid.x();
            double outwardZ = side.oneOutward().z() - mid.z();
            // Dot the outward step with the vector from the plate centre to this side: same sign means
            // it points away from the middle.
            double toSideX = mid.x() - centreX;
            double toSideZ = mid.z() - centreZ;
            assertEquals(true, outwardX * toSideX + outwardZ * toSideZ > 0,
                "local -Z should point away from the plate centre");
        }
    }

    @Test
    void alternatesSpansSoANonSquareFootprintStillCloses() {
        List<Side> sides = walk(2, 7, 1, 3);

        assertAt(7, 0, 1, sides.get(0).farEnd(), "side 0 far end");
        assertAt(2, 0, 1, sides.get(0).nearEnd(), "side 0 near end");

        assertAt(7, 0, 3, sides.get(1).farEnd(), "side 1 far end");
        assertAt(7, 0, 1, sides.get(1).nearEnd(), "side 1 near end");

        assertAt(2, 0, 3, sides.get(2).farEnd(), "side 2 far end");
        assertAt(7, 0, 3, sides.get(2).nearEnd(), "side 2 near end");

        assertAt(2, 0, 1, sides.get(3).farEnd(), "side 3 far end");
        assertAt(2, 0, 3, sides.get(3).nearEnd(), "side 3 near end");
    }
}
