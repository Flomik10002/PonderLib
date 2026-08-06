package dev.flomik.ponderlib.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;

/**
 * Vanilla's own {@code POSITION_COLOR_SHADER}/{@code NO_TRANSPARENCY}/{@code
 * TRANSLUCENT_TRANSPARENCY}/{@code NO_CULL}/{@code COLOR_WRITE} shard singletons are declared
 * {@code protected} on {@link RenderStateShard} - not reachable from here, and not worth an access
 * transformer for since the nested shard classes that build them are already public. These are
 * fresh instances with the same GL effect, built for the handful of custom {@code RenderType}s this
 * package and {@code foundation.ui.PonderUI} construct for their own quads (box borders, outlines,
 * shadow/flash gradients).
 */
public final class PonderRenderStateShards {

    public static final RenderStateShard.ShaderStateShard POSITION_COLOR_SHADER =
        new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader);

    public static final RenderStateShard.TransparencyStateShard NO_TRANSPARENCY =
        new RenderStateShard.TransparencyStateShard("ponderlib_no_transparency", () -> {
        }, () -> {
        });

    public static final RenderStateShard.TransparencyStateShard TRANSLUCENT_TRANSPARENCY =
        new RenderStateShard.TransparencyStateShard("ponderlib_translucent_transparency", () -> {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }, RenderSystem::disableBlend);

    public static final RenderStateShard.CullStateShard NO_CULL = new RenderStateShard.CullStateShard(false);

    public static final RenderStateShard.WriteMaskStateShard COLOR_WRITE = new RenderStateShard.WriteMaskStateShard(true, false);

    private PonderRenderStateShards() {
    }
}
