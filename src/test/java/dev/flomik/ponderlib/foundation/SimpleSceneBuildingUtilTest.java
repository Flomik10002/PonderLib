package dev.flomik.ponderlib.foundation;

import dev.flomik.ponderlib.api.scene.SceneBuildingUtil;
import dev.flomik.ponderlib.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Ported formulas, checked against a known scene size the same way PonderBasePlateShadowGeometryTest
// checks the shadow/flash geometry - real Ponder's own SelectionUtil derives every helper here from the
// scene's OWN schematic size (see SimpleSceneBuildingUtil's javadoc for why that's the real answer to
// "how does their floor work" - there is no separate floor system, just this).
class SimpleSceneBuildingUtilTest {

    // A 5(x) x 3(y) x 4(z) scene, matching how PonderScene#compile(StoryBoardEntry) wires the real
    // schematic size in.
    private static final SceneBuildingUtil UTIL = new SimpleSceneBuildingUtil(new Vec3i(5, 3, 4));

    @Test
    void layerSpansTheFullXZExtentAtExactlyOneHeight() {
        List<BlockPos> positions = toList(UTIL.select().layer(1));

        assertEquals(5 * 4, positions.size());
        assertEquals(1, positions.get(0).getY());
        for (BlockPos pos : positions) {
            assertEquals(1, pos.getY());
        }
    }

    @Test
    void everywhereCoversTheEntireDeclaredSize() {
        List<BlockPos> positions = toList(UTIL.select().everywhere());

        assertEquals(5 * 3 * 4, positions.size());
    }

    @Test
    void layersFromGoesFromTheGivenHeightToTheTop() {
        List<BlockPos> positions = toList(UTIL.select().layersFrom(1));

        // Scene height is 3 (y = 0, 1, 2); from y=1 that is 2 layers of 5x4.
        assertEquals(5 * 4 * 2, positions.size());
    }

    @Test
    void layersCapsAtTheScenesOwnHeightEvenWhenAskedForMore() {
        // Asking for 100 layers from y=0 in a scene only 3 tall must not run off the end.
        List<BlockPos> positions = toList(UTIL.select().layers(0, 100));

        assertEquals(5 * 3 * 4, positions.size());
    }

    @Test
    void columnStartsAtYOneAndIsAsTallAsTheSceneMeasuredFromThere() {
        // Ported as-is from upstream's own literal formula (see SelectionUtil#column's javadoc), not
        // reinterpreted into what might seem more sensible: y=0 is treated as the floor a column stands
        // ON, and the column's HEIGHT is the scene's own Y size (3, here) counted again starting from
        // y=1 - so it reaches y=1..4, one block PAST this scene's real top (y=2), not "up to the top".
        // First found by a test written with the "obviously correct" cap assumption, which upstream's
        // own arithmetic does not actually make - Selection is pure position math, independent of
        // whether anything real occupies a position, so overshooting costs nothing in practice.
        List<BlockPos> positions = toList(UTIL.select().column(2, 2));

        assertEquals(4, positions.size(), "column height is the scene's Y size again, from y=1 - not capped at the scene's real top");
        for (BlockPos pos : positions) {
            assertEquals(2, pos.getX());
            assertEquals(2, pos.getZ());
        }
        assertEquals(1, positions.stream().mapToInt(BlockPos::getY).min().orElseThrow());
        assertEquals(4, positions.stream().mapToInt(BlockPos::getY).max().orElseThrow());
    }

    @Test
    void fromToWorksRegardlessOfWhichCornerComesFirst() {
        Selection forward = UTIL.select().fromTo(0, 0, 0, 1, 0, 1);
        Selection backward = UTIL.select().fromTo(1, 0, 1, 0, 0, 0);

        assertEquals(toList(forward), toList(backward));
        assertEquals(4, toList(forward).size());
    }

    @Test
    void positionsPreservesArgumentOrderUnlikeEveryOtherBuilder() {
        BlockPos a = new BlockPos(3, 0, 0);
        BlockPos b = new BlockPos(0, 0, 3);

        assertEquals(List.of(a, b), toList(UTIL.select().positions(a, b)));
    }

    @Test
    void vectorCenterOfIsHalfABlockAboveTheOrigin() {
        assertEquals(new Vec3(1.5, 0.5, 2.5), UTIL.vector().centerOf(1, 0, 2));
    }

    @Test
    void vectorTopOfSitsExactlyOnTheUpperFace() {
        assertEquals(new Vec3(0.5, 1.0, 0.5), UTIL.vector().topOf(0, 0, 0));
    }

    @Test
    void vectorBlockSurfaceMarginPushesFurtherAlongTheFacesNormal() {
        Vec3 onFace = UTIL.vector().blockSurface(BlockPos.ZERO, Direction.SOUTH, 0);
        Vec3 pastFace = UTIL.vector().blockSurface(BlockPos.ZERO, Direction.SOUTH, 0.5F);

        assertEquals(1.0, onFace.z(), 1e-9);
        assertEquals(1.5, pastFace.z(), 1e-9);
    }

    @Test
    void gridAtAndZeroBuildPlainBlockPositions() {
        assertEquals(new BlockPos(1, 2, 3), UTIL.grid().at(1, 2, 3));
        assertEquals(BlockPos.ZERO, UTIL.grid().zero());
    }

    private static List<BlockPos> toList(Iterable<BlockPos> positions) {
        List<BlockPos> result = new ArrayList<>();
        positions.forEach(result::add);
        return result;
    }
}
