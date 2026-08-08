package dev.flomik.ponderlib.foundation.ui;

import dev.flomik.ponderlib.api.PonderColorScheme;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PonderTagLayoutTest {

    @Test
    void sceneSidebarUsesCreateAnchorsAndPitch() {
        assertEquals(31, PonderUI.TAG_BUTTON_X);
        assertEquals(81, PonderUI.tagButtonY(0));
        assertEquals(111, PonderUI.tagButtonY(1));
        assertEquals(141, PonderUI.tagButtonY(2));
    }

    @Test
    void createButtonUsesTheInflatedFrameAsItsHoverAndClickArea() {
        PonderButton button = new PonderButton(31, 81, PonderButton.Icon.LEFT, Component.empty(),
            () -> { }, () -> PonderColorScheme.DEFAULT).withCreateLayout();

        assertTrue(button.isMouseOver(27, 77));
        assertTrue(button.clicked(27, 77));
        assertFalse(button.isMouseOver(55, 81));
        assertFalse(button.clicked(31, 105));
    }

    @Test
    void emptyLayoutPreservesCatnipsNegativeWidthEdgeCase() {
        PonderTagScreen.LayoutMetrics layout = PonderTagScreen.layoutFor(0);
        assertEquals(1, layout.rows());
        assertArrayEquals(new int[] {0}, layout.rowCounts());
        assertEquals(-8, layout.totalWidth());
        assertEquals(28, layout.totalHeight());
        assertEquals(4, layout.areaX());
        assertEquals(-14, layout.areaY());
    }

    @Test
    void rowsAreDistributedExactlyLikeCenteredHorizontalLayoutHelper() {
        assertLayout(11, 1, new int[] {11}, 388, 28);
        assertLayout(12, 2, new int[] {6, 6}, 208, 64);
        assertLayout(23, 3, new int[] {8, 8, 7}, 280, 100);
        assertLayout(34, 3, new int[] {12, 11, 11}, 424, 100);
    }

    @Test
    void verticalAnchorsDoNotMergeHeaderAndItemGrid() {
        assertEquals(121, PonderTagScreen.itemsY(240));
        assertEquals(-4D, PonderTagScreen.headerOriginY(240));
        assertEquals(40, PonderTagScreen.headerOriginX(320));
        assertEquals(144, PonderTagScreen.descriptionWidth(320));
        assertEquals(159, PonderTagScreen.descriptionY(240, 28));
        assertEquals(175, PonderTagScreen.descriptionY(240, 64));
        assertEquals(211, PonderTagScreen.descriptionY(240, 100));
    }

    private static void assertLayout(int items, int rows, int[] rowCounts, int width, int height) {
        PonderTagScreen.LayoutMetrics layout = PonderTagScreen.layoutFor(items);
        assertEquals(rows, layout.rows());
        assertArrayEquals(rowCounts, layout.rowCounts());
        assertEquals(width, layout.totalWidth());
        assertEquals(height, layout.totalHeight());
    }
}
