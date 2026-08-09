package dev.flomik.ponderlib.foundation.registration;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
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
 * avoids this by reaching into the private {@code palettes} field directly; this does the same via
 * reflection since that field has no public accessor.
 * <p>
 * The field is looked up through {@link ObfuscationReflectionHelper} rather than plain
 * {@code Class#getDeclaredField}: Forge 1.20.1's production runtime still loads a patched,
 * SRG-named {@code client-*-srg.jar}, and its live official→SRG renaming only rewrites compiled
 * bytecode field/method references, not string literals passed to reflection — a raw
 * {@code getDeclaredField("palettes")} finds the field in the Mojmap-named dev environment but
 * throws {@code NoSuchFieldException} in a real production launch.
 */
public final class SchematicLoader {

    private static final Field PALETTES_FIELD = findPalettesField();

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
            CompoundTag nbt = NbtIo.readCompressed(stream);
            StructureTemplate template = new StructureTemplate();
            template.load(BuiltInRegistries.BLOCK.asLookup(), nbt);
            return new LoadedSchematic(blocksOf(template), template.getSize());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load ponder schematic " + schematicLocation, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<StructureTemplate.StructureBlockInfo> blocksOf(StructureTemplate template) {
        List<StructureTemplate.Palette> palettes;
        try {
            palettes = (List<StructureTemplate.Palette>) PALETTES_FIELD.get(template);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not read StructureTemplate#palettes", e);
        }
        if (palettes.isEmpty()) {
            return List.of();
        }
        return new StructurePlaceSettings().getRandomPalette(palettes, BlockPos.ZERO).blocks();
    }

    private static Field findPalettesField() {
        // SRG name for StructureTemplate#palettes as of MC 1.20.1 mappings 20230612.114412 — Forge's
        // production runtime loads an SRG-named client jar and only remaps compiled bytecode field
        // references at class-load time, not string literals passed to reflection, so
        // ObfuscationReflectionHelper needs the SRG id here rather than the Mojmap name.
        try {
            return ObfuscationReflectionHelper.findField(StructureTemplate.class, "f_74482_");
        } catch (ObfuscationReflectionHelper.UnableToFindFieldException e) {
            throw new IllegalStateException("StructureTemplate#palettes field is missing - vanilla layout changed", e);
        }
    }
}
