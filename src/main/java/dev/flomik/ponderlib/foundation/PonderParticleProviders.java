package dev.flomik.ponderlib.foundation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Gets a real vanilla {@link Particle} for a fake scene by fetching the {@link ParticleProvider}
 * already registered for a {@link ParticleOptions}' type and calling it directly with a scene's own
 * wrapped level - the same {@link ParticleProvider#createParticle} vanilla's own {@link
 * ParticleEngine#createParticle} would end up calling, just skipping past {@code ParticleEngine}
 * itself (which always renders/ticks against the REAL current level, not whatever {@link
 * ClientLevel} you hand it - see {@code ParticleEngine#makeParticle}, which always passes {@code
 * this.level}). {@code ParticleEngine#providers} is private with no public accessor in vanilla;
 * {@code META-INF/accesstransformer.cfg} widens it, so this is a plain field read - no reflection,
 * no runtime name lookup, and a rename in a future Minecraft version breaks the build instead of
 * silently producing no particles at runtime.
 * <p>
 * Note for anyone porting this to the Forge 1.20.1 branch: an AT does <em>not</em> work for this
 * field there. Both loaders patch the field's declaration line (its type becomes {@code
 * Map<ResourceLocation, ...>}), but ForgeGradle applies the AT before decompilation and then
 * recompiles the patched sources, so the patch overwrites the widened access; NeoForge's toolchain
 * applies mod ATs to the final artifact, after patching, so it holds. That branch uses an accessor
 * mixin instead.
 */
public final class PonderParticleProviders {

    private PonderParticleProviders() {
    }

    /**
     * {@code null} if the given options' particle type has no registered provider - shouldn't
     * happen for any real {@link net.minecraft.core.particles.ParticleType} constant, but every
     * vanilla particle-creation path treats a missing provider as "just don't spawn one" rather
     * than an error, so this does too.
     */
    @SuppressWarnings("unchecked")
    public static <T extends ParticleOptions> Particle create(T options, ClientLevel level,
                                                                double x, double y, double z,
                                                                double mx, double my, double mz) {
        ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
        ParticleProvider<T> provider = (ParticleProvider<T>) particleEngine.providers.get(BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType()));
        return provider == null ? null : provider.createParticle(options, level, x, y, z, mx, my, mz);
    }
}
