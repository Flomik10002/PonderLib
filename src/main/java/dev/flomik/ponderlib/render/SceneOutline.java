package dev.flomik.ponderlib.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Draws a box outline as twelve solid edge boxes rather than GL lines. Extracted from
 * {@code PonderUI}'s identify-mode hover highlight so the storyboard-facing outline element
 * ({@code foundation.element.OutlineElement}) draws exactly the same shape through exactly the same
 * code, instead of a second, drifting copy.
 * <p>
 * Why edge boxes and not {@code RenderType.lines()}: a GL line's width is a fixed screen-space pixel
 * count that does not scale with how large the block is on screen, so at a scene's zoom it reads as a
 * barely-visible scratch. Each of the cube's 12 edges is its own small box instead, inflated on its two
 * CONSTANT axes only, so adjoining edges meet exactly at the shared corners with no gap and no overlap.
 */
public final class SceneOutline {

    /**
     * Note the buffer size: 12 edge boxes x 6 faces x 4 vertices = 288 vertices per box, and the size
     * passed here is a real cap — an earlier 256 silently truncated the tail of the stream, which
     * always cut one of the bottom edges (and no depth-test or blending change could fix it, because
     * that edge was never submitted). 1536 matches {@code RenderType.debugQuads()} and leaves headroom.
     * <p>
     * {@code NO_TRANSPARENCY}: a solid highlight has no reason to blend, and blending left a seam where
     * two of the outline's own edge boxes abut. Depth test is left at the builder's default
     * ({@code LEQUAL}) on purpose — the outline should be occluded by real geometry in front of it,
     * exactly like the block it surrounds.
     */
    public static final RenderType RENDER_TYPE = RenderType.create(
        "ponderlib_scene_outline",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(PonderRenderStateShards.POSITION_COLOR_SHADER)
            .setTransparencyState(PonderRenderStateShards.NO_TRANSPARENCY)
            .setCullState(PonderRenderStateShards.NO_CULL)
            .createCompositeState(false)
    );

    private SceneOutline() {
    }

    /**
     * Outlines the axis-aligned box spanning {@code min..max}, transformed by {@code pose}.
     *
     * @param thickness edge box thickness, in the same units as the box itself
     * @param argb      0xAARRGGBB
     */
    public static void render(VertexConsumer consumer, Matrix4f pose,
                              float minX, float minY, float minZ,
                              float maxX, float maxY, float maxZ,
                              float thickness, int argb) {
        float t = thickness / 2F;
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        // Each edge connects two corners differing in exactly one coordinate: inflate the two constant
        // axes by +-t and leave the varying one at its full span.
        for (float y : new float[]{minY, maxY}) {
            for (float z : new float[]{minZ, maxZ}) {
                box(consumer, pose, minX, y - t, z - t, maxX, y + t, z + t, r, g, b, a);
            }
        }
        for (float x : new float[]{minX, maxX}) {
            for (float z : new float[]{minZ, maxZ}) {
                box(consumer, pose, x - t, minY, z - t, x + t, maxY, z + t, r, g, b, a);
            }
        }
        for (float x : new float[]{minX, maxX}) {
            for (float y : new float[]{minY, maxY}) {
                box(consumer, pose, x - t, y - t, minZ, x + t, y + t, maxZ, r, g, b, a);
            }
        }
    }

    private static void box(VertexConsumer consumer, Matrix4f pose,
                            float x0, float y0, float z0, float x1, float y1, float z1,
                            int r, int g, int b, int a) {
        quad(consumer, pose, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1, r, g, b, a);
        quad(consumer, pose, x1, y0, z1, x1, y1, z1, x1, y1, z0, x1, y0, z0, r, g, b, a);
        quad(consumer, pose, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a);
        quad(consumer, pose, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, r, g, b, a);
        quad(consumer, pose, x1, y0, z0, x1, y1, z0, x0, y1, z0, x0, y0, z0, r, g, b, a);
        quad(consumer, pose, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1, r, g, b, a);
    }

    private static void quad(VertexConsumer consumer, Matrix4f pose,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             int r, int g, int b, int a) {
        vertex(consumer, pose, x1, y1, z1, r, g, b, a);
        vertex(consumer, pose, x2, y2, z2, r, g, b, a);
        vertex(consumer, pose, x3, y3, z3, r, g, b, a);
        vertex(consumer, pose, x4, y4, z4, r, g, b, a);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z,
                              int r, int g, int b, int a) {
        Vector4f pos = new Vector4f(x, y, z, 1F).mul(pose);
        consumer.vertex(pos.x(), pos.y(), pos.z()).color(r, g, b, a).endVertex();
    }
}
