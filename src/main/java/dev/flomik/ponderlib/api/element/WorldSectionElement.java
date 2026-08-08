package dev.flomik.ponderlib.api.element;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A section of blocks shown in a scene, animatable as a rigid body: {@link #setAnimatedRotation}
 * rotates it (in degrees, around its selection's center) and {@link #setAnimatedOffset} moves it,
 * both independently of the other. Get a link to one via
 * {@code SceneBuilder#world()#showSection}, then animate it with
 * {@code WorldInstructions#rotateSection}/{@code #moveSection}.
 */
public interface WorldSectionElement extends AnimatedSceneElement {

    void setAnimatedRotation(Vec3 eulerDegrees);

    Vec3 getAnimatedRotation();

    void setAnimatedOffset(Vec3 offset);

    Vec3 getAnimatedOffset();

    /**
     * The block entity at {@code pos} within this section, if any — an escape hatch for
     * storyboards to reach block-specific APIs (e.g. triggering a chest's open animation) that
     * have no reason to be generic instructions.
     */
    BlockEntity getBlockEntity(BlockPos pos);

    /**
     * Swaps the {@link BlockState} already shown at {@code pos} within this section for a new one
     * (e.g. a furnace's {@code LIT} property flipping on mid-scene) — a no-op if {@code pos} isn't
     * one of this section's own captured positions. Unlike the section's initial content (baked
     * once from the schematic when the section is first shown), this takes effect on the very next
     * frame — there's no fade or transition, the swap itself is instant, same as a real block state
     * change in a real world.
     */
    void setBlockState(BlockPos pos, BlockState state);
}
