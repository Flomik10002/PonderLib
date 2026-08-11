package dev.flomik.ponderlib.foundation.registration;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;

/**
 * Loads a vanilla structure-template NBT schematic and returns its raw block content directly,
 * without placing it into any level — placing it into a real {@code PonderLevel} is {@code
 * PonderScene#compile}'s job, done right after this returns.
 * <p>
 * There is no public, unfiltered "give me every block" method on {@link StructureTemplate}:
 * {@link StructureTemplate#filterBlocks} always filters by a specific {@code Block}, and passing
 * {@code null} for "no filter" crashes with an NPE ({@code Palette}'s internal cache is a
 * {@code ConcurrentHashMap}, which rejects null keys). {@link StructureTemplate#placeInWorld}
 * avoids this by reaching into the private {@code palettes} field directly; {@code
 * META-INF/accesstransformer.cfg} widens that same field so this can do it as a plain field read,
 * with no reflection and no runtime name lookup.
 */
public final class SchematicLoader {

    private SchematicLoader() {
    }

    /**
     * A schematic's block content plus its own declared {@code size} — needed for
     * {@code api.scene.SelectionUtil}'s floor/layer/column helpers (see
     * {@code foundation.PonderSceneBuildingUtil}), which select "everything at this height" from the
     * scene's known extent rather than a bounding box recomputed from wherever non-air blocks happen to
     * sit (a schematic can have air padding at its edges, which would otherwise shrink the selectable
     * area).
     */
    public record LoadedSchematic(List<StructureTemplate.StructureBlockInfo> blocks, Vec3i size) {

        static final LoadedSchematic EMPTY = new LoadedSchematic(List.of(), Vec3i.ZERO);
    }

    public static LoadedSchematic load(ResourceLocation schematicLocation) {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        Optional<Resource> resource = resourceManager.getResource(schematicLocation);
        if (resource.isEmpty()) {
            return LoadedSchematic.EMPTY;
        }

        try (InputStream stream = resource.get().open()) {
            CompoundTag nbt = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
            StructureTemplate template = new StructureTemplate();
            template.load(BuiltInRegistries.BLOCK.asLookup(), nbt);
            return new LoadedSchematic(blocksOf(template), template.getSize());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load ponder schematic " + schematicLocation, e);
        }
    }

    private static List<StructureTemplate.StructureBlockInfo> blocksOf(StructureTemplate template) {
        List<StructureTemplate.Palette> palettes = template.palettes;
        if (palettes.isEmpty()) {
            return List.of();
        }
        return new StructurePlaceSettings().getRandomPalette(palettes, BlockPos.ZERO).blocks();
    }
}
