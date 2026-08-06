package dev.flomik.ponderlib.api.scene;

import net.minecraft.world.phys.Vec3;

/**
 * Special effects to embellish a scene - one effect for now.
 */
public interface EffectInstructions {

    /**
     * Emits {@code count} short-lived colored sparks scattering outward from {@code position} — real
     * vanilla {@code DustParticleOptions} particles, spawned through the scene's own level exactly
     * like any other in-scene particle (see {@code foundation.PonderSceneParticles}).
     *
     * @param color 0xRRGGBB (no alpha channel - sparks fade out on their own over their lifetime)
     */
    void emitSparks(Vec3 position, int color, int count);
}
