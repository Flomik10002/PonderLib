package dev.flomik.ponderlib.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * PonderLib's own "retained buffer, transform-only redraw" rendering primitive: a {@link
 * BlockState}'s geometry is baked into a raw vertex buffer exactly once via vanilla {@link
 * net.minecraft.client.renderer.block.ModelBlockRenderer}, then every frame {@link #renderInto}
 * re-emits those same cached vertices offset by a translation, without ever re-tesselating the
 * model. No GPU instancing, no dedicated rendering engine dependency — plain vanilla buffer
 * re-emission.
 * <p>
 * A cached-{@code VertexBuffer}/GPU-uniform version of this ({@link #renderInto} replaced by a
 * {@code renderCached} that uploads the baked mesh once and redraws it via {@code
 * VertexBuffer.drawWithShader} instead of walking vertices in Java every frame) was tried and
 * reverted: it corrupted every section's geometry except block-entity-rendered ones after the
 * first keyframe of a scene. Root cause unconfirmed - re-attempting this needs a live client to
 * actually observe the corruption before assuming the same approach will work this time.
 */
public class SceneRenderBuffer {

    private static final int VERTEX_SIZE = DefaultVertexFormat.BLOCK.getVertexSize();

    private final RenderType renderType;
    private final ByteBuffer template;
    private final int vertexCount;

    private SceneRenderBuffer(RenderType renderType, ByteBuffer template, int vertexCount) {
        this.renderType = renderType;
        this.template = template;
        this.vertexCount = vertexCount;
    }

    /**
     * Bakes one buffer per {@link RenderType} the model actually uses for {@code state} — a block
     * with e.g. a solid base and a translucent overlay (glass panes, some plants) needs more than
     * one draw pass, and baking only the first render type silently dropped the rest.
     */
    public static List<SceneRenderBuffer> bakeAll(BlockState state) {
        BlockPos pos = BlockPos.ZERO;
        VirtualBlockView view = new VirtualBlockView(pos, state);
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getBlockRenderer().getBlockModel(state);
        long seed = state.getSeed(pos);
        RandomSource random = RandomSource.create(seed);

        List<SceneRenderBuffer> buffers = new ArrayList<>();
        for (RenderType renderType : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            buffers.add(bake(view, model, state, pos, random, seed, renderType));
        }
        if (buffers.isEmpty()) {
            buffers.add(bake(view, model, state, pos, random, seed, RenderType.solid()));
        }
        FluidState fluidState = state.getFluidState();
        if (!fluidState.isEmpty()) {
            buffers.add(bakeFluid(view, state, fluidState, pos, ItemBlockRenderTypes.getRenderLayer(fluidState)));
        }
        return buffers;
    }

    /** Fluids have no baked block-model quads; vanilla emits them through renderLiquid instead. */
    private static SceneRenderBuffer bakeFluid(VirtualBlockView view, BlockState state, FluidState fluidState,
                                                BlockPos pos, RenderType renderType) {
        try (ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(2048)) {
            BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, VertexFormat.Mode.QUADS,
                DefaultVertexFormat.BLOCK);
            Minecraft.getInstance().getBlockRenderer().renderLiquid(pos, view, bufferBuilder, state, fluidState);
            try (MeshData mesh = bufferBuilder.build()) {
                if (mesh == null) return new SceneRenderBuffer(renderType, ByteBuffer.allocateDirect(0), 0);
                ByteBuffer rendered = mesh.vertexBuffer().order(ByteOrder.nativeOrder());
                int vertexCount = mesh.drawState().vertexCount();
                ByteBuffer copy = ByteBuffer.allocateDirect(vertexCount * VERTEX_SIZE).order(ByteOrder.nativeOrder());
                copy.put(rendered).flip();
                return new SceneRenderBuffer(renderType, copy, vertexCount);
            }
        }
    }

    static boolean needsFluidPass(BlockState state) {
        return !state.getFluidState().isEmpty();
    }

    private static SceneRenderBuffer bake(VirtualBlockView view, BakedModel model, BlockState state, BlockPos pos,
                                           RandomSource random, long seed, RenderType renderType) {
        Minecraft mc = Minecraft.getInstance();
        try (ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(2048)) {
            BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            random.setSeed(seed);
            mc.getBlockRenderer()
                .getModelRenderer()
                .tesselateBlock(view, model, state, pos, new PoseStack(), bufferBuilder, false, random, seed,
                    OverlayTexture.NO_OVERLAY, ModelData.EMPTY, renderType);

            try (MeshData mesh = bufferBuilder.build()) {
                if (mesh == null) {
                    return new SceneRenderBuffer(renderType, ByteBuffer.allocateDirect(0), 0);
                }

                ByteBuffer rendered = mesh.vertexBuffer();
                rendered.order(ByteOrder.nativeOrder());
                int vertexCount = mesh.drawState().vertexCount();

                ByteBuffer copy = ByteBuffer.allocateDirect(vertexCount * VERTEX_SIZE).order(ByteOrder.nativeOrder());
                copy.put(rendered);
                copy.flip();

                return new SceneRenderBuffer(renderType, copy, vertexCount);
            }
        }
    }

    public RenderType getRenderType() {
        return renderType;
    }

    /**
     * Re-emits the vertices baked in {@link #bake} through {@code viewPose}, additionally
     * transformed by {@code localTransform} (translation, and now rotation too - see
     * {@code WorldSectionElementImpl}'s animated rotation/offset), without ever re-tesselating
     * the model. Uses each vertex's own baked light (always full-bright, since scenes bake against
     * {@link VirtualBlockView}, which is always full-bright).
     */
    public void renderInto(PoseStack viewPose, Matrix4f localTransform, VertexConsumer out) {
        renderInto(viewPose, localTransform, out, -1);
    }

    /**
     * Same as {@link #renderInto(PoseStack, Matrix4f, VertexConsumer)}, but overrides every
     * vertex's light with {@code lightOverride} instead of the baked (always full-bright) value
     * when {@code lightOverride >= 0} - used to dim a section while it's fading into the scene (see
     * {@code foundation.instruction.RevealSectionInstruction}) without having to re-bake anything.
     */
    public void renderInto(PoseStack viewPose, Matrix4f localTransform, VertexConsumer out, int lightOverride) {
        if (vertexCount == 0) {
            return;
        }

        Matrix4f modelMatrix = new Matrix4f(viewPose.last().pose()).mul(localTransform);
        Matrix3f normalMatrix = new Matrix3f(viewPose.last().normal()).mul(new Matrix3f(localTransform));

        Vector4f pos = new Vector4f();
        Vector3f normal = new Vector3f();

        for (int i = 0; i < vertexCount; i++) {
            int base = i * VERTEX_SIZE;

            float x = template.getFloat(base);
            float y = template.getFloat(base + 4);
            float z = template.getFloat(base + 8);

            byte r = template.get(base + 12);
            byte g = template.get(base + 13);
            byte b = template.get(base + 14);
            byte a = template.get(base + 15);

            float u = template.getFloat(base + 16);
            float v = template.getFloat(base + 20);

            int light = lightOverride >= 0 ? lightOverride : template.getInt(base + 24);

            byte nx = template.get(base + 28);
            byte ny = template.get(base + 29);
            byte nz = template.get(base + 30);

            pos.set(x, y, z, 1F).mul(modelMatrix);
            normal.set(nx, ny, nz).mul(normalMatrix);

            out.addVertex(pos.x(), pos.y(), pos.z());
            out.setColor(r, g, b, a);
            out.setUv(u, v);
            out.setLight(light);
            out.setNormal(normal.x(), normal.y(), normal.z());
        }
    }
}
