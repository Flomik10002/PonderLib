package dev.flomik.ponderlib.api.scene;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Passed alongside {@link SceneBuilder} into every {@link PonderStoryBoard}; helpers for building
 * scene geometry, split into three groups: {@link #select} for {@link Selection}s, {@link #vector}
 * for {@link Vec3} points, and {@link #grid} for plain {@link BlockPos} construction.
 */
public interface SceneBuildingUtil {

    SelectionUtil select();

    VectorUtil vector();

    PositionUtil grid();
}
