package dev.flomik.ponderlib.api.element;

import java.util.UUID;

/**
 * A UUID-based handle to an element created earlier in the same scene. Instructions scheduled
 * later refer to elements this way instead of holding a direct Java reference, since elements get
 * discarded and recreated whenever a scene is rebuilt (e.g. {@code PonderScene#begin}).
 */
public interface ElementLink<T extends PonderElement> {

    UUID getId();

    /**
     * Casts a resolved {@link PonderElement} back to this link's own type — used internally by
     * {@code PonderScene#resolve}; storyboards never need to call this directly.
     */
    T cast(PonderElement element);
}
