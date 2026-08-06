package dev.flomik.ponderlib.foundation;

import dev.flomik.ponderlib.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleSelectionTest {

    @Test
    void centerIsAverageOfBlockCenters() {
        SimpleSelection selection = new SimpleSelection(List.of(
            new BlockPos(-2, 1, 4),
            new BlockPos(4, 3, -2)
        ));

        assertEquals(new Vec3(1.5, 2.5, 1.5), selection.getCenter());
    }

    @Test
    void emptySelectionHasCanonicalHalfBlockCenter() {
        assertEquals(new Vec3(0.5, 0.5, 0.5), new SimpleSelection(List.of()).getCenter());
    }

    @Test
    void constructorDefensivelyCopiesInput() {
        List<BlockPos> positions = new ArrayList<>();
        positions.add(BlockPos.ZERO);
        SimpleSelection selection = new SimpleSelection(positions);
        positions.add(new BlockPos(1, 1, 1));

        assertEquals(List.of(BlockPos.ZERO), toList(selection));
    }

    @Test
    void deduplicatesRepeatedPositions() {
        // A Selection is conceptually a SET (matches upstream) - add()/subtract() rely on this.
        SimpleSelection selection = new SimpleSelection(List.of(BlockPos.ZERO, BlockPos.ZERO, new BlockPos(1, 0, 0)));

        assertEquals(2, toList(selection).size());
    }

    @Test
    void buildingUtilPreservesVarargOrder() {
        BlockPos first = new BlockPos(3, 2, 1);
        BlockPos second = new BlockPos(-1, 0, 7);

        assertEquals(List.of(first, second), toList(new SimpleSceneBuildingUtil().select().positions(first, second)));
    }

    @Test
    void addUnionsPositionsWithoutMutatingEitherInput() {
        Selection a = new SimpleSelection(List.of(BlockPos.ZERO, new BlockPos(1, 0, 0)));
        Selection b = new SimpleSelection(List.of(new BlockPos(1, 0, 0), new BlockPos(2, 0, 0)));

        Selection union = a.add(b);

        assertEquals(List.of(BlockPos.ZERO, new BlockPos(1, 0, 0), new BlockPos(2, 0, 0)), toList(union));
        assertEquals(2, toList(a).size(), "a must be left unchanged");
        assertEquals(2, toList(b).size(), "b must be left unchanged");
    }

    @Test
    void subtractRemovesOnlyThePositionsPresentInTheOtherSelection() {
        Selection a = new SimpleSelection(List.of(BlockPos.ZERO, new BlockPos(1, 0, 0), new BlockPos(2, 0, 0)));
        Selection b = new SimpleSelection(List.of(new BlockPos(1, 0, 0)));

        Selection remainder = a.subtract(b);

        assertEquals(List.of(BlockPos.ZERO, new BlockPos(2, 0, 0)), toList(remainder));
        assertEquals(3, toList(a).size(), "a must be left unchanged");
    }

    @Test
    void copyIsIndependentOfFurtherChangesToTheOriginal() {
        Selection original = new SimpleSelection(List.of(BlockPos.ZERO));
        Selection copy = original.copy();

        Selection grown = original.add(new SimpleSelection(List.of(new BlockPos(1, 0, 0))));

        assertFalse(toList(copy).contains(new BlockPos(1, 0, 0)));
        assertTrue(toList(grown).contains(new BlockPos(1, 0, 0)));
    }

    private static List<BlockPos> toList(Iterable<BlockPos> positions) {
        List<BlockPos> result = new ArrayList<>();
        positions.forEach(result::add);
        return result;
    }
}
