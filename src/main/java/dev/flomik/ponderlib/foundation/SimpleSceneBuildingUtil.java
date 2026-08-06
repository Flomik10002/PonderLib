package dev.flomik.ponderlib.foundation;

import dev.flomik.ponderlib.api.scene.PositionUtil;
import dev.flomik.ponderlib.api.scene.SceneBuildingUtil;
import dev.flomik.ponderlib.api.scene.Selection;
import dev.flomik.ponderlib.api.scene.SelectionUtil;
import dev.flomik.ponderlib.api.scene.VectorUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Three inner classes, one per {@link SceneBuildingUtil} builder group. Every one of {@link
 * SelectionUtil}'s cuboid-shaped selections is built from the one {@link #cuboid} primitive - see
 * that method's javadoc for the exact formulas.
 */
public class SimpleSceneBuildingUtil implements SceneBuildingUtil {

    /**
     * The size a scene with no real schematic gets (see {@code PonderScene#compile(PonderStoryBoard)} —
     * tests and the direct-API path never load one). 16 blocks a side is a generous guess, not a real
     * measurement; {@code everywhere()}/{@code layer(...)} etc. degrade gracefully either way (they just
     * select from a placeholder volume instead of a real one), so this only matters for a storyboard
     * that leans on this class' bounds-aware helpers without a real schematic backing it.
     */
    private static final Vec3i DEFAULT_SIZE = new Vec3i(16, 16, 16);

    private final SelectionUtil select;
    private final VectorUtil vector = new SimpleVectorUtil();
    private final PositionUtil grid = new SimplePositionUtil();

    public SimpleSceneBuildingUtil() {
        this(DEFAULT_SIZE);
    }

    /**
     * @param sceneSize the schematic's own declared size (see
     *                  {@code registration.SchematicLoader.LoadedSchematic#size}) — block positions in a
     *                  loaded schematic are always local to origin (0, 0, 0), so this alone is enough to
     *                  know the scene's full extent.
     */
    public SimpleSceneBuildingUtil(Vec3i sceneSize) {
        this.select = new SimpleSelectionUtil(sceneSize);
    }

    @Override
    public SelectionUtil select() {
        return select;
    }

    @Override
    public VectorUtil vector() {
        return vector;
    }

    @Override
    public PositionUtil grid() {
        return grid;
    }

    private static final class SimplePositionUtil implements PositionUtil {

        @Override
        public BlockPos at(int x, int y, int z) {
            return new BlockPos(x, y, z);
        }

        @Override
        public BlockPos zero() {
            return BlockPos.ZERO;
        }
    }

    private static final class SimpleVectorUtil implements VectorUtil {

        @Override
        public Vec3 centerOf(int x, int y, int z) {
            return centerOf(new BlockPos(x, y, z));
        }

        @Override
        public Vec3 centerOf(BlockPos pos) {
            return new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        }

        @Override
        public Vec3 topOf(int x, int y, int z) {
            return topOf(new BlockPos(x, y, z));
        }

        @Override
        public Vec3 topOf(BlockPos pos) {
            return blockSurface(pos, Direction.UP);
        }

        @Override
        public Vec3 blockSurface(BlockPos pos, Direction face) {
            return blockSurface(pos, face, 0);
        }

        @Override
        public Vec3 blockSurface(BlockPos pos, Direction face, float margin) {
            return centerOf(pos).add(
                face.getStepX() * (0.5 + margin),
                face.getStepY() * (0.5 + margin),
                face.getStepZ() * (0.5 + margin));
        }

        @Override
        public Vec3 of(double x, double y, double z) {
            return new Vec3(x, y, z);
        }
    }

    private static final class SimpleSelectionUtil implements SelectionUtil {

        private final Vec3i sceneSize;

        private SimpleSelectionUtil(Vec3i sceneSize) {
            this.sceneSize = sceneSize;
        }

        @Override
        public Selection everywhere() {
            return cuboid(BlockPos.ZERO, new Vec3i(sceneSize.getX() - 1, sceneSize.getY() - 1, sceneSize.getZ() - 1));
        }

        @Override
        public Selection position(int x, int y, int z) {
            return position(new BlockPos(x, y, z));
        }

        @Override
        public Selection position(BlockPos pos) {
            return cuboid(pos, Vec3i.ZERO);
        }

        @Override
        public Selection positions(BlockPos... positions) {
            return new SimpleSelection(List.of(positions));
        }

        @Override
        public Selection fromTo(int x, int y, int z, int x2, int y2, int z2) {
            return fromTo(new BlockPos(x, y, z), new BlockPos(x2, y2, z2));
        }

        @Override
        public Selection fromTo(BlockPos pos1, BlockPos pos2) {
            return cuboid(pos1, pos2.subtract(pos1));
        }

        @Override
        public Selection column(int x, int z) {
            // y=1 is the column's start - see SelectionUtil#column's javadoc for why.
            return cuboid(new BlockPos(x, 1, z), new Vec3i(0, sceneSize.getY(), 0));
        }

        @Override
        public Selection layer(int y) {
            return layers(y, 1);
        }

        @Override
        public Selection layersFrom(int y) {
            return layers(y, sceneSize.getY() - y);
        }

        @Override
        public Selection layers(int y, int height) {
            return cuboid(new BlockPos(0, y, 0), new Vec3i(
                sceneSize.getX() - 1,
                Math.min(sceneSize.getY() - y, height) - 1,
                sceneSize.getZ() - 1));
        }

        /**
         * The one primitive everything above is built from: normalizes {@code origin}/{@code
         * origin.offset(size)} into min/max corners (so a negative size, or {@code fromTo} given
         * corners in either order, both work) and enumerates every position in that box directly
         * into a {@link SimpleSelection}. Scenes are small enough (a handful of blocks to a few
         * hundred) that materializing every position costs nothing worth avoiding, and it lets
         * {@link Selection#add}/{@link Selection#subtract} work uniformly on every selection this
         * class produces.
         */
        @Override
        public Selection cuboid(BlockPos origin, Vec3i size) {
            BlockPos corner = origin.offset(size);
            int minX = Math.min(origin.getX(), corner.getX());
            int maxX = Math.max(origin.getX(), corner.getX());
            int minY = Math.min(origin.getY(), corner.getY());
            int maxY = Math.max(origin.getY(), corner.getY());
            int minZ = Math.min(origin.getZ(), corner.getZ());
            int maxZ = Math.max(origin.getZ(), corner.getZ());

            List<BlockPos> positions = new ArrayList<>();
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        positions.add(new BlockPos(x, y, z));
                    }
                }
            }
            return new SimpleSelection(positions);
        }
    }
}
