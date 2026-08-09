package dev.flomik.ponderlib.client;

import dev.flomik.ponderlib.foundation.PonderLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.ApiStatus;

/**
 * Internal policy shared by the client mixins that neutralise vanilla's real-camera nameplate
 * distance inside a local-coordinate Ponder scene.
 */
@ApiStatus.Internal
public final class PonderNameplateDistance {

    private PonderNameplateDistance() {
    }

    /**
     * Returns whether distance alone must be ignored for this entity. Every other renderer and
     * loader visibility rule is deliberately left to its original implementation.
     */
    public static boolean shouldBypass(Entity entity) {
        return entity.level() instanceof PonderLevel && entity.isCustomNameVisible();
    }
}
