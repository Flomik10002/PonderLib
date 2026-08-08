package dev.flomik.ponderlib.api.scene;

import dev.flomik.ponderlib.api.ParticleEmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;

/**
 * Special effects to embellish and communicate with — the visual language for "something just
 * happened" (see [[Scene-Writing Paradigm]]'s guidance on pairing these with a real state change,
 * never a bare decoration).
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

    /**
     * {@link #emitSparks}, keyed by a block position and centered on it rather than an arbitrary
     * scene-space point — {@link #indicateRedstone} is this with a fixed redstone-red colour.
     */
    void createRedstoneParticles(BlockPos pos, int color, int amount);

    /**
     * The standard "a signal just changed here" cue — a small burst of redstone-red sparks centered
     * on {@code pos}. Pair this with the state change that actually caused it (a {@code
     * WorldInstructions#toggleRedstonePower}/{@code #cycleBlockProperty} call), not with a beat where
     * nothing happens.
     */
    void indicateRedstone(BlockPos pos);

    /**
     * The standard "this just completed successfully" cue — a small burst of vanilla's own
     * happy-villager sparkle centered on {@code pos}, the same particle a successful trade/bonemeal
     * use already reads as "good outcome" to a player.
     */
    void indicateSuccess(BlockPos pos);

    /**
     * Fires {@code emitter} {@code amountPerCycle} times per tick, for {@code cycles} ticks, at
     * {@code location} — the general-purpose particle spawner behind {@link #emitSparks}/{@link
     * #indicateSuccess}, for a storyboard that wants a specific vanilla particle type instead of
     * this library's own fixed choices. Build the emitter itself with {@link #simpleParticleEmitter}
     * or {@link #particleEmitterWithinBlockSpace}.
     */
    void emitParticles(Vec3 location, ParticleEmitter emitter, float amountPerCycle, int cycles);

    /**
     * An emitter that spawns {@code data} exactly at {@link #emitParticles}'s {@code location} every
     * cycle, with a fixed {@code motion} — no positional randomness.
     */
    <T extends ParticleOptions> ParticleEmitter simpleParticleEmitter(T data, Vec3 motion);

    /**
     * {@link #simpleParticleEmitter}, jittered within a block-sized cube centered on {@code
     * location} instead of spawning at the exact same point every cycle — for a burst that should
     * read as "coming from this whole block", not a single point.
     */
    <T extends ParticleOptions> ParticleEmitter particleEmitterWithinBlockSpace(T data, Vec3 motion);
}
