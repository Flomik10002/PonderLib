package dev.flomik.ponderlib.foundation.element;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.flomik.ponderlib.api.element.WorldSectionElement;
import dev.flomik.ponderlib.api.scene.Selection;
import dev.flomik.ponderlib.foundation.PonderLevel;
import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.render.SceneRenderBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WorldSectionElementImpl implements WorldSectionElement {

    // Per-block, per-tick chance of running Block#animateTick (see #animateBlocks). Derived from
    // vanilla's own density rather than picked by feel: ClientLevel.animateTick runs 667 iterations
    // per tick, each doing two doAnimateTick calls that pick a random position via
    // `center + random.nextInt(n) - random.nextInt(n)` for n = 16 and n = 32. That difference is
    // triangular, peaking at offset 0 with probability n/n^2 = 1/n per axis, so a block sitting
    // exactly at the sampling center gets picked with probability (1/16)^3 + (1/32)^3 ~= 2.75e-4 per
    // iteration, i.e. ~0.18 per tick across all 667. Calling animateTick unconditionally every tick
    // instead (the obvious naive choice) would be ~5x denser than vanilla ever is.
    private static final float ANIMATE_TICK_CHANCE = 0.18F;

    private final RandomSource random = RandomSource.create();
    private final Selection selection;
    private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
    private final Map<BlockPos, BlockEntity> blockEntities = new LinkedHashMap<>();
    private final Map<BlockState, List<SceneRenderBuffer>> bakedByState = new LinkedHashMap<>();
    // 0..9 (ModelBakery.DESTROY_STAGE_COUNT - 1), no entry = no crack overlay - see
    // #setBreakingStage/WorldInstructions#incrementBlockBreakingProgress.
    private final Map<BlockPos, Integer> breakingStages = new LinkedHashMap<>();
    private boolean visible;
    private boolean basePlate;

    // Stashed at #capture so #render can pass a real BlockAndTintGetter to vanilla's own
    // BlockRenderDispatcher#renderBreakingTexture for the crack overlay - the baked SceneRenderBuffer
    // geometry this element normally renders through has no such dependency, this is the one path
    // that goes through vanilla's real block model renderer instead.
    private PonderLevel level;

    private Vec3 animatedRotation = Vec3.ZERO;
    private Vec3 animatedOffset = Vec3.ZERO;

    // null = rotate around the selection's own center (the default) - see #configureCenterOfRotation.
    private Vec3 rotationCenter;

    // A section reveal is a light-level fade (dim to full-bright, since baked geometry can't
    // cheaply alpha-blend - see SceneRenderBuffer) plus a small slide in from fadeFromNormal, both
    // driven by fade going 0 (just revealed) to 1 (fully shown). Defaults to "already fully
    // visible" so nothing changes for callers that never touch these (e.g. direct setVisible(true)
    // without going through RevealSectionInstruction).
    private float fade = 1F;
    private Vec3 fadeFromNormal = Vec3.ZERO;

    public WorldSectionElementImpl(Selection selection) {
        this.selection = selection;
    }

    /**
     * Snapshots block states (and any block entities, already loaded with their saved data - see
     * {@code PonderScene#compile}) for this element's selection from {@code level}, and bakes
     * render buffers for the states. Called once when the section is shown — from then on this
     * element owns its own render/tick data and no longer depends on the level.
     */
    public void capture(PonderLevel level) {
        this.level = level;
        for (BlockPos pos : selection) {
            BlockPos immutable = pos.immutable();
            BlockState state = level.getBlockState(pos);
            blocks.put(immutable, state);
            bakedByState.computeIfAbsent(state, SceneRenderBuffer::bakeAll);

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                blockEntities.put(immutable, blockEntity);
            }
        }
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return blockEntities.get(pos);
    }

    /**
     * The (local, schematic-space) positions this section actually has baked geometry for - used
     * by {@code foundation.ui.PonderUI}'s identify-mode hover picking to know which positions to
     * forward-project (see {@link #getSectionTransform()}) and compare against the cursor.
     */
    public Set<BlockPos> getBlockPositions() {
        return Collections.unmodifiableSet(blocks.keySet());
    }

    public BlockState getBlockState(BlockPos pos) {
        return blocks.get(pos);
    }

    /**
     * Sets/advances the crack overlay stage shown at {@code pos} - see {@code
     * WorldInstructions#incrementBlockBreakingProgress}. {@code stage < 0} removes the overlay
     * entirely; otherwise clamped to vanilla's own 0..{@code ModelBakery.DESTROY_STAGE_COUNT - 1}
     * range. A no-op if {@code pos} isn't one of this section's own captured positions.
     */
    public void setBreakingStage(BlockPos pos, int stage) {
        if (!blocks.containsKey(pos)) {
            return;
        }
        if (stage < 0) {
            breakingStages.remove(pos);
        } else {
            breakingStages.put(pos, Mth.clamp(stage, 0, ModelBakery.DESTROY_STAGE_COUNT - 1));
        }
    }

    @Override
    public void setBlockState(BlockPos pos, BlockState state) {
        if (!blocks.containsKey(pos)) {
            return;
        }
        blocks.put(pos, state);
        BlockEntity blockEntity = blockEntities.get(pos);
        if (blockEntity != null) {
            blockEntity.setBlockState(state);
        }
        // computeIfAbsent, not put - bakeAll re-tesselates and bakes a fresh GPU buffer (see
        // SceneRenderBuffer), real work worth skipping if this exact state was already baked for
        // this section before (e.g. toggling a furnace's LIT property back and forth).
        bakedByState.computeIfAbsent(state, SceneRenderBuffer::bakeAll);
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    /**
     * Marks this section as the scene's base plate - set only by {@code
     * PonderSceneBuilder#showBasePlate}. {@code foundation.ui.PonderUI}'s identify-mode hover
     * picking skips base plate sections entirely: the floor is scenery, not something a scene is
     * ever actually about, so it shouldn't outline or tooltip like a real subject block would.
     */
    public void setBasePlate(boolean basePlate) {
        this.basePlate = basePlate;
    }

    public boolean isBasePlate() {
        return basePlate;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void setAnimatedRotation(Vec3 eulerDegrees) {
        this.animatedRotation = eulerDegrees;
    }

    @Override
    public Vec3 getAnimatedRotation() {
        return animatedRotation;
    }

    @Override
    public void setAnimatedOffset(Vec3 offset) {
        this.animatedOffset = offset;
    }

    @Override
    public Vec3 getAnimatedOffset() {
        return animatedOffset;
    }

    @Override
    public void setFade(float fade) {
        this.fade = fade;
    }

    @Override
    public void forceApplyFade(float fade) {
        this.fade = fade;
    }

    @Override
    public void setFadeVec(Vec3 fadeVec) {
        this.fadeFromNormal = fadeVec;
    }

    public float getFade() {
        return fade;
    }

    /**
     * Sets the direction a reveal/hide slides from/to - a half-block offset along {@code
     * direction}'s normal that shrinks to zero as {@link #fade} reaches 1. {@code null} disables the
     * slide (fade-only, e.g. for a section revealed with no meaningful direction).
     */
    public void setFadeFromDirection(Direction direction) {
        setFadeVec(direction == null ? Vec3.ZERO : Vec3.atLowerCornerOf(direction.getNormal()).scale(0.5));
    }

    /**
     * Overrides the point {@link #getSectionTransform} rotates around - see {@code
     * WorldInstructions#configureCenterOfRotation}. {@code null} (the default) falls back to the
     * selection's own center.
     */
    public void setRotationCenter(Vec3 anchor) {
        this.rotationCenter = anchor;
    }

    private Vec3 rotationCenter() {
        return rotationCenter != null ? rotationCenter : selection.getCenter();
    }

    /**
     * Moves {@code positions} out of this section's own tracked blocks/block-entities/baked
     * geometry and into {@code target}'s - the "pull apart" half of {@code
     * WorldInstructions#makeSectionIndependent}, so those positions render as part of {@code target}
     * instead of here from this point on.
     */
    public void extractInto(WorldSectionElementImpl target, Set<BlockPos> positions) {
        for (BlockPos pos : positions) {
            BlockState state = blocks.remove(pos);
            if (state == null) {
                continue;
            }
            target.blocks.put(pos, state);
            target.bakedByState.computeIfAbsent(state, SceneRenderBuffer::bakeAll);
            BlockEntity blockEntity = blockEntities.remove(pos);
            if (blockEntity != null) {
                target.blockEntities.put(pos, blockEntity);
            }
        }
    }

    /**
     * Copies every block/block-entity/baked-geometry entry from {@code other} into this section -
     * the backing of {@code WorldInstructions#showSectionAndMerge}. {@code other} is a throwaway
     * staging element that only ever exists to be captured once and merged here.
     */
    public void mergeFrom(WorldSectionElementImpl other) {
        blocks.putAll(other.blocks);
        blockEntities.putAll(other.blockEntities);
        bakedByState.putAll(other.bakedByState);
    }

    @Override
    public void tick(PonderScene scene) {
        Level level = scene.getLevel();
        for (Map.Entry<BlockPos, BlockEntity> entry : blockEntities.entrySet()) {
            tickBlockEntity(level, entry.getKey(), entry.getValue());
        }
        if (visible) {
            animateBlocks(level);
        }
    }

    /**
     * Gives each shown block its own {@code Block#animateTick} — the client-side hook a block uses
     * to emit its ambient particles/sounds (a lit furnace's smoke and flame, a torch's flame,
     * redstone dust's sparks) — so a block that visibly animates in a real world does the same
     * inside a scene showing it, without the storyboard having to know or care.
     * <p>
     * Particles land in the scene's own pool rather than the real world because {@code PonderLevel}
     * overrides {@code addParticle} — see {@code PonderLevel#addParticle}/{@code PonderSceneParticles}.
     */
    private void animateBlocks(Level level) {
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            if (random.nextFloat() >= ANIMATE_TICK_CHANCE) {
                continue;
            }
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();
            state.getBlock().animateTick(state, level, pos, random);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void tickBlockEntity(Level level, BlockPos pos, BlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        if (!(state.getBlock() instanceof EntityBlock entityBlock)) {
            return;
        }
        BlockEntityTicker ticker = entityBlock.getTicker(level, state, blockEntity.getType());
        if (ticker != null) {
            ticker.tick(level, pos, state, blockEntity);
        }
    }

    /**
     * The section's current rigid-body transform: rotate around the selection's center, then shift
     * by the animated offset (plus the shrinking fade-in slide) - see {@code
     * api.element.WorldSectionElement}'s javadoc. Shared by {@link #render} and {@code
     * foundation.ui.PonderUI}'s identify-mode hover picking, which needs to forward-project the
     * exact same per-block positions the renderer draws at.
     */
    public Matrix4f getSectionTransform() {
        Vec3 center = rotationCenter();
        Vec3 fadeOffset = fadeFromNormal.scale(1 - fade);
        return new Matrix4f()
            .translate((float) (animatedOffset.x + fadeOffset.x + center.x), (float) (animatedOffset.y + fadeOffset.y + center.y), (float) (animatedOffset.z + fadeOffset.z + center.z))
            .rotateXYZ((float) Math.toRadians(animatedRotation.x), (float) Math.toRadians(animatedRotation.y), (float) Math.toRadians(animatedRotation.z))
            .translate((float) -center.x, (float) -center.y, (float) -center.z);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, float partialTick) {
        if (!visible) {
            return;
        }

        Matrix4f sectionTransform = getSectionTransform();

        // A light-level fade (5 = dim, 15 = full-bright), only overridden while actually fading -
        // once fade reaches 1 this matches the baked light exactly, so -1 (use baked) would be
        // equivalent, but keeping one code path is simpler.
        int fadeLightLevel = (int) Mth.lerp(fade, 5, 15);
        int light = LightTexture.pack(fadeLightLevel, fadeLightLevel);

        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            Matrix4f blockTransform = new Matrix4f(sectionTransform).translate(pos.getX(), pos.getY(), pos.getZ());

            List<SceneRenderBuffer> baked = bakedByState.get(entry.getValue());
            if (baked != null) {
                for (SceneRenderBuffer layer : baked) {
                    VertexConsumer consumer = buffer.getBuffer(layer.getRenderType());
                    layer.renderInto(poseStack, blockTransform, consumer, light);
                }
            }

            BlockEntity blockEntity = blockEntities.get(pos);
            if (blockEntity != null) {
                renderBlockEntity(poseStack, blockTransform, blockEntity, buffer, partialTick);
            }

            Integer stage = breakingStages.get(pos);
            if (stage != null && level != null) {
                renderBreakingOverlay(poseStack, blockTransform, entry.getValue(), pos, level, buffer, stage);
            }
        }
    }

    /**
     * Vanilla's own mining crack texture, drawn through the real {@link BlockRenderDispatcher} —
     * unlike this element's own baked {@link SceneRenderBuffer} geometry, this goes through the real
     * block model renderer directly (it's the only vanilla API surface for this effect), so it needs
     * a real {@code BlockAndTintGetter} ({@link #level}, stashed at {@link #capture}) rather than
     * this element's own captured state.
     */
    private static void renderBreakingOverlay(PoseStack poseStack, Matrix4f blockTransform, BlockState state,
                                               BlockPos pos, PonderLevel level, MultiBufferSource buffer, int stage) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        VertexConsumer consumer = buffer.getBuffer(ModelBakery.DESTROY_TYPES.get(stage));
        poseStack.pushPose();
        poseStack.last().pose().mul(blockTransform);
        poseStack.last().normal().mul(new Matrix3f(blockTransform));
        dispatcher.renderBreakingTexture(state, pos, level, poseStack, consumer);
        poseStack.popPose();
    }

    @SuppressWarnings("unchecked")
    private static void renderBlockEntity(PoseStack poseStack, Matrix4f blockTransform, BlockEntity blockEntity,
                                           MultiBufferSource buffer, float partialTick) {
        // Not BlockEntityRenderDispatcher#render(...): it gates on shouldRender(be, camera.getPosition())
        // - a real distance check against the REAL player camera, which is nowhere near a scene's
        // tiny local coordinates (e.g. (1,1,1)), so it always silently fails and skips rendering.
        // Call the actual BlockEntityRenderer directly instead, skipping that (and the similarly
        // irrelevant hasLevel/isValid) gating entirely - a Ponder scene always wants to render its
        // own block entities regardless of where the real camera happens to be standing.
        BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        BlockEntityRenderer<BlockEntity> renderer = (BlockEntityRenderer<BlockEntity>) dispatcher.getRenderer(blockEntity);
        if (renderer == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.last().pose().mul(blockTransform);
        poseStack.last().normal().mul(new Matrix3f(blockTransform));
        renderer.render(blockEntity, partialTick, poseStack, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
