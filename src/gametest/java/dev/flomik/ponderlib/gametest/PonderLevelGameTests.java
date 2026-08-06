package dev.flomik.ponderlib.gametest;

import dev.flomik.ponderlib.Ponderlib;
import dev.flomik.ponderlib.foundation.PonderLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Ponderlib.MODID)
@PrefixGameTestTemplate(false)
public final class PonderLevelGameTests {

    private PonderLevelGameTests() {
    }

    @GameTest(template = "empty")
    public static void storesSceneBlocksWithoutMutatingTheRealLevel(GameTestHelper helper) {
        PonderLevel ponder = new PonderLevel(helper.getLevel());
        BlockPos scenePos = new BlockPos(2, 5, 3);
        var realBefore = helper.getLevel().getBlockState(scenePos);

        helper.assertTrue(ponder.setBlock(scenePos, Blocks.GOLD_BLOCK.defaultBlockState(), 3), "PonderLevel rejected a block update");
        helper.assertValueEqual(ponder.getBlockState(scenePos).getBlock(), Blocks.GOLD_BLOCK, "Scene block was not stored");
        helper.assertValueEqual(ponder.getBlockState(scenePos.above()).getBlock(), Blocks.AIR, "Unknown scene positions must be air");
        helper.assertValueEqual(helper.getLevel().getBlockState(scenePos), realBefore, "Scene write leaked into the real GameTest level");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void attachesAndReturnsBlockEntities(GameTestHelper helper) {
        PonderLevel ponder = new PonderLevel(helper.getLevel());
        BlockPos pos = new BlockPos(1, 2, 1);
        var state = Blocks.CHEST.defaultBlockState();
        ChestBlockEntity chest = new ChestBlockEntity(pos, state);
        ponder.setBlockDirect(pos, state);
        ponder.setBlockEntityDirect(pos, chest);

        helper.assertValueEqual(ponder.getBlockEntity(pos), chest, "Stored block entity could not be retrieved");
        helper.assertValueEqual(chest.getLevel(), ponder, "Block entity was not attached to the scene level");
        helper.assertTrue(ponder.getBlockEntity(pos.above()) == null, "Unknown block entity position was not empty");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void suppliesDeterministicSceneEnvironment(GameTestHelper helper) {
        PonderLevel ponder = new PonderLevel(helper.getLevel());

        helper.assertValueEqual(ponder.getFluidState(BlockPos.ZERO), Fluids.EMPTY.defaultFluidState(), "Scene fluid should default to empty");
        helper.assertValueEqual(ponder.getMaxLocalRawBrightness(BlockPos.ZERO), 15, "Scene should be full-bright");
        for (net.minecraft.world.level.LightLayer layer : net.minecraft.world.level.LightLayer.values()) {
            // Real query, not a hardcoded light value at the entity-render call site (see
            // renderEntities' javadoc) - getBrightness itself is what has to be flat, so
            // getPackedLightCoords resolves to full brightness rather than the real level's light
            // engine at whatever arbitrary coordinates a scene entity happens to occupy.
            helper.assertValueEqual(ponder.getBrightness(layer, BlockPos.ZERO), 15,
                "Scene brightness should be flat full-bright on layer " + layer);
        }
        for (Direction direction : Direction.values()) {
            helper.assertValueEqual(ponder.getShade(direction, true), 1.0F, "Scene shade should be flat");
        }
        helper.assertTrue(ponder.players().isEmpty(), "Fake scene must not expose real players");
        helper.assertTrue(!ponder.getBlockTicks().willTickThisTick(BlockPos.ZERO, Blocks.STONE), "Scene block tick queue should be empty");
        helper.assertTrue(!ponder.getFluidTicks().willTickThisTick(BlockPos.ZERO, Fluids.WATER), "Scene fluid tick queue should be empty");
        helper.succeed();
    }

    // Regression test for a real, user-reported bug: a chest opened via triggerEvent during one
    // playthrough stayed rendered open on a later replay/seekToTime-backward, because
    // getBlockEntity always returns the SAME long-lived instance - replaying the schedule resets
    // instructions and elements, but never touches a block entity's own internal Java state. With
    // identify mode holding scene.tick() paused, nothing ever ticked forward again to let the
    // chest close "naturally" the way normal playback would within the same or next frame.
    //
    // Can't actually tick the lid open here to observe openness go from 0 to >0 first -
    // ChestBlock#getTicker returns null unless level.isClientSide (the lid animation is
    // client-only), and PonderLevel inherits isClientSide from whatever real Level it wraps -
    // GameTest's is server-side. The fix has to work by unconditional instance replacement, not by
    // inspecting before/after state, so that's what this actually verifies: a fresh ChestBlockEntity
    // (which is all resetBlockEntities constructs, via BlockEntity#loadStatic) always starts with
    // its lid-controller state at its Java default (closed) regardless of what triggerEvent did to
    // the PREVIOUS instance - the replacement itself is what matters, not any specific NBT content.
    @GameTest(template = "empty")
    public static void resetBlockEntitiesReplacesTheStoredInstanceWithAFreshCopy(GameTestHelper helper) {
        PonderLevel ponder = new PonderLevel(helper.getLevel());
        BlockPos pos = new BlockPos(1, 2, 1);
        BlockState state = Blocks.CHEST.defaultBlockState();
        ChestBlockEntity chest = new ChestBlockEntity(pos, state);
        ponder.setBlockDirect(pos, state);
        ponder.setBlockEntityDirect(pos, chest);
        helper.assertValueEqual(chest.getOpenNess(1.0F), 0F, "Chest should start closed");

        ponder.createBackup();
        chest.triggerEvent(1, 1);
        helper.assertValueEqual(ponder.getBlockEntity(pos), chest, "Sanity check: still the same instance before reset");

        ponder.resetBlockEntities();
        BlockEntity restored = ponder.getBlockEntity(pos);
        helper.assertTrue(restored != chest, "resetBlockEntities should replace the instance, not reuse the mutated one");
        helper.assertTrue(restored instanceof ChestBlockEntity, "Restored block entity should still be a chest");
        helper.assertValueEqual(((ChestBlockEntity) restored).getOpenNess(1.0F), 0F,
            "A freshly constructed chest always starts closed, regardless of what triggerEvent did to the old instance");
        helper.succeed();
    }

    // The wiring that makes a lit furnace smoke inside a scene, verified end to end on the real
    // block: FurnaceBlock#animateTick -> level.addParticle -> PonderLevel's own override -> the
    // installed PonderParticleSink. Runs server-side because every type involved is dist-neutral -
    // animateTick carries no @OnlyIn, ParticleTypes/ParticleOptions are what the server sends to
    // clients anyway, and PonderParticleSink exists precisely so PonderLevel never has to name a
    // client-only type (see that interface's javadoc). That also makes this test the guard for that
    // dist boundary: if PonderLevel ever gains a direct client-class reference, this whole class
    // stops loading here.
    @GameTest(template = "empty")
    public static void litFurnaceEmitsItsOwnParticlesThroughTheScenesSink(GameTestHelper helper) {
        PonderLevel ponder = new PonderLevel(helper.getLevel());
        BlockPos pos = new BlockPos(1, 1, 1);
        List<ParticleOptions> spawned = new ArrayList<>();
        ponder.setParticleSink((options, x, y, z, mx, my, mz) -> spawned.add(options));

        BlockState unlit = Blocks.FURNACE.defaultBlockState()
            .setValue(FurnaceBlock.FACING, Direction.SOUTH)
            .setValue(FurnaceBlock.LIT, false);
        ponder.setBlockDirect(pos, unlit);
        // animateTick is the vanilla hook, and FurnaceBlock's own implementation gates every particle
        // behind LIT - so an unlit furnace must stay completely silent no matter how often it runs.
        for (int i = 0; i < 20; i++) {
            unlit.getBlock().animateTick(unlit, ponder, pos, RandomSource.create(i));
        }
        helper.assertValueEqual(spawned.size(), 0, "An unlit furnace must not emit any particles");

        BlockState lit = unlit.setValue(FurnaceBlock.LIT, true);
        ponder.setBlockDirect(pos, lit);
        lit.getBlock().animateTick(lit, ponder, pos, RandomSource.create(1L));

        // FurnaceBlock.animateTick emits exactly one SMOKE and one FLAME per call when lit.
        helper.assertValueEqual(spawned.size(), 2, "A lit furnace should emit one smoke and one flame per animateTick");
        helper.assertTrue(spawned.contains(ParticleTypes.SMOKE), "Lit furnace should have emitted smoke");
        helper.assertTrue(spawned.contains(ParticleTypes.FLAME), "Lit furnace should have emitted flame");
        helper.succeed();
    }

    // A PonderLevel with no sink installed (exactly what the GameTests above build) must swallow
    // particles rather than NPE - it stands in for the vanilla Level#addParticle no-op stub it
    // overrides, and nothing outside PonderScene ever installs one.
    @GameTest(template = "empty")
    public static void particlesAreDroppedWhenNoSinkIsInstalled(GameTestHelper helper) {
        PonderLevel ponder = new PonderLevel(helper.getLevel());

        ponder.addParticle(ParticleTypes.FLAME, 0.5, 0.5, 0.5, 0, 0, 0);
        ponder.addAlwaysVisibleParticle(ParticleTypes.SMOKE, 0.5, 0.5, 0.5, 0, 0, 0);

        helper.succeed();
    }

    // Entity tracking, verified server-side against the real Entity#tick (item gravity/physics) -
    // renderEntities itself needs a client EntityRenderDispatcher and stays untested here for the
    // same dist reason litFurnaceEmitsItsOwnParticlesThroughTheScenesSink's comment explains.
    @GameTest(template = "empty")
    public static void tracksAndTicksEntitiesWithoutLeakingIntoTheRealLevel(GameTestHelper helper) {
        PonderLevel ponder = new PonderLevel(helper.getLevel());
        ItemEntity item = new ItemEntity(ponder, 0.5, 5, 0.5, new ItemStack(Items.STICK));

        helper.assertTrue(ponder.addFreshEntity(item), "addFreshEntity should start tracking the entity");
        helper.assertTrue(ponder.getSceneEntities().contains(item), "Entity should be in the scene's own list");
        helper.assertTrue(helper.getLevel().getEntities(item, item.getBoundingBox()).isEmpty(),
            "Scene entity must not leak into the real GameTest level");

        ponder.tickEntities();
        helper.assertValueEqual(item.tickCount, 1, "tickEntities should tick every entity it tracks");

        ponder.clearEntities();
        helper.assertTrue(ponder.getSceneEntities().isEmpty(), "clearEntities should drop every tracked entity");
        helper.succeed();
    }

    // Ported from Catnip's SchematicLevel#addFreshEntity/ComponentProcessors literally: an item
    // frame's displayed item can come from any mod (see WorldInstructions#createEntity's javadoc),
    // so its components get stripped down to the small allowlist before the frame ever gets shown.
    @GameTest(template = "empty")
    public static void addFreshEntityStripsUnsafeComponentsFromAnItemFramesContents(GameTestHelper helper) {
        PonderLevel ponder = new PonderLevel(helper.getLevel());
        ItemStack stack = new ItemStack(Items.DIAMOND);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Kept"));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag()));
        ItemFrame frame = new ItemFrame(ponder, BlockPos.ZERO, Direction.NORTH);
        frame.setItem(stack, false);

        ponder.addFreshEntity(frame);

        ItemStack sanitized = frame.getItem();
        helper.assertTrue(sanitized.has(DataComponents.CUSTOM_NAME),
            "Allowlisted component (custom name) should survive sanitizing");
        helper.assertTrue(!sanitized.has(DataComponents.CUSTOM_DATA),
            "Non-allowlisted component (arbitrary custom NBT) should be stripped");
        helper.succeed();
    }

    // Ported from real PonderLevel#tick()'s "y <= -0.5 -> discard" cleanup: an entity that falls out
    // of the scene's own small bounds (nothing below it to land on, unlike a real world) is removed
    // instead of free-falling forever.
    @GameTest(template = "empty")
    public static void tickEntitiesDiscardsWhateverFallsOutOfTheScene(GameTestHelper helper) {
        PonderLevel ponder = new PonderLevel(helper.getLevel());
        ItemEntity item = new ItemEntity(ponder, 0.5, -1, 0.5, new ItemStack(Items.STICK));
        ponder.addFreshEntity(item);

        ponder.tickEntities();

        helper.assertTrue(!ponder.getSceneEntities().contains(item),
            "An entity that fell below the scene should be discarded and removed on the very next tick");
        helper.succeed();
    }

    // VirtualBlockView is covered by a plain JUnit test (src/test/.../render/VirtualBlockViewTest)
    // instead of a GameTest: it's a client-only rendering helper, and merely loading its class here
    // pulls in net.minecraft.client.multiplayer.ClientLevel, which NeoForge's RuntimeDistCleaner
    // refuses to load on the DEDICATED_SERVER dist that GameTest runs under.
}
