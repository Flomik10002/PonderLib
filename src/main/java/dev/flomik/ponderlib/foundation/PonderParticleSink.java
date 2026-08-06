package dev.flomik.ponderlib.foundation;

import net.minecraft.core.particles.ParticleOptions;

/**
 * Where a scene's {@link PonderLevel} sends particles spawned inside it — implemented by
 * {@link PonderSceneParticles} (client-only, owns the actual {@code net.minecraft.client.particle.Particle}
 * instances).
 * <p>
 * This indirection exists purely for dist safety: {@code PonderLevel} is loaded on the DEDICATED_SERVER
 * dist too (its GameTests construct one directly), and NeoForge's {@code RuntimeDistCleaner} refuses
 * to load client-only classes there — so {@code PonderLevel} can only ever refer to particles through
 * a dist-neutral type like this one. {@link ParticleOptions} itself is dist-neutral (the server sends
 * these to clients), so it's fine in this signature.
 */
public interface PonderParticleSink {

    void addParticle(ParticleOptions options, double x, double y, double z, double mx, double my, double mz);
}
