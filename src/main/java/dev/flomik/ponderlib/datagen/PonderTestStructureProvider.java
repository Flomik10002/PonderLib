package dev.flomik.ponderlib.datagen;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Writes a single, genuinely empty structure-template NBT (no blocks, no entities) directly under
 * {@code data/ponderlib/structures/empty.nbt} (plural - 1.20.1's datapack convention, renamed
 * singular only in 1.21+) — the test suite's own {@code @GameTest(template = "empty")} annotations
 * load exactly this file. Run via {@code ./gradlew runData}.
 */
public class PonderTestStructureProvider implements DataProvider {

    private final PackOutput.PathProvider pathProvider;

    public PonderTestStructureProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "structures");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        CompoundTag root = new CompoundTag();
        root.put("size", intList(1, 1, 1));
        root.put("entities", new ListTag());
        root.put("blocks", new ListTag());
        root.put("palette", new ListTag());
        NbtUtils.addCurrentDataVersion(root);

        Path target = pathProvider.file(ResourceLocation.fromNamespaceAndPath("ponderlib", "empty"), "nbt");
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

    private static ListTag intList(int... values) {
        ListTag tag = new ListTag();
        for (int value : values) {
            tag.add(IntTag.valueOf(value));
        }
        return tag;
    }

    @Override
    public String getName() {
        return "PonderLib Test Structures";
    }
}
