package dev.flomik.ponderlib.foundation;

import dev.flomik.ponderlib.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SimpleSelection implements Selection {

    // A Set, not a List: a selection is conceptually a SET of positions, and add()/subtract() need
    // set semantics anyway - de-duplicating is more correct than allowing the same position twice.
    // LinkedHashSet keeps insertion order.
    private final Set<BlockPos> positions;

    public SimpleSelection(List<BlockPos> positions) {
        this.positions = new LinkedHashSet<>(positions);
    }

    private SimpleSelection(Set<BlockPos> positions) {
        this.positions = positions;
    }

    @Override
    public Vec3 getCenter() {
        double x = 0;
        double y = 0;
        double z = 0;
        for (BlockPos pos : positions) {
            x += pos.getX();
            y += pos.getY();
            z += pos.getZ();
        }
        int count = Math.max(positions.size(), 1);
        return new Vec3(x / count + 0.5, y / count + 0.5, z / count + 0.5);
    }

    // Pure functions returning a NEW selection rather than mutating this one and returning it -
    // matches this class' own immutable style and needs no defensive copying at call sites. Every
    // real usage is a single chained expression, `fromTo(...).add(position(...))`, so nothing ever
    // needs the original selection's identity to stay the same after a call.
    @Override
    public Selection add(Selection other) {
        Set<BlockPos> merged = new LinkedHashSet<>(positions);
        other.forEach(merged::add);
        return new SimpleSelection(merged);
    }

    @Override
    public Selection subtract(Selection other) {
        Set<BlockPos> remaining = new LinkedHashSet<>(positions);
        other.forEach(remaining::remove);
        return new SimpleSelection(remaining);
    }

    @Override
    public Selection copy() {
        return new SimpleSelection(new LinkedHashSet<>(positions));
    }

    @Override
    public Iterator<BlockPos> iterator() {
        return positions.iterator();
    }
}
