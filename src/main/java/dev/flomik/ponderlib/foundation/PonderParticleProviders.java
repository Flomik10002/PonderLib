package dev.flomik.ponderlib.foundation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Gets a real vanilla {@link Particle} for a fake scene by fetching the {@link ParticleProvider}
 * already registered for a {@link ParticleOptions}' type and calling it directly with a scene's own
 * wrapped level - the same {@link ParticleProvider#createParticle} vanilla's own {@link
 * ParticleEngine#createParticle} would end up calling, just skipping past {@code ParticleEngine}
 * itself (which always renders/ticks against the REAL current level, not whatever {@link
 * ClientLevel} you hand it - see {@code ParticleEngine#makeParticle}, which always passes {@code
 * this.level}). {@code ParticleEngine}'s {@code providers} map is private with no public accessor,
 * so this reaches it via plain reflection instead, cached once since the map itself never changes
 * after client setup.
 */
public final class PonderParticleProviders {

    private static final MethodHandle PROVIDERS_GETTER = resolveProvidersGetter();

    private PonderParticleProviders() {
    }

    /**
     * Whether the private-field reflection this class depends on actually succeeded, i.e.
     * whether the current Minecraft version's {@code ParticleEngine} still has a field literally
     * named {@code "providers"} of the expected type - doesn't need a live game instance to check
     * (only {@code ParticleEngine}'s class metadata, resolved once at class-load time), so it's
     * testable without a real Minecraft client - see {@code PonderParticleProvidersTest}.
     */
    public static boolean isAvailable() {
        return PROVIDERS_GETTER != null;
    }

    /**
     * {@code null} if the given options' particle type has no registered provider (shouldn't
     * happen for any real {@link net.minecraft.core.particles.ParticleType} constant, but every
     * vanilla particle-creation path treats a missing provider as "just don't spawn one" rather
     * than an error, so this does too) or if reflection setup itself failed (logged once at
     * startup instead of throwing on every single spawn attempt - see {@link #resolveProvidersGetter}).
     */
    @SuppressWarnings("unchecked")
    public static <T extends ParticleOptions> Particle create(T options, ClientLevel level,
                                                                double x, double y, double z,
                                                                double mx, double my, double mz) {
        if (PROVIDERS_GETTER == null) {
            return null;
        }
        Map<ResourceLocation, ParticleProvider<?>> providers;
        try {
            providers = (Map<ResourceLocation, ParticleProvider<?>>) PROVIDERS_GETTER.invoke(Minecraft.getInstance().particleEngine);
        } catch (Throwable t) {
            return null;
        }
        ParticleProvider<T> provider = (ParticleProvider<T>) providers.get(BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType()));
        return provider == null ? null : provider.createParticle(options, level, x, y, z, mx, my, mz);
    }

    private static MethodHandle resolveProvidersGetter() {
        try {
            Field field = ParticleEngine.class.getDeclaredField("providers");
            field.setAccessible(true);
            return MethodHandles.lookup().unreflectGetter(field);
        } catch (ReflectiveOperationException e) {
            // A future Minecraft/mapping update renamed or removed the field - fail soft (no real
            // particles, PonderLib keeps working otherwise) rather than crashing scene playback.
            return null;
        }
    }
}
