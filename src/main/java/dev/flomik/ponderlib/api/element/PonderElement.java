package dev.flomik.ponderlib.api.element;

import dev.flomik.ponderlib.foundation.PonderScene;

/**
 * One visible or animatable thing in a scene — a shown section of blocks, a text window, an
 * outline, a tracked entity. Most storyboards never implement this directly; it's the shape
 * {@code WorldInstructions}/{@code OverlayInstructions} calls hand back as an {@link ElementLink}.
 */
public interface PonderElement {

    /**
     * Called once per scene tick while this element is part of the scene, for anything that needs
     * to update over time on its own (e.g. re-checking a block entity's ticker). No-op by default —
     * most elements are driven entirely by the instruction that shows/hides/animates them instead.
     */
    default void tick(PonderScene scene) {
    }

    /**
     * Called when the scene restarts from the beginning (a fresh play, a replay, or scrubbing
     * backward on the timeline). No-op by default.
     */
    default void reset(PonderScene scene) {
    }

    /**
     * Whether this element currently renders/ticks as part of the scene. Most elements start
     * invisible until the instruction that owns them calls {@link #setVisible} — a section fading
     * in, a text window easing in, and so on.
     */
    boolean isVisible();

    void setVisible(boolean visible);
}
