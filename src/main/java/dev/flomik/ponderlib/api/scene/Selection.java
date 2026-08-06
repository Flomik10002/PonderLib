package dev.flomik.ponderlib.api.scene;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * An abstract region of block positions. {@link #add}/{@link #subtract}/{@link #copy} let scenes
 * build up an irregular shape from simpler pieces (see {@link SelectionUtil}'s builders), e.g.
 * {@code fromTo(...).add(position(...))}.
 */
public interface Selection extends Iterable<BlockPos> {

    Vec3 getCenter();

    /**
     * Every position in either selection, as a new {@link Selection} — this and {@code other} are left
     * unchanged.
     */
    Selection add(Selection other);

    /**
     * Every position in this selection that is NOT also in {@code other}, as a new {@link Selection}.
     */
    Selection subtract(Selection other);

    /**
     * An independent copy — mutating one of {@link #add}/{@link #subtract}'s inputs afterwards can't
     * affect a copy taken before the change (moot for the current immutable implementation, but real
     * Ponder's own {@code Selection} is mutable, and a future implementation backed by a live level
     * selection could be too).
     */
    Selection copy();
}
