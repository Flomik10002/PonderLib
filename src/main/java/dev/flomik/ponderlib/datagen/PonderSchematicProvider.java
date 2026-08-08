package dev.flomik.ponderlib.datagen;

import net.minecraft.core.BlockPos;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Writes this mod's own demo ponder schematics as real, loadable vanilla structure-template NBT —
 * under {@code assets/ponderlib/ponder/<path>.nbt} (a resource, not a datapack structure - see
 * {@code foundation.registration.SchematicLoader}, which reads it back the same way a real
 * consumer's own {@code assets/<modid>/ponder/...} schematic gets read). Run via {@code ./gradlew
 * runData}.
 * <p>
 * The NBT shape (`size`/`entities`/`blocks`/`palette`, one palette entry per distinct {@link
 * BlockState}) is vanilla's own structure-template format, hand-built here rather than obtained by
 * populating a real {@code StructureTemplate} and calling its own {@code save} - datagen has no
 * live {@code Level} to capture from, and {@code StructureTemplate}'s only public way to gain
 * content ({@code fillFromWorld}) needs one. Round-tripped through the real vanilla {@code
 * StructureTemplate#load} in {@code PonderSchematicProviderTest} to confirm it parses correctly,
 * the same contract {@link PonderTestStructureProvider} already relies on for its own (trivially
 * empty) file.
 */
public class PonderSchematicProvider implements DataProvider {

    private final PackOutput.PathProvider pathProvider;

    public PonderSchematicProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "ponder");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        // No floor at all, deliberately - see this class' own javadoc on why oak_planks/single
        // stays bare: it's the minimal GameTest structure template (build.gradle's
        // processGameTestResources), not one of the actual demo scenes below.
        futures.add(write(output, "oak_planks/single", Map.of(BlockPos.ZERO, Blocks.OAK_PLANKS.defaultBlockState()), 1, 1, 1));

        Map<BlockPos, BlockState> oakPlanksFloor = withCheckerboardFloor(3, 3, new BlockPos(1, 1, 1), Blocks.OAK_PLANKS.defaultBlockState());
        futures.add(write(output, "oak_planks/floor", oakPlanksFloor, 3, 2, 3));

        Map<BlockPos, BlockState> chestFloor = withCheckerboardFloor(3, 3, new BlockPos(1, 1, 1), Blocks.CHEST.defaultBlockState());
        futures.add(write(output, "chest/floor", chestFloor, 3, 2, 3));

        Map<BlockPos, BlockState> furnaceFloor = withCheckerboardFloor(3, 3, new BlockPos(1, 1, 1), Blocks.FURNACE.defaultBlockState());
        futures.add(write(output, "furnace/unlit", furnaceFloor, 3, 2, 3));

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    /**
     * A y=0 floor of {@code sizeX} x {@code sizeZ}, alternating {@code minecraft:white_concrete}/
     * {@code minecraft:light_gray_concrete} by the parity of {@code (x + z)} - the same checkerboard
     * real Create's own ponder schematics bake by hand into every one of their NBT files
     * (`white_concrete`/`snow_block` there; `light_gray_concrete` here instead of `snow_block`, by
     * explicit choice rather than matching the original exactly), plus one subject block placed at
     * {@code subjectPos} on top of it.
     */
    static Map<BlockPos, BlockState> withCheckerboardFloor(int sizeX, int sizeZ, BlockPos subjectPos, BlockState subject) {
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                BlockState floorState = (x + z) % 2 == 0
                    ? Blocks.WHITE_CONCRETE.defaultBlockState()
                    : Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
                blocks.put(new BlockPos(x, 0, z), floorState);
            }
        }
        blocks.put(subjectPos, subject);
        return blocks;
    }

    private CompletableFuture<?> write(CachedOutput output, String path, Map<BlockPos, BlockState> blocks,
                                        int sizeX, int sizeY, int sizeZ) {
        CompoundTag root = buildStructureNbt(blocks, sizeX, sizeY, sizeZ);

        Path target = pathProvider.file(ResourceLocation.fromNamespaceAndPath("ponderlib", path), "nbt");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            NbtIo.writeCompressed(root, bytes);
            byte[] data = bytes.toByteArray();
            HashCode hash = Hashing.sha1().hashBytes(data);
            output.writeIfNeeded(target, data, hash);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * The actual vanilla structure-template NBT shape (see this class' own javadoc) - split out
     * from {@link #write} so {@code PonderSchematicProviderTest} can round-trip this tag through
     * the real {@code StructureTemplate#load} directly, with no {@link CachedOutput}/file-system
     * machinery in the way.
     */
    static CompoundTag buildStructureNbt(Map<BlockPos, BlockState> blocks, int sizeX, int sizeY, int sizeZ) {
        // A List, not a Set, so palette index lookups below are stable/repeatable - LinkedHashSet
        // only to de-duplicate while preserving first-seen order.
        List<BlockState> palette = new ArrayList<>(new LinkedHashSet<>(blocks.values()));

        ListTag paletteTag = new ListTag();
        for (BlockState state : palette) {
            paletteTag.add(NbtUtils.writeBlockState(state));
        }

        ListTag blocksTag = new ListTag();
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            CompoundTag blockTag = new CompoundTag();
            blockTag.put("pos", intList(pos.getX(), pos.getY(), pos.getZ()));
            blockTag.putInt("state", palette.indexOf(entry.getValue()));
            blocksTag.add(blockTag);
        }

        CompoundTag root = new CompoundTag();
        root.put("size", intList(sizeX, sizeY, sizeZ));
        root.put("entities", new ListTag());
        root.put("blocks", blocksTag);
        root.put("palette", paletteTag);
        NbtUtils.addCurrentDataVersion(root);
        return root;
    }

    private static ListTag intList(int... values) {
        ListTag tag = new ListTag();
        for (int value : values) {
            tag.add(IntTag.valueOf(value));
        }
        return tag;
    }

    @Override
    public String getName() {
        return "PonderLib Demo Schematics";
    }
}
