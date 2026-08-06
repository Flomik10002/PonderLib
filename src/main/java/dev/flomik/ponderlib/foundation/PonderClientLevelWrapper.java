package dev.flomik.ponderlib.foundation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * A real {@link ClientLevel} whose block/light/entity queries all delegate to a wrapped fake
 * {@link PonderLevel} instead of the real world - exists purely so real vanilla {@code
 * net.minecraft.client.particle.ParticleProvider}s (which require an actual {@code ClientLevel},
 * not just any {@code Level}) can construct real {@code Particle} objects for a scene's own fake
 * level (see {@link PonderSceneParticles#addParticle}).
 * <p>
 * Two of {@code ClientLevel}'s constructor arguments aren't otherwise reachable without reflection
 * (the server's chunk radius, and the biome-zoom seed), so this substitutes plain stand-ins
 * instead: the client's own render-distance option for the chunk radius (only ever fed into a
 * {@code ClientChunkCache} this class never queries - {@link #getChunkForCollisions} and friends
 * below all delegate to the wrapped level instead) and {@code 0L} for the biome-zoom seed (only
 * affects the noise pattern used to smooth biome-tint blending at block boundaries - not a
 * correctness issue worth chasing down).
 */
public class PonderClientLevelWrapper extends ClientLevel {

    private final PonderLevel wrapped;

    private PonderClientLevelWrapper(PonderLevel wrapped, Minecraft minecraft) {
        super(
            minecraft.getConnection(),
            minecraft.level.getLevelData(),
            wrapped.dimension(),
            wrapped.dimensionTypeRegistration(),
            minecraft.options.renderDistance().get(),
            minecraft.level.getServerSimulationDistance(),
            wrapped.getProfilerSupplier(),
            minecraft.levelRenderer,
            wrapped.isDebug(),
            0L
        );
        this.wrapped = wrapped;
    }

    /**
     * {@code minecraft.getConnection()} requires the player to actually be connected to a world -
     * always true whenever a {@code PonderUI} could even be open (opening one requires already
     * being in a world), so no null-check/fallback here.
     */
    public static PonderClientLevelWrapper of(PonderLevel wrapped) {
        return new PonderClientLevelWrapper(wrapped, Minecraft.getInstance());
    }

    @Override
    public boolean hasChunkAt(BlockPos pos) {
        return wrapped.hasChunkAt(pos);
    }

    @Override
    public boolean isLoaded(BlockPos pos) {
        return wrapped.isLoaded(pos);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return wrapped.getBlockState(pos);
    }

    @Override
    public BlockGetter getChunkForCollisions(int x, int z) {
        return wrapped.getChunkForCollisions(x, z);
    }

    @Override
    public int getBrightness(LightLayer type, BlockPos pos) {
        return wrapped.getBrightness(type, pos);
    }

    @Override
    public int getLightEmission(BlockPos pos) {
        return wrapped.getLightEmission(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return wrapped.getFluidState(pos);
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return wrapped.getBlockEntity(pos);
    }
}
