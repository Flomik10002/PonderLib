package dev.flomik.ponderlib.foundation.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Pins the exact Ponder 1.0.91 scene-information geometry and title-transition proportions. */
class PonderSceneInformationLayoutTest {

    @Test
    void plaqueUsesPondersFixedTopLeftGeometry() {
        assertEquals(55, PonderUI.SCENE_INFO_ORIGIN_X);
        assertEquals(19, PonderUI.SCENE_INFO_ORIGIN_Y);
        assertEquals(-34, PonderUI.SCENE_INFO_ICON_X);
        assertEquals(2, PonderUI.SCENE_INFO_ICON_Y);
        assertEquals(30, PonderUI.SCENE_INFO_ICON_SIZE);
        assertEquals(-35, PonderUI.SCENE_INFO_SUBJECT_X);
        assertEquals(1, PonderUI.SCENE_INFO_SUBJECT_Y);
        assertEquals(2F, PonderUI.SCENE_INFO_SUBJECT_SCALE, 0F);
        assertEquals(180, PonderUI.SCENE_INFO_TITLE_MAX_WIDTH);

        assertEquals(21, PonderUI.SCENE_INFO_ORIGIN_X + PonderUI.SCENE_INFO_ICON_X);
        assertEquals(21, PonderUI.SCENE_INFO_ORIGIN_Y + PonderUI.SCENE_INFO_ICON_Y);
    }

    @Test
    void streakInterpolatesProportionallyBetweenBothTitleLayouts() {
        assertEquals(35, PonderUI.sceneInfoStreakHeight(9, 27, 0F));
        assertEquals(44, PonderUI.sceneInfoStreakHeight(9, 27, .5F));
        assertEquals(53, PonderUI.sceneInfoStreakHeight(9, 27, 1F));

        assertEquals(130, PonderUI.sceneInfoStreakWidth(60, 180, 0F));
        assertEquals(190, PonderUI.sceneInfoStreakWidth(60, 180, .5F));
        assertEquals(250, PonderUI.sceneInfoStreakWidth(60, 180, 1F));
    }

    @Test
    void neighbourSelectionFollowsTheLazyIndexSign() {
        assertEquals(1, PonderUI.sceneInfoOtherIndex(1, 3, 0F));
        assertEquals(0, PonderUI.sceneInfoOtherIndex(1, 3, -.5F));
        assertEquals(2, PonderUI.sceneInfoOtherIndex(1, 3, .5F));
        assertEquals(-1, PonderUI.sceneInfoOtherIndex(0, 3, -.5F));
        assertEquals(-1, PonderUI.sceneInfoOtherIndex(2, 3, .5F));
    }

    @Test
    void titleFlipMatchesPondersQuarterTurnAtBothEnds() {
        assertEquals(0F, PonderUI.sceneInfoActiveTitleRotation(0F), 0F);
        assertEquals(90F, PonderUI.sceneInfoActiveTitleRotation(-1F), 0F);
        assertEquals(-90F, PonderUI.sceneInfoActiveTitleRotation(1F), 0F);

        assertEquals(0F, PonderUI.sceneInfoOtherTitleRotation(-1F), 0F);
        assertEquals(-45F, PonderUI.sceneInfoOtherTitleRotation(-.5F), 0F);
        assertEquals(45F, PonderUI.sceneInfoOtherTitleRotation(.5F), 0F);
        assertEquals(0F, PonderUI.sceneInfoOtherTitleRotation(1F), 0F);
    }
}
