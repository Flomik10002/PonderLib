package dev.flomik.ponderlib.api.scene;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Common scene-space {@link Vec3} points, so a storyboard can say what it means — the centre of a
 * block, or a point on one of its faces — instead of hand-writing {@code new Vec3(x + 0.5, y + 0.5,
 * z + 0.5)}.
 */
public interface VectorUtil {

    Vec3 centerOf(int x, int y, int z);

    Vec3 centerOf(BlockPos pos);

    /**
     * The centre of {@code pos}'s top face — {@code blockSurface(pos, Direction.UP)}.
     */
    Vec3 topOf(int x, int y, int z);

    Vec3 topOf(BlockPos pos);

    Vec3 blockSurface(BlockPos pos, Direction face);

    /**
     * @param margin how far outside the block's own bounds to sit, along {@code face}'s normal — 0 sits
     *               exactly on the face.
     */
    Vec3 blockSurface(BlockPos pos, Direction face, float margin);

    Vec3 of(double x, double y, double z);
}
