package dev.flomik.ponderlib.api.element;

import net.minecraft.world.entity.Entity;

import java.util.function.Consumer;

/**
 * A handle to an {@link Entity} spawned in a scene via {@code WorldInstructions#createEntity}.
 * Unlike {@link WorldSectionElement}, this has no render/tick role of its own — the entity ticks
 * and renders exactly like any entity in a real world would, on its own. This exists purely so a
 * later instruction can get the entity back via {@code WorldInstructions#modifyEntity}, the same
 * reason {@link WorldSectionElement}'s link exists for a section.
 */
public interface EntityElement extends PonderElement {

    /**
     * Runs {@code action} with the wrapped entity, or not at all if it's already gone (died, fell
     * out of the scene, or the scene itself was restarted).
     */
    void ifPresent(Consumer<Entity> action);
}
