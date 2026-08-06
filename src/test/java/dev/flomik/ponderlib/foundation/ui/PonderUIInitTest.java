package dev.flomik.ponderlib.foundation.ui;

import dev.flomik.ponderlib.foundation.PonderScene;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Guards the resize-related invariant behind a real, confirmed bug (see docs in the repo's plan file):
 * {@code PonderUI} used to override {@code Screen#init()} just to call {@code scene.begin()}. That
 * looked like a harmless extra reset on first open, but vanilla's {@code Screen.rebuildWidgets()}
 * (called from {@code resize()}) invokes the overridable {@code init()} on EVERY window resize or
 * fullscreen toggle, and GLFW fires a burst of framebuffer-resize callbacks per transition (~30 in
 * under 400ms, confirmed with a temporary diagnostic log). Each call cleared the scene's elements;
 * they normally refill on the next {@code scene.tick()} invisibly - except identify mode gates
 * ticking off entirely, so with it held the scene stayed empty.
 * <p>
 * The guard used to be "init() must not exist at all". That is no longer the right shape: init() is
 * vanilla's widget-building hook and the button row genuinely has to be rebuilt on resize, because
 * every button's position is derived from width/height. So the invariant is now narrower and stated
 * directly: init() may build widgets, and no resize hook may reset scene state. Only the structural
 * half of that is mechanically checkable from here, so the scene-state half is asserted where it IS
 * reachable - through {@link #pagingRestartsOnlyTheSceneItMovesTo}, the one path that legitimately
 * calls {@code begin()}.
 */
class PonderUIInitTest {

    @Test
    void buildsItsWidgetsInInitSoTheySurviveAResize() {
        assertDoesNotThrow(() -> PonderUI.class.getDeclaredMethod("init"),
            "PonderUI must build its buttons in init() - it is the only hook vanilla re-runs on resize, "
                + "and every button's position depends on the screen size");
    }

    @Test
    void overridesNoOtherResizeHook() {
        // resize()/repositionElements() are the other two entry points vanilla calls on a window
        // change. Nothing should be moved into them: init() is where widget layout belongs, and any
        // scene mutation on these paths is the exact bug this class exists for.
        for (String hook : List.of("repositionElements")) {
            try {
                PonderUI.class.getDeclaredMethod(hook);
                fail("PonderUI must not override " + hook + "() - see this class' javadoc");
            } catch (NoSuchMethodException expected) {
                // good
            }
        }
        try {
            PonderUI.class.getDeclaredMethod("resize", net.minecraft.client.Minecraft.class, int.class, int.class);
            fail("PonderUI must not override resize() - see this class' javadoc");
        } catch (NoSuchMethodException expected) {
            // good
        }
    }

    @Test
    void pagingRestartsOnlyTheSceneItMovesTo() {
        PonderScene first = mock(PonderScene.class);
        PonderScene second = mock(PonderScene.class);
        PonderUI ui = new PonderUI(List.of(first, second));

        assertTrue(ui.scroll(true), "should page forward when there is a next scene");
        assertEquals(second, ui.scene());
        verify(second).begin();
        // The scene being left must NOT be restarted - only the one being shown.
        verify(first, never()).begin();
    }

    @Test
    void pagingStopsAtBothEndsWithoutRestartingAnything() {
        PonderScene only = mock(PonderScene.class);
        PonderUI ui = new PonderUI(List.of(only));

        assertFalse(ui.scroll(true), "a single-scene item has nowhere to page to");
        assertFalse(ui.scroll(false), "a single-scene item has nowhere to page back to");
        assertEquals(only, ui.scene());
        verify(only, never()).begin();
    }
}
