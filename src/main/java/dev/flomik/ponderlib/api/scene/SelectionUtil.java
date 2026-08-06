package dev.flomik.ponderlib.api.scene;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

/**
 * Builds {@link Selection}s over a scene's own schematic. There's no separate "floor" concept: a
 * schematic's floor is just blocks placed at its lowest layer like anything else, and {@link
 * #layer}/{@link #layersFrom} are a convenience for selecting "everything at this height" from the
 * schematic's own known size, instead of hand-writing a nested loop over its X/Z extent every time a
 * scene needs to reveal or animate a floor.
 */
public interface SelectionUtil {

    /**
     * Every block position within the scene's own known bounds (its schematic's size, or — for a scene
     * with no schematic — {@code SimpleSceneBuildingUtil}'s documented placeholder size).
     */
    Selection everywhere();

    Selection position(int x, int y, int z);

    Selection position(BlockPos pos);

    /**
     * An arbitrary, irregular set of positions in one call, in the given order — the alternative to
     * building the same shape via repeated {@code .add(position(...))} chains.
     */
    Selection positions(BlockPos... positions);

    Selection fromTo(int x, int y, int z, int x2, int y2, int z2);

    Selection fromTo(BlockPos pos1, BlockPos pos2);

    /**
     * Every position at {@code (x, z)} from {@code y=1} up, spanning the scene's own Y size again
     * counted from there — not capped at the scene's real top. Two things worth knowing about that:
     * {@code y=0} is treated as the floor a column stands ON, not part of the column itself, and the
     * column's top can sit ABOVE the scene's actual ceiling (a scene 3 blocks tall gets a column
     * reaching {@code y=1..4}, not capped at {@code y=2}) — harmless in practice since a {@link
     * Selection} is pure position math, independent of whether anything real occupies a position.
     */
    Selection column(int x, int z);

    /**
     * Every position at height {@code y}, spanning the scene's full X/Z extent — the floor helper this
     * interface's javadoc is about.
     */
    Selection layer(int y);

    Selection layersFrom(int y);

    Selection layers(int y, int height);

    Selection cuboid(BlockPos origin, Vec3i size);
}
