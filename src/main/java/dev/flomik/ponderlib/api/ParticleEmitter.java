package dev.flomik.ponderlib.api;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * One particle-spawning recipe, built by {@code EffectInstructions#simpleParticleEmitter}/{@code
 * #particleEmitterWithinBlockSpace} and fired repeatedly by {@code EffectInstructions#emitParticles}
 * — the two are split apart so the "what particle, what motion" choice can be built once and reused
 * across every cycle of an emission, rather than each cycle re-deciding both "what" and "where".
 */
@FunctionalInterface
public interface ParticleEmitter {

    /**
     * Spawns (typically) one particle into {@code level}, positioned relative to {@code origin} —
     * see the emitter's own javadoc (e.g. {@code particleEmitterWithinBlockSpace}) for exactly how.
     */
    void emit(Level level, Vec3 origin, RandomSource random);
}
