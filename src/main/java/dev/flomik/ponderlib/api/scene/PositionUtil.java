package dev.flomik.ponderlib.api.scene;

import net.minecraft.core.BlockPos;

/**
 * A one-line {@code BlockPos} constructor, mostly useful for reading like the rest of {@link
 * SceneBuildingUtil}'s fluent calls ({@code util.grid().at(1, 0, 1)}) rather than for anything
 * {@code new BlockPos(1, 0, 1)} couldn't do.
 */
public interface PositionUtil {

    BlockPos at(int x, int y, int z);

    BlockPos zero();
}
