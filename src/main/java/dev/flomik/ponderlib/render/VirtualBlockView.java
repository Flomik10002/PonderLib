package dev.flomik.ponderlib.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

/**
 * A minimal {@link BlockAndTintGetter} holding a single {@link BlockState} in isolation, used to
 * bake a block's model outside of any real level (so context-sensitive models still get valid
 * neighbor/light/tint queries instead of crashing on a null level).
 */
public class VirtualBlockView implements BlockAndTintGetter {

    private final BlockPos pos;
    private final BlockState state;

    public VirtualBlockView(BlockPos pos, BlockState state) {
        this.pos = pos;
        this.state = state;
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return pos.equals(this.pos) ? state : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return pos.equals(this.pos) ? state.getFluidState()
            : net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        // Flat, unshaded - directional shading is left entirely to
        // RenderSystem.setupLevelDiffuseLighting at render time instead of being baked into the
        // vertex colors here.
        return 1.0F;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        throw new UnsupportedOperationException("VirtualBlockView has no light engine, use getBrightness/getRawBrightness");
    }

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        return 15;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int amount) {
        return 15;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        // Matches the real SchematicLevel.getBiome: resolve tint (grass/leaves/water color etc.)
        // as if the block were standing in a plains biome, since a Ponder scene has no real
        // biome of its own. Baking only ever happens while a real world is loaded (from the
        // tooltip trigger), so Minecraft.getInstance().level is always available here.
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return -1;
        }
        Biome biome = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS).value();
        return resolver.getColor(biome, pos.getX(), pos.getZ());
    }

    @Override
    public int getHeight() {
        return 384;
    }

    @Override
    public int getMinBuildHeight() {
        return -64;
    }
}
