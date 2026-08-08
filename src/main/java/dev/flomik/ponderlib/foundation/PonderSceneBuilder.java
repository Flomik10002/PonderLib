package dev.flomik.ponderlib.foundation;

import dev.flomik.ponderlib.api.element.ElementLink;
import dev.flomik.ponderlib.api.element.EntityElement;
import dev.flomik.ponderlib.api.element.PonderElement;
import dev.flomik.ponderlib.api.element.WorldSectionElement;
import dev.flomik.ponderlib.api.ParticleEmitter;
import dev.flomik.ponderlib.api.scene.EffectInstructions;
import dev.flomik.ponderlib.api.PonderPalette;
import dev.flomik.ponderlib.api.Pointing;
import dev.flomik.ponderlib.api.scene.InputElementBuilder;
import dev.flomik.ponderlib.api.scene.OverlayInstructions;
import dev.flomik.ponderlib.api.scene.SceneBuilder;
import dev.flomik.ponderlib.api.scene.Selection;
import dev.flomik.ponderlib.api.scene.TextElementBuilder;
import dev.flomik.ponderlib.api.scene.WorldInstructions;
import dev.flomik.ponderlib.foundation.element.BoundingBoxOutlineElement;
import dev.flomik.ponderlib.foundation.element.EntityElementImpl;
import dev.flomik.ponderlib.foundation.element.InputIconElement;
import dev.flomik.ponderlib.foundation.element.InputWindowElement;
import dev.flomik.ponderlib.foundation.element.LineElement;
import dev.flomik.ponderlib.foundation.element.OutlineElement;
import dev.flomik.ponderlib.foundation.element.TextWindowElement;
import dev.flomik.ponderlib.foundation.element.WorldSectionElementImpl;
import dev.flomik.ponderlib.foundation.instruction.AnimateElementInstruction;
import dev.flomik.ponderlib.foundation.instruction.BoundingBoxOutlineInstruction;
import dev.flomik.ponderlib.foundation.instruction.DelayInstruction;
import dev.flomik.ponderlib.foundation.instruction.EmitParticlesInstruction;
import dev.flomik.ponderlib.foundation.instruction.HideSectionInstruction;
import dev.flomik.ponderlib.foundation.instruction.InputIconInstruction;
import dev.flomik.ponderlib.foundation.instruction.KeyframeInstruction;
import dev.flomik.ponderlib.foundation.instruction.InputWindowInstruction;
import dev.flomik.ponderlib.foundation.instruction.LineInstruction;
import dev.flomik.ponderlib.foundation.instruction.MarkAsFinishedInstruction;
import dev.flomik.ponderlib.foundation.instruction.OutlineInstruction;
import dev.flomik.ponderlib.foundation.instruction.PonderInstruction;
import dev.flomik.ponderlib.foundation.instruction.RevealSectionInstruction;
import dev.flomik.ponderlib.foundation.instruction.TextInstruction;
import dev.flomik.ponderlib.foundation.registration.PonderLocalization;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Non-final on purpose — see {@link SceneBuilder}'s javadoc. A mod can subclass this to layer its
 * own instruction vocabulary (its own {@code WorldInstructions}/{@code OverlayInstructions}
 * implementations calling back into the protected {@link #scene}) on top of this engine.
 */
public class PonderSceneBuilder implements SceneBuilder {

    /**
     * Ticks a revealed section takes to fade in (see {@link RevealSectionInstruction}) - about half
     * a second at 20 ticks/sec. {@code showSection} has no duration parameter in its public
     * signature, so this is a fixed default rather than something a storyboard can tune per call.
     */
    private static final int SECTION_FADE_TICKS = 20;

    /** {@code OverlayInstructions#showLine}'s edge-box thickness, matching {@code OutlineElement}'s. */
    private static final float LINE_THICKNESS = 0.05F;

    /** {@code OverlayInstructions#showBigLine}'s edge-box thickness - visibly the "main subject" weight. */
    private static final float BIG_LINE_THICKNESS = 0.125F;

    protected final PonderScene scene;
    private final WorldInstructions worldInstructions = new WorldInstructionsImpl();
    private final OverlayInstructions overlayInstructions = new OverlayInstructionsImpl();
    private final EffectInstructions effectInstructions = new EffectInstructionsImpl();

    /**
     * {@code null} for scenes with no known registering mod (e.g. {@link PonderScene#compile(PonderStoryBoard)}'s
     * raw, unregistered path) - title/text then stay plain {@link Component#literal} with no
     * translation key, since there's no {@code <modid>.ponder....} namespace to put one under.
     */
    private final String modId;
    private String sceneId = "";
    private int textIndex;

    public PonderSceneBuilder(PonderScene scene) {
        this(scene, null);
    }

    public PonderSceneBuilder(PonderScene scene, String modId) {
        this.scene = scene;
        this.modId = modId;
    }

    /**
     * Builds a real, live-translatable component when this scene has a known mod namespace (real
     * play, via {@link PonderScene#compile(StoryBoardEntry)}), falling back to a fixed literal
     * otherwise - {@code translatableWithFallback} always displays correctly even before/without a
     * generated lang file, so this never needs the raw {@code fallback} text to also be baked in
     * separately (see {@code datagen.PonderLangProvider} for where the lang file itself comes from).
     */
    private Component component(String key, String fallback) {
        return key == null ? Component.literal(fallback) : Component.translatableWithFallback(key, fallback);
    }

    private String titleKey(String sceneId) {
        return modId == null ? null : PonderLocalization.keyForTitle(modId, sceneId);
    }

    private String textKey(String sceneId, int index) {
        return modId == null ? null : PonderLocalization.keyForText(modId, sceneId, index);
    }

    @Override
    public WorldInstructions world() {
        return worldInstructions;
    }

    @Override
    public OverlayInstructions overlay() {
        return overlayInstructions;
    }

    @Override
    public EffectInstructions effects() {
        return effectInstructions;
    }

    @Override
    public PonderScene getScene() {
        return scene;
    }

    @Override
    public void title(String sceneId, String title) {
        this.sceneId = sceneId;
        scene.setTitle(component(titleKey(sceneId), title));
    }

    @Override
    public void configureBasePlate(int xOffset, int zOffset, int basePlateSize) {
        scene.configureBasePlateBounds(xOffset, xOffset + basePlateSize, zOffset, zOffset + basePlateSize);
    }

    @Override
    public void showBasePlate() {
        createSection(basePlateSelection(), Direction.UP, true);
    }

    /**
     * Shared by {@link WorldInstructionsImpl#showSection} and {@link #showBasePlate}, which needs
     * to mark its own section as the base plate (see {@link WorldSectionElementImpl#setBasePlate})
     * before it's ever added to the scene - something the public {@link WorldInstructions#showSection}
     * signature has no way to express.
     */
    private ElementLink<WorldSectionElement> createSection(Selection selection, Direction direction, boolean basePlate) {
        WorldSectionElementImpl element = new WorldSectionElementImpl(selection);
        element.setBasePlate(basePlate);
        ElementLink<WorldSectionElement> link = new SimpleElementLink<>(WorldSectionElement.class);

        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (BlockPos pos : selection) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX() + 1);
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ() + 1);
        }
        addInstruction(new RevealSectionInstruction(element, link, direction, SECTION_FADE_TICKS,
            selection.getCenter(), minX, maxX, minZ, maxZ));
        return link;
    }

    // Package-private (not private) so PonderSceneBuilderTest can verify the cuboid math directly,
    // the same reasoning as PonderScene#getSchedule()'s visibility.
    Selection basePlateSelection() {
        int minX = (int) scene.getBasePlateMinX();
        int maxX = (int) scene.getBasePlateMaxX();
        int minZ = (int) scene.getBasePlateMinZ();
        int maxZ = (int) scene.getBasePlateMaxZ();

        List<BlockPos> positions = new ArrayList<>();
        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                positions.add(new BlockPos(x, 0, z));
            }
        }
        return new SimpleSelection(positions);
    }

    /**
     * Every currently-visible {@link WorldSectionElementImpl} that has {@code pos} among its own
     * captured positions - shared by {@code modifyBlock} (push a state change into whichever
     * section(s) already display it) and {@code hideSection} (match a raw {@link Selection} back to
     * the element that owns it).
     */
    private static List<WorldSectionElementImpl> sectionsContaining(PonderScene scene, BlockPos pos) {
        List<WorldSectionElementImpl> result = new ArrayList<>();
        for (PonderElement element : scene.getElements()) {
            if (element instanceof WorldSectionElementImpl section && section.isVisible()
                && section.getBlockPositions().contains(pos)) {
                result.add(section);
            }
        }
        return result;
    }

    /**
     * The block currently shown at {@code pos}: an already-revealed section's own captured state if
     * one exists (what the player actually sees), falling back to the scene's virtual world
     * otherwise (a position never shown yet, or restored/overwritten since).
     */
    private static BlockState currentDisplayedState(PonderScene scene, BlockPos pos) {
        for (WorldSectionElementImpl section : sectionsContaining(scene, pos)) {
            BlockState state = section.getBlockState(pos);
            if (state != null) {
                return state;
            }
        }
        return scene.getBlockState(pos);
    }

    /**
     * A short-lived burst of the given block's own break-particle look (vanilla's {@code
     * ParticleTypes.BLOCK}), scattered across the block's own space - shared by {@code
     * destroyBlock}/{@code setBlock(..., true)}/{@code modifyBlock(..., true)} and the particle cue
     * half of {@code incrementBlockBreakingProgress}.
     */
    private static void spawnBreakParticles(PonderScene scene, BlockPos pos, BlockState state, int count) {
        if (state.isAir()) {
            return;
        }
        RandomSource random = RandomSource.create();
        BlockParticleOption options = new BlockParticleOption(ParticleTypes.BLOCK, state);
        for (int i = 0; i < count; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            double mx = (random.nextDouble() - 0.5) * 0.2;
            double my = random.nextDouble() * 0.2;
            double mz = (random.nextDouble() - 0.5) * 0.2;
            scene.getLevel().addParticle(options, x, y, z, mx, my, mz);
        }
    }

    /**
     * {@code state.cycle(property)} through a wildcard-capture helper - {@code
     * WorldInstructions#cycleBlockProperty} only has a {@code Property<?>} to work with, and
     * vanilla's own {@code cycle} needs the captured type to call {@code setValue} internally.
     */
    private static BlockState cycleProperty(BlockState state, Property<?> property) {
        return cyclePropertyTyped(state, property);
    }

    private static <T extends Comparable<T>> BlockState cyclePropertyTyped(BlockState state, Property<T> property) {
        return state.cycle(property);
    }

    /**
     * {@code WorldInstructions#toggleRedstonePower}'s per-block logic: flip {@code POWERED} for a
     * simple on/off signal source, or bounce {@code POWER} between 0 and 15 for an analog one
     * (redstone dust). Leaves anything with neither alone.
     */
    private static BlockState toggleRedstone(BlockState state) {
        if (state.hasProperty(BlockStateProperties.POWERED)) {
            return state.cycle(BlockStateProperties.POWERED);
        }
        if (state.hasProperty(BlockStateProperties.POWER)) {
            int current = state.getValue(BlockStateProperties.POWER);
            return state.setValue(BlockStateProperties.POWER, current > 0 ? 0 : 15);
        }
        return state;
    }

    @Override
    public void addKeyframe() {
        addInstruction(KeyframeInstruction.IMMEDIATE);
    }

    @Override
    public void addLazyKeyframe() {
        addInstruction(KeyframeInstruction.DELAYED);
    }

    @Override
    public void addInstruction(PonderInstruction instruction) {
        scene.getSchedule().add(instruction);
    }

    @Override
    public void addInstruction(Consumer<PonderScene> callback) {
        addInstruction(PonderInstruction.simple(callback));
    }

    @Override
    public void idle(int ticks) {
        addInstruction(new DelayInstruction(ticks));
    }

    @Override
    public void idleSeconds(int seconds) {
        idle(seconds * 20);
    }

    @Override
    public void markAsFinished() {
        addInstruction(new MarkAsFinishedInstruction());
    }

    // Whole-scene configuration, same treatment as configureBasePlate above: applied directly rather
    // than scheduled, since there's exactly one value for the scene's whole lifetime rather than a
    // point in time it takes effect at.

    @Override
    public void scaleSceneView(float factor) {
        scene.setSceneScale(factor);
    }

    @Override
    public void rotateCameraY(float degrees) {
        scene.addCameraYRotation(degrees);
    }

    @Override
    public void removeShadow() {
        scene.setShadowEnabled(false);
    }

    @Override
    public void setSceneOffsetY(float yOffset) {
        scene.setSceneOffsetY(yOffset);
    }

    @Override
    public void setNextUpEnabled(boolean isEnabled) {
        scene.setNextUpEnabled(isEnabled);
    }

    protected class WorldInstructionsImpl implements WorldInstructions {

        // Only ever read/written from inside addInstruction closures built here during program() -
        // see #incrementBlockBreakingProgress. Doesn't need to reset on scene replay: each call
        // already bakes its target stage into the closure it schedules, so replaying the same fixed
        // instruction sequence reproduces the same stages regardless of this map's own state.
        private final Map<BlockPos, Integer> breakingProgress = new HashMap<>();

        @Override
        public void setBlock(BlockPos pos, BlockState state) {
            BlockPos immutable = pos.immutable();
            addInstruction(s -> s.setBlockState(immutable, state));
        }

        @Override
        public void setBlock(BlockPos pos, BlockState state, boolean spawnParticles) {
            BlockPos immutable = pos.immutable();
            addInstruction(s -> {
                if (spawnParticles) {
                    spawnBreakParticles(s, immutable, state, 10);
                }
                s.setBlockState(immutable, state);
            });
        }

        @Override
        public void setBlocks(Selection selection, BlockState state, boolean spawnParticles) {
            for (BlockPos pos : selection) {
                setBlock(pos, state, spawnParticles);
            }
        }

        @Override
        public void replaceBlocks(Selection selection, BlockState state, boolean spawnParticles) {
            for (BlockPos pos : selection) {
                BlockPos immutable = pos.immutable();
                addInstruction(s -> {
                    if (currentDisplayedState(s, immutable).isAir()) {
                        return;
                    }
                    if (spawnParticles) {
                        spawnBreakParticles(s, immutable, state, 10);
                    }
                    s.setBlockState(immutable, state);
                });
            }
        }

        @Override
        public void destroyBlock(BlockPos pos) {
            BlockPos immutable = pos.immutable();
            addInstruction(s -> {
                spawnBreakParticles(s, immutable, currentDisplayedState(s, immutable), 20);
                s.setBlockState(immutable, Blocks.AIR.defaultBlockState());
            });
        }

        @Override
        public void restoreBlocks(Selection selection) {
            addInstruction(s -> s.getLevel().restoreBlocks(selection));
        }

        @Override
        public void modifyBlock(BlockPos pos, UnaryOperator<BlockState> stateFunc, boolean spawnParticles) {
            BlockPos immutable = pos.immutable();
            addInstruction(s -> {
                BlockState current = currentDisplayedState(s, immutable);
                BlockState next = stateFunc.apply(current);
                s.setBlockState(immutable, next);
                for (WorldSectionElementImpl section : sectionsContaining(s, immutable)) {
                    section.setBlockState(immutable, next);
                }
                if (spawnParticles) {
                    spawnBreakParticles(s, immutable, current, 10);
                }
            });
        }

        @Override
        public void modifyBlocks(Selection selection, UnaryOperator<BlockState> stateFunc, boolean spawnParticles) {
            for (BlockPos pos : selection) {
                modifyBlock(pos, stateFunc, spawnParticles);
            }
        }

        @Override
        public void cycleBlockProperty(BlockPos pos, Property<?> property) {
            modifyBlock(pos, state -> state.hasProperty(property) ? cycleProperty(state, property) : state, false);
        }

        @Override
        public void toggleRedstonePower(Selection selection) {
            for (BlockPos pos : selection) {
                modifyBlock(pos, PonderSceneBuilder::toggleRedstone, false);
            }
        }

        @Override
        public ElementLink<WorldSectionElement> showSection(Selection selection, Direction direction) {
            return createSection(selection, direction, false);
        }

        @Override
        public ElementLink<WorldSectionElement> showIndependentSection(Selection selection, Direction direction) {
            return createSection(selection, direction, false);
        }

        @Override
        public ElementLink<WorldSectionElement> showIndependentSectionImmediately(Selection selection) {
            WorldSectionElementImpl element = new WorldSectionElementImpl(selection);
            ElementLink<WorldSectionElement> link = new SimpleElementLink<>(WorldSectionElement.class);
            addInstruction(s -> {
                element.capture(s.getLevel());
                s.addElement(element);
                s.linkElement(element, link);
                element.setVisible(true);
                element.forceApplyFade(1);
                element.setFadeVec(Vec3.ZERO);
            });
            return link;
        }

        @Override
        public void showSectionAndMerge(Selection selection, Direction fadeInDirection, ElementLink<WorldSectionElement> link) {
            WorldSectionElementImpl staging = new WorldSectionElementImpl(selection);
            addInstruction(s -> {
                staging.capture(s.getLevel());
                WorldSectionElement target = s.resolve(link);
                if (target instanceof WorldSectionElementImpl impl) {
                    impl.mergeFrom(staging);
                }
            });
        }

        @Override
        public ElementLink<WorldSectionElement> makeSectionIndependent(Selection selection) {
            WorldSectionElementImpl extracted = new WorldSectionElementImpl(selection);
            ElementLink<WorldSectionElement> link = new SimpleElementLink<>(WorldSectionElement.class);
            Set<BlockPos> positions = new HashSet<>();
            selection.forEach(positions::add);
            addInstruction(s -> {
                for (PonderElement candidate : new ArrayList<>(s.getElements())) {
                    if (candidate instanceof WorldSectionElementImpl source && source != extracted) {
                        source.extractInto(extracted, positions);
                    }
                }
                extracted.setVisible(true);
                extracted.forceApplyFade(1);
                extracted.setFadeVec(Vec3.ZERO);
                s.addElement(extracted);
                s.linkElement(extracted, link);
            });
            return link;
        }

        @Override
        public void hideSection(Selection selection, Direction fadeOutDirection) {
            Set<BlockPos> positions = new HashSet<>();
            selection.forEach(positions::add);
            addInstruction(s -> {
                for (PonderElement candidate : s.getElements()) {
                    if (candidate instanceof WorldSectionElementImpl section && section.isVisible()
                        && section.getBlockPositions().equals(positions)) {
                        addInstruction(new HideSectionInstruction(section, fadeOutDirection, SECTION_FADE_TICKS));
                        return;
                    }
                }
            });
        }

        @Override
        public void hideIndependentSection(ElementLink<WorldSectionElement> link, Direction fadeOutDirection) {
            addInstruction(s -> {
                WorldSectionElement element = s.resolve(link);
                if (element instanceof WorldSectionElementImpl impl) {
                    addInstruction(new HideSectionInstruction(impl, fadeOutDirection, SECTION_FADE_TICKS));
                }
            });
        }

        @Override
        public void rotateSection(ElementLink<WorldSectionElement> link, Vec3 eulerDegrees, int duration) {
            addInstruction(new AnimateElementInstruction<>(link, eulerDegrees, duration,
                WorldSectionElement::setAnimatedRotation, WorldSectionElement::getAnimatedRotation));
        }

        @Override
        public void configureCenterOfRotation(ElementLink<WorldSectionElement> link, Vec3 anchor) {
            addInstruction(s -> {
                WorldSectionElement element = s.resolve(link);
                if (element instanceof WorldSectionElementImpl impl) {
                    impl.setRotationCenter(anchor);
                }
            });
        }

        @Override
        public void configureStabilization(ElementLink<WorldSectionElement> link, Vec3 anchor) {
            // Accepted for signature parity - see WorldInstructions#configureStabilization's javadoc
            // on why this library has nothing yet for it to actually counteract.
        }

        @Override
        public void moveSection(ElementLink<WorldSectionElement> link, Vec3 offset, int duration) {
            addInstruction(new AnimateElementInstruction<>(link, offset, duration,
                WorldSectionElement::setAnimatedOffset, WorldSectionElement::getAnimatedOffset));
        }

        @Override
        public void incrementBlockBreakingProgress(BlockPos pos) {
            BlockPos immutable = pos.immutable();
            int stage = (breakingProgress.getOrDefault(immutable, -1) + 1) % 10;
            breakingProgress.put(immutable, stage);
            addInstruction(s -> {
                for (WorldSectionElementImpl section : sectionsContaining(s, immutable)) {
                    section.setBreakingStage(immutable, stage);
                }
                spawnBreakParticles(s, immutable, currentDisplayedState(s, immutable), 3);
            });
        }

        @Override
        public HolderLookup.Provider getHolderLookupProvider() {
            return scene.getLevel().registryAccess();
        }

        @Override
        public void modifyBlockEntityNBT(Selection selection, Class<? extends BlockEntity> beType, Consumer<CompoundTag> consumer) {
            modifyBlockEntityNBT(selection, beType, consumer, true);
        }

        @Override
        public void modifyBlockEntityNBT(Selection selection, Class<? extends BlockEntity> beType,
                                          Consumer<CompoundTag> consumer, boolean reDrawBlocks) {
            addInstruction(s -> {
                for (BlockPos pos : selection) {
                    BlockEntity blockEntity = s.getLevel().getBlockEntity(pos);
                    if (!beType.isInstance(blockEntity)) {
                        continue;
                    }
                    var registries = s.getLevel().registryAccess();
                    CompoundTag tag = blockEntity.saveWithFullMetadata(registries);
                    consumer.accept(tag);
                    blockEntity.loadWithComponents(tag, registries);
                }
            });
        }

        @Override
        public <T extends BlockEntity> void modifyBlockEntity(BlockPos position, Class<T> beType, Consumer<T> consumer) {
            BlockPos immutable = position.immutable();
            addInstruction(s -> {
                BlockEntity blockEntity = s.getLevel().getBlockEntity(immutable);
                if (beType.isInstance(blockEntity)) {
                    consumer.accept(beType.cast(blockEntity));
                }
            });
        }

        @Override
        public ElementLink<EntityElement> createEntity(Function<Level, Entity> factory) {
            ElementLink<EntityElement> link = new SimpleElementLink<>(EntityElement.class);
            // A one-shot side effect, same shape as setBlock above - no dedicated Instruction class
            // needed (unlike showSection, which has to run every tick to animate a fade-in).
            addInstruction(s -> {
                Entity entity = factory.apply(s.getLevel());
                EntityElementImpl element = new EntityElementImpl(entity);
                s.addElement(element);
                s.linkElement(element, link);
                s.getLevel().addFreshEntity(entity);
            });
            return link;
        }

        @Override
        public ElementLink<EntityElement> createItemEntity(Vec3 location, Vec3 motion, ItemStack stack) {
            return createEntity(level -> {
                ItemEntity itemEntity = new ItemEntity(level, location.x, location.y, location.z, stack);
                itemEntity.setDeltaMovement(motion);
                return itemEntity;
            });
        }

        @Override
        public void modifyEntity(ElementLink<EntityElement> link, Consumer<Entity> entityCallback) {
            addInstruction(s -> {
                EntityElement element = s.resolve(link);
                if (element != null) {
                    element.ifPresent(entityCallback);
                }
            });
        }

        @Override
        public <T extends Entity> void modifyEntities(Class<T> entityClass, Consumer<T> entityCallback) {
            addInstruction(s -> s.forEachWorldEntity(entityClass, entityCallback));
        }

        @Override
        public <T extends Entity> void modifyEntitiesInside(Class<T> entityClass, Selection area, Consumer<T> entityCallback) {
            Set<BlockPos> positions = new HashSet<>();
            area.forEach(positions::add);
            addInstruction(s -> s.forEachWorldEntity(entityClass, entity -> {
                if (positions.contains(entity.blockPosition())) {
                    entityCallback.accept(entity);
                }
            }));
        }
    }

    protected class OverlayInstructionsImpl implements OverlayInstructions {

        @Override
        public TextElementBuilder showText(int duration, String text) {
            TextWindowElement element = new TextWindowElement();
            element.setText(component(textKey(sceneId, textIndex++), text));
            addInstruction(new TextInstruction(element, duration));
            // The element is configured through the returned builder AFTER the instruction is queued,
            // which is fine: nothing reads these fields until the instruction first ticks, long
            // after the storyboard has finished programming.
            return new TextElementBuilderImpl(element);
        }

        @Override
        public void showOutline(PonderPalette palette, Selection selection, int duration) {
            OutlineElement element = new OutlineElement(selection);
            element.setPalette(palette);
            addInstruction(new OutlineInstruction(element, duration));
        }

        // Build-time only (one PonderSceneBuilder per compiled scene, discarded once program()
        // returns) - keyed by whatever arbitrary Object a storyboard passes as its own "this outline
        // identifies the same thing across calls" token. See OutlineInstruction/
        // BoundingBoxOutlineInstruction's own javadoc for why reusing the element (rather than the
        // simpler-looking "just spawn a new one every call") is what actually avoids two outlines
        // fighting for the same slot's visibility.
        private final Map<Object, OutlineElement> slottedOutlines = new HashMap<>();
        private final Map<Object, BoundingBoxOutlineElement> slottedBoxOutlines = new HashMap<>();

        @Override
        public void showOutline(PonderPalette palette, Object slot, Selection selection, int duration) {
            if (slot == null) {
                showOutline(palette, selection, duration);
                return;
            }
            boolean firstUse = !slottedOutlines.containsKey(slot);
            OutlineElement element = slottedOutlines.computeIfAbsent(slot, k -> new OutlineElement(selection));
            element.setPalette(palette);
            addInstruction(new OutlineInstruction(element, duration, firstUse ? null : selection));
        }

        @Override
        public void chaseBoundingBoxOutline(PonderPalette color, Object slot, AABB boundingBox, int duration) {
            if (slot == null) {
                BoundingBoxOutlineElement element = new BoundingBoxOutlineElement(boundingBox);
                element.setPalette(color);
                addInstruction(new BoundingBoxOutlineInstruction(element, duration));
                return;
            }
            boolean firstUse = !slottedBoxOutlines.containsKey(slot);
            BoundingBoxOutlineElement element = slottedBoxOutlines.computeIfAbsent(slot, k -> new BoundingBoxOutlineElement(boundingBox));
            element.setPalette(color);
            addInstruction(new BoundingBoxOutlineInstruction(element, duration, firstUse ? null : boundingBox));
        }

        @Override
        public void showLine(PonderPalette color, Vec3 start, Vec3 end, int duration) {
            LineElement element = new LineElement(start, end, LINE_THICKNESS);
            element.setPalette(color);
            addInstruction(new LineInstruction(element, duration));
        }

        @Override
        public void showBigLine(PonderPalette color, Vec3 start, Vec3 end, int duration) {
            LineElement element = new LineElement(start, end, BIG_LINE_THICKNESS);
            element.setPalette(color);
            addInstruction(new LineInstruction(element, duration));
        }

        @Override
        public void showCenteredScrollInput(BlockPos pos, Direction side, int duration) {
            Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            Vec3 sceneSpace = center.add(Vec3.atLowerCornerOf(side.getNormal()).scale(0.5));
            showScrollInput(sceneSpace, side, duration);
        }

        @Override
        public void showScrollInput(Vec3 location, Direction side, int duration) {
            InputIconElement element = new InputIconElement(location, InputIconElement.ICON_SCROLL);
            addInstruction(new InputIconInstruction(element, duration));
        }

        @Override
        public void showRepeaterScrollInput(BlockPos pos, int duration) {
            showCenteredScrollInput(pos, Direction.UP, duration);
        }

        @Override
        public void showFilterSlotInput(Vec3 location, int duration) {
            InputIconElement element = new InputIconElement(location, InputIconElement.ICON_FILTER);
            addInstruction(new InputIconInstruction(element, duration));
        }

        @Override
        public void showFilterSlotInput(Vec3 location, Direction side, int duration) {
            Vec3 offset = location.add(Vec3.atLowerCornerOf(side.getNormal()).scale(0.3));
            showFilterSlotInput(offset, duration);
        }

        @Override
        public TextElementBuilder showOutlineWithText(Selection selection, int duration, String text) {
            OutlineElement outline = new OutlineElement(selection);
            addInstruction(new OutlineInstruction(outline, duration));
            // The text defaults to pointing at the selection's own centre, so the common case needs
            // no pointAt() call - and the builder recolours the outline along with the text, since
            // both come from the one PonderPalette passed to .colored(...).
            TextWindowElement window = new TextWindowElement();
            window.setText(component(textKey(sceneId, textIndex++), text));
            window.setPointAt(selection.getCenter());
            addInstruction(new TextInstruction(window, duration));
            return new TextElementBuilderImpl(window, outline);
        }

        @Override
        public InputElementBuilder showControls(Vec3 sceneSpace, Pointing direction, int duration) {
            InputWindowElement element = new InputWindowElement(sceneSpace, direction);
            addInstruction(new InputWindowInstruction(element, duration));
            return new InputElementBuilderImpl(element);
        }
    }

    private static class InputElementBuilderImpl implements InputElementBuilder {

        private final InputWindowElement element;

        private InputElementBuilderImpl(InputWindowElement element) {
            this.element = element;
        }

        @Override
        public InputElementBuilder withItem(ItemStack stack) {
            element.setItem(stack);
            return this;
        }

        @Override
        public InputElementBuilder leftClick() {
            element.leftClick();
            return this;
        }

        @Override
        public InputElementBuilder rightClick() {
            element.rightClick();
            return this;
        }

        @Override
        public InputElementBuilder scroll() {
            element.scroll();
            return this;
        }

        @Override
        public InputElementBuilder whileSneaking() {
            element.setQualifier("Sneak");
            return this;
        }

        @Override
        public InputElementBuilder whileCTRL() {
            element.setQualifier("Ctrl");
            return this;
        }
    }

    private class TextElementBuilderImpl implements TextElementBuilder {

        private final TextWindowElement element;
        // Non-null only for showOutlineWithText, where colouring the text must recolour the outline too.
        private final OutlineElement linkedOutline;

        private TextElementBuilderImpl(TextWindowElement element) {
            this(element, null);
        }

        private TextElementBuilderImpl(TextWindowElement element, OutlineElement linkedOutline) {
            this.element = element;
            this.linkedOutline = linkedOutline;
        }

        @Override
        public TextElementBuilder pointAt(Vec3 scenePos) {
            element.setPointAt(scenePos);
            return this;
        }

        @Override
        public TextElementBuilder placeNearTarget() {
            element.setNearScene(true);
            return this;
        }

        @Override
        public TextElementBuilder independent(int y) {
            element.setIndependentY(y);
            return this;
        }

        @Override
        public TextElementBuilder colored(PonderPalette palette) {
            element.setPalette(palette);
            if (linkedOutline != null) {
                linkedOutline.setPalette(palette);
            }
            return this;
        }

        @Override
        public TextElementBuilder attachKeyFrame() {
            addLazyKeyframe();
            return this;
        }
    }

    protected class EffectInstructionsImpl implements EffectInstructions {

        // The colour real redstone dust itself glows at full power - see DustParticleOptions.
        private static final int REDSTONE_COLOR = 0xFF0000;
        private static final int REDSTONE_INDICATOR_COUNT = 10;
        private static final int SUCCESS_INDICATOR_COUNT = 6;

        private final RandomSource random = RandomSource.create();

        // Goes straight through the scene's level - PonderLevel routes it into the scene's particle
        // pool, see PonderLevel#addParticle. Uses DustParticleOptions (ParticleTypes.DUST, what
        // redstone dust uses) since it's vanilla's one built-in particle type taking an arbitrary
        // RGB colour, matching this method's colour-int signature.
        @Override
        public void emitSparks(Vec3 position, int color, int count) {
            addInstruction(s -> {
                Vector3f rgb = new Vector3f(((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F);
                DustParticleOptions options = new DustParticleOptions(rgb, 1F);
                for (int i = 0; i < count; i++) {
                    double mx = (random.nextFloat() - 0.5F) * 0.1;
                    double my = random.nextFloat() * 0.1;
                    double mz = (random.nextFloat() - 0.5F) * 0.1;
                    s.getLevel().addParticle(options, position.x, position.y, position.z, mx, my, mz);
                }
            });
        }

        @Override
        public void createRedstoneParticles(BlockPos pos, int color, int amount) {
            emitSparks(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), color, amount);
        }

        @Override
        public void indicateRedstone(BlockPos pos) {
            createRedstoneParticles(pos, REDSTONE_COLOR, REDSTONE_INDICATOR_COUNT);
        }

        @Override
        public void indicateSuccess(BlockPos pos) {
            addInstruction(s -> {
                for (int i = 0; i < SUCCESS_INDICATOR_COUNT; i++) {
                    double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5);
                    double y = pos.getY() + 0.5 + random.nextDouble() * 0.5;
                    double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5);
                    s.getLevel().addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0, 0.05, 0);
                }
            });
        }

        @Override
        public void emitParticles(Vec3 location, ParticleEmitter emitter, float amountPerCycle, int cycles) {
            addInstruction(new EmitParticlesInstruction(location, emitter, amountPerCycle, cycles));
        }

        @Override
        public <T extends ParticleOptions> ParticleEmitter simpleParticleEmitter(T data, Vec3 motion) {
            return (level, origin, rnd) -> level.addParticle(data, origin.x, origin.y, origin.z, motion.x, motion.y, motion.z);
        }

        @Override
        public <T extends ParticleOptions> ParticleEmitter particleEmitterWithinBlockSpace(T data, Vec3 motion) {
            return (level, origin, rnd) -> {
                double x = origin.x + rnd.nextDouble() - 0.5;
                double y = origin.y + rnd.nextDouble() - 0.5;
                double z = origin.z + rnd.nextDouble() - 0.5;
                level.addParticle(data, x, y, z, motion.x, motion.y, motion.z);
            };
        }
    }
}
