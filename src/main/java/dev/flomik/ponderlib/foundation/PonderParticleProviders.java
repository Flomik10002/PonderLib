package dev.flomik.ponderlib.foundation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import dev.flomik.ponderlib.mixin.client.ParticleEngineAccessor;

import java.util.Map;

/**
 * Gets a real vanilla {@link Particle} for a fake scene by fetching the {@link ParticleProvider}
 * already registered for a {@link ParticleOptions}' type and calling it directly with a scene's own
 * wrapped level - the same {@link ParticleProvider#createParticle} vanilla's own {@link
 * ParticleEngine#createParticle} would end up calling, just skipping past {@code ParticleEngine}
 * itself (which always renders/ticks against the REAL current level, not whatever {@link
 * ClientLevel} you hand it - see {@code ParticleEngine#makeParticle}, which always passes {@code
 * this.level}). {@code ParticleEngine#providers} is private with no public accessor in vanilla, and
 * unlike every other vanilla member this mod needs it cannot be widened by an access transformer
 * either - it is reached through {@link ParticleEngineAccessor}, an accessor mixin, for the reason
 * documented on that interface.
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
        Map<ResourceLocation, ParticleProvider<?>> providers =
            ((ParticleEngineAccessor) particleEngine).ponderlib$getProviders();
        ParticleProvider<T> provider = (ParticleProvider<T>) providers.get(BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType()));
        return provider == null ? null : provider.createParticle(options, level, x, y, z, mx, my, mz);
    }
}
