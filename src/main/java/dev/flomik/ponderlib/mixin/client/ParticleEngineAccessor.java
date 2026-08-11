package dev.flomik.ponderlib.mixin.client;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * {@code ParticleEngine#providers} is the one private vanilla member in this project that an access
 * transformer cannot reach: Forge's own source patch rewrites that field's declaration line
 * (changing its type from {@code Int2ObjectMap} to {@code Map<ResourceLocation, ...>}), and
 * ForgeGradle applies the AT to the binary jar *before* the Forge patches are applied to the
 * decompiled sources the compile classpath is then recompiled from - so the widened access is
 * overwritten by the patch and {@code javac} still sees {@code private}. An accessor mixin is
 * unaffected, since it rewrites the loaded class at runtime instead. The real Ponder library solves
 * it exactly the same way (see {@code net.createmod.ponder.mixin.client.accessor.ParticleEngineAccessor}).
 * <p>
 * Members Forge does <em>not</em> patch stay on the plain AT route - see
 * {@code META-INF/accesstransformer.cfg}.
 */
@Mixin(ParticleEngine.class)
public interface ParticleEngineAccessor {

    @Accessor("providers")
    Map<ResourceLocation, ParticleProvider<?>> ponderlib$getProviders();
}
