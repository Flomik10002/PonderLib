package dev.flomik.ponderlib.api.registration;

import dev.flomik.ponderlib.api.scene.PonderStoryBoard;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * The record of one registered scene, before compilation.
 */
public interface StoryBoardEntry {

    PonderStoryBoard getBoard();

    ResourceLocation getSchematicLocation();

    ResourceLocation getComponent();

    /**
     * Arbitrary labels a mod can register scenes under (e.g. a category) - browsable via {@code
     * foundation.ui.PonderTagIndexScreen}/{@code PonderIndexScreen}'s tag filter, and queryable
     * directly through {@code foundation.registration.PonderSceneRegistry#getScenesByTag}. {@code
     * getScenes} itself does not filter by tag - it always returns every scene for a given {@link
     * #getComponent()}.
     */
    default Set<ResourceLocation> getTags() {
        return Set.of();
    }

    /**
     * Other entries (identified by their {@link #getSchematicLocation()}) that must come after
     * this one when multiple scenes are registered for the same {@link #getComponent()} - see
     * {@code PonderSceneRegistry#getScenes}'s topological sort. References to scenes outside the
     * same component, or to entries that were never registered, are silently ignored rather than
     * failing registration (a mod removing/renaming a scene shouldn't break another mod's ordering
     * hint against it).
     */
    default Set<ResourceLocation> getOrderBefore() {
        return Set.of();
    }

    /**
     * The mirror of {@link #getOrderBefore()}: other entries that must come before this one.
     */
    default Set<ResourceLocation> getOrderAfter() {
        return Set.of();
    }
}
