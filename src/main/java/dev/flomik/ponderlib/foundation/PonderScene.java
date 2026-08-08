package dev.flomik.ponderlib.foundation;

import dev.flomik.ponderlib.api.PonderColorScheme;
import dev.flomik.ponderlib.api.element.ElementLink;
import dev.flomik.ponderlib.api.element.PonderElement;
import dev.flomik.ponderlib.api.registration.StoryBoardEntry;
import dev.flomik.ponderlib.api.scene.PonderStoryBoard;
import dev.flomik.ponderlib.foundation.instruction.PonderInstruction;
import dev.flomik.ponderlib.foundation.registration.SchematicLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Runtime state of one Ponder scene. Blocks/block entities live in {@link #level}, a fake
 * {@link PonderLevel} — everything else (the instruction schedule and its blocking/non-blocking
 * tick scheduler) is the real, permanent design.
 */
public class PonderScene {

    private final List<PonderInstruction> schedule = new ArrayList<>();
    private final List<PonderInstruction> activeSchedule = new ArrayList<>();
    private final Set<PonderElement> elements = new LinkedHashSet<>();
    private final Map<UUID, PonderElement> linkedElements = new HashMap<>();
    private final PonderLevel level;
    private final PonderSceneParticles particles;

    private boolean finished;
    private ResourceLocation component;
    // null for a scene with no known registering mod (tests, the direct-PonderStoryBoard compile
    // path below) - getColors() treats that the same as "no override", never a crash.
    private String modId;
    private Component title = Component.empty();
    private Vec3 focusPoint = new Vec3(0.5, 0.5, 0.5);
    private double basePlateMinX = 0;
    private double basePlateMaxX = 1;
    private double basePlateMinZ = 0;
    private double basePlateMaxZ = 1;
    private boolean basePlateConfigured;
    private float sceneScale = 1F;
    private float cameraYRotationOffset;
    private float sceneOffsetY;
    private boolean shadowEnabled = true;
    private boolean nextUpEnabled = true;
    private int currentTime;
    private int totalTime;
    private boolean stoppedCounting;
    private final List<Integer> keyframeTimes = new ArrayList<>();

    private PonderScene() {
        this.level = new PonderLevel(Minecraft.getInstance().level);
        this.particles = new PonderSceneParticles(level);
        level.setParticleSink(particles);
    }

    public static PonderScene compile(PonderStoryBoard storyBoard) {
        PonderScene scene = new PonderScene();
        scene.level.createBackup();
        PonderSceneBuilder builder = new PonderSceneBuilder(scene);
        storyBoard.program(builder, new SimpleSceneBuildingUtil());
        scene.begin();
        return scene;
    }

    /**
     * Loads {@code entry}'s schematic into this scene's level before running its storyboard — the
     * hybrid data/code model: static geometry (and block entities, with their saved data if any)
     * from the .nbt, behavior from the story board.
     */
    public static PonderScene compile(StoryBoardEntry entry) {
        PonderScene scene = new PonderScene();
        scene.component = entry.getComponent();
        SchematicLoader.LoadedSchematic schematic = SchematicLoader.load(entry.getSchematicLocation());
        for (StructureTemplate.StructureBlockInfo info : schematic.blocks()) {
            scene.level.setBlockDirect(info.pos(), info.state());
            if (info.state().hasBlockEntity() && info.state().getBlock() instanceof EntityBlock entityBlock) {
                BlockEntity blockEntity = entityBlock.newBlockEntity(info.pos(), info.state());
                if (blockEntity != null) {
                    if (info.nbt() != null) {
                        blockEntity.load(info.nbt());
                    }
                    scene.level.setBlockEntityDirect(info.pos(), blockEntity);
                }
            }
        }
        // Snapshot every block entity's freshly-loaded (schematic-saved) state right here, before
        // the storyboard runs and has any chance to mutate one (e.g. a chest's triggerEvent
        // toggling its own openCount) - see PonderLevel#createBackup/resetBlockEntities.
        scene.level.createBackup();
        // The schematic location is already namespaced to whichever mod registered this entry
        // (see DefaultPonderSceneRegistrationHelper#asLocation) - reuse that for both lang-key
        // generation (see PonderSceneBuilder) and #getColors, rather than adding a second field.
        String modId = entry.getSchematicLocation().getNamespace();
        scene.modId = modId;
        PonderSceneBuilder builder = new PonderSceneBuilder(scene, modId);
        // The schematic's own size, not a guess - see SimpleSceneBuildingUtil's javadoc for why this is
        // what makes select().layer(0)/everywhere()/column(...) work without hand-rolled loops.
        entry.getBoard().program(builder, new SimpleSceneBuildingUtil(schematic.size()));
        scene.begin();
        return scene;
    }

    List<PonderInstruction> getSchedule() {
        return schedule;
    }

    public void setBlockState(BlockPos pos, BlockState state) {
        level.setBlockDirect(pos, state);
    }

    public BlockState getBlockState(BlockPos pos) {
        return level.getBlockState(pos);
    }

    public PonderLevel getLevel() {
        return level;
    }

    /**
     * The colours this scene draws its base plate/tooltip/UI chrome with - whichever mod registered
     * this scene's own {@code PonderPlugin#colors()}, or PonderLib's own defaults if this scene has
     * no known owner. See {@link PonderIndex#colorsFor}.
     */
    public PonderColorScheme getColors() {
        return PonderIndex.colorsFor(modId);
    }

    /**
     * This scene's live particles — see {@link PonderSceneParticles}. Rendered by
     * {@code foundation.ui.PonderUI#renderScene}, ticked by {@link #tick()}, cleared by
     * {@link #begin()}.
     */
    public PonderSceneParticles getParticles() {
        return particles;
    }

    /**
     * The registered item id this scene is about ({@link StoryBoardEntry#getComponent()}), or
     * {@code null} for a scene compiled directly from a {@link PonderStoryBoard} with no entry
     * (tests, the demo storyboard's own direct-compile path) - see {@link
     * dev.flomik.ponderlib.foundation.ui.PonderUI#getSubject()}, the actual consumer.
     */
    public ResourceLocation getComponent() {
        return component;
    }

    public void begin() {
        // A block entity is a long-lived, mutable Java object (see PonderLevel#getBlockEntity) -
        // unlike a plain BlockState, replaying the schedule from scratch doesn't reset ITS internal
        // state on its own (a chest's triggerEvent-driven openCount/openness, say). Reset it back
        // to its schematic-saved snapshot here, before anything re-captures it (see
        // RevealSectionInstruction#firstTick -> WorldSectionElementImpl#capture), so a previous
        // playthrough's chest left open doesn't carry that state into this one - with identify mode
        // paused, nothing would otherwise tick forward to close it again the way normal, unpaused
        // playback does within the same or next frame.
        level.resetBlockEntities();
        // Entities are storyboard-created transient content, same reasoning as particles below: a
        // scene's schematic never carries entities of its own (see PonderLevel#clearEntities), so
        // there's nothing to reset THEM to except gone - otherwise a replay/rewind would pile up a
        // second copy of every entity the first playthrough already created.
        level.clearEntities();
        // Particles outlive individual elements (they're owned by the scene, not by whatever spawned
        // them), so replaying/rewinding has to drop them explicitly or a rewind would leave the
        // previous playthrough's smoke still drifting through the restarted scene.
        particles.clear();
        activeSchedule.clear();
        // Must reset() every instruction, not just re-add them: a TickingInstruction that already
        // ran to completion (remainingTicks == 0) looks permanently "already complete" otherwise -
        // tick() would never call firstTick() again, so replaying after a rewind (see seekToTime)
        // would silently never re-add/re-link/re-show any element again.
        for (PonderInstruction instruction : schedule) {
            instruction.reset(this);
            activeSchedule.add(instruction);
        }
        elements.clear();
        linkedElements.clear();
        finished = false;
        currentTime = 0;
        totalTime = 0;
        stoppedCounting = false;
        keyframeTimes.clear();
        // totalTime is NOT a naive sum of every instruction's own duration - that would overcount
        // non-blocking ones (fades/text/animation run alongside
        // whatever's next, they don't extend the scene's serial length) and inflate totalTime past
        // what seekToTime could ever actually reach, leaving a dead zone at the end of the scrubber.
        // Each instruction reports its own contribution via addToSceneTime from onScheduled instead
        // (see TickingInstruction#onScheduled) - only blocking ones (DelayInstruction) call it.
        for (PonderInstruction instruction : schedule) {
            instruction.onScheduled(this);
        }
    }

    public <E extends PonderElement> void linkElement(E element, ElementLink<E> link) {
        linkedElements.put(link.getId(), element);
    }

    public <E extends PonderElement> E resolve(ElementLink<E> link) {
        PonderElement element = linkedElements.get(link.getId());
        return element == null ? null : link.cast(element);
    }

    /**
     * Runs {@code function} against every entity of type {@code type} currently in this scene —
     * the backing of {@code WorldInstructions#modifyEntities}/{@code #modifyEntitiesInside}.
     */
    public <T extends Entity> void forEachWorldEntity(Class<T> type, Consumer<T> function) {
        for (Entity entity : level.getSceneEntities()) {
            if (type.isInstance(entity)) {
                function.accept(type.cast(entity));
            }
        }
    }

    public void tick() {
        particles.tick();
        level.tickEntities();
        for (PonderElement element : elements) {
            element.tick(this);
        }

        if (currentTime < totalTime) {
            currentTime++;
        }

        for (Iterator<PonderInstruction> iterator = activeSchedule.iterator(); iterator.hasNext(); ) {
            PonderInstruction instruction = iterator.next();
            instruction.tick(this);
            if (instruction.isComplete()) {
                iterator.remove();
                if (instruction.isBlocking()) {
                    break;
                }
                continue;
            }
            if (instruction.isBlocking()) {
                break;
            }
        }

        if (activeSchedule.isEmpty()) {
            finished = true;
        }
    }

    /**
     * Ticks elapsed since {@link #begin()}, capped at {@link #getTotalTime()} - drives the
     * scrubber (see {@code foundation.ui.PonderUI}'s timeline bar).
     */
    public int getCurrentTime() {
        return currentTime;
    }

    public int getTotalTime() {
        return totalTime;
    }

    public float getSceneProgress() {
        return totalTime == 0 ? 0 : currentTime / (float) totalTime;
    }

    /**
     * Extends the timeline's total length by {@code time} ticks - called from {@link
     * dev.flomik.ponderlib.foundation.instruction.PonderInstruction#onScheduled} once per
     * instruction when the schedule is (re-)built. Only blocking instructions call this (see
     * {@code TickingInstruction#onScheduled}) - the scene's serial length is exactly the sum of
     * what it's forced to wait for, nothing more.
     */
    public void addToSceneTime(int time) {
        if (!stoppedCounting) {
            totalTime += time;
        }
    }

    /**
     * Freezes {@link #getTotalTime()} - called by {@code MarkAsFinishedInstruction#onScheduled} so
     * nothing scheduled after the scene's own "finished" marker (there shouldn't be anything, but
     * just in case) inflates the timeline past where the scene actually ends.
     */
    public void stopCounting() {
        stoppedCounting = true;
    }

    /**
     * Records a scrubber tick mark at {@code totalTime + offset} - called from {@code
     * foundation.instruction.KeyframeInstruction#onScheduled}. Called at the same {@code
     * onScheduled} pass as {@link #addToSceneTime}, so it sees each keyframe's position exactly as
     * authored: after every blocking wait that already ran before it in the schedule, before any
     * that come later.
     */
    public void markKeyframe(int offset) {
        if (!stoppedCounting) {
            keyframeTimes.add(totalTime + offset);
        }
    }

    public int getKeyframeCount() {
        return keyframeTimes.size();
    }

    public int getKeyframeTime(int index) {
        return keyframeTimes.get(index);
    }

    /**
     * Jumps playback to {@code time} ticks from the start: seeking backwards restarts the scene
     * (calls {@link #begin()}) and fast-forwards from 0, since there's no reverse playback -
     * scrubbing to an earlier point in a scene with side effects (e.g. block state changes) re-runs
     * them exactly as they played the first time.
     */
    public void seekToTime(int time) {
        if (time < currentTime) {
            begin();
        }
        while (currentTime < time && !finished) {
            tick();
        }
    }

    public void addElement(PonderElement element) {
        elements.add(element);
    }

    public Set<PonderElement> getElements() {
        return elements;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setTitle(Component title) {
        this.title = title;
    }

    public Component getTitle() {
        return title;
    }

    /**
     * The point in scene-local space the camera centers on. Stands in for the real system's
     * base-plate-rectangle centering (see {@code SceneTransform.apply}'s final translate) — v1
     * only ever shows one section at a time, so a single point is enough.
     */
    public void setFocusPoint(Vec3 focusPoint) {
        this.focusPoint = focusPoint;
    }

    public Vec3 getFocusPoint() {
        return focusPoint;
    }

    /**
     * The footprint (in scene-local X/Z) the ground-contact shadow streak is drawn around — see
     * {@code PonderUI#renderBasePlateShadowAndFlash}. Auto-derived from whatever selection {@code
     * PonderSceneBuilder}'s {@code showSection} last revealed, unless the author pinned it via
     * {@link dev.flomik.ponderlib.api.scene.SceneBuilder#configureBasePlate} (see {@link
     * #configureBasePlateBounds}) — a convenience fallback for scenes that don't bother calling it
     * explicitly.
     */
    public void setBasePlateBounds(double minX, double maxX, double minZ, double maxZ) {
        if (basePlateConfigured) {
            return;
        }
        this.basePlateMinX = minX;
        this.basePlateMaxX = maxX;
        this.basePlateMinZ = minZ;
        this.basePlateMaxZ = maxZ;
    }

    /**
     * The explicit, pinned equivalent of {@link #setBasePlateBounds} — once called, later {@code
     * showSection} calls no longer silently move/shrink the footprint to whatever they last
     * revealed. See {@link dev.flomik.ponderlib.api.scene.SceneBuilder#configureBasePlate}.
     */
    public void configureBasePlateBounds(double minX, double maxX, double minZ, double maxZ) {
        this.basePlateConfigured = true;
        this.basePlateMinX = minX;
        this.basePlateMaxX = maxX;
        this.basePlateMinZ = minZ;
        this.basePlateMaxZ = maxZ;
    }

    public double getBasePlateMinX() {
        return basePlateMinX;
    }

    public double getBasePlateMaxX() {
        return basePlateMaxX;
    }

    public double getBasePlateMinZ() {
        return basePlateMinZ;
    }

    public double getBasePlateMaxZ() {
        return basePlateMaxZ;
    }

    /**
     * @see dev.flomik.ponderlib.api.scene.SceneBuilder#scaleSceneView
     */
    public void setSceneScale(float sceneScale) {
        this.sceneScale = sceneScale;
    }

    public float getSceneScale() {
        return sceneScale;
    }

    /**
     * @see dev.flomik.ponderlib.api.scene.SceneBuilder#rotateCameraY
     */
    public void addCameraYRotation(float degrees) {
        this.cameraYRotationOffset += degrees;
    }

    public float getCameraYRotationOffset() {
        return cameraYRotationOffset;
    }

    /**
     * @see dev.flomik.ponderlib.api.scene.SceneBuilder#setSceneOffsetY
     */
    public void setSceneOffsetY(float sceneOffsetY) {
        this.sceneOffsetY = sceneOffsetY;
    }

    public float getSceneOffsetY() {
        return sceneOffsetY;
    }

    /**
     * @see dev.flomik.ponderlib.api.scene.SceneBuilder#removeShadow
     */
    public void setShadowEnabled(boolean shadowEnabled) {
        this.shadowEnabled = shadowEnabled;
    }

    public boolean isShadowEnabled() {
        return shadowEnabled;
    }

    /**
     * @see dev.flomik.ponderlib.api.scene.SceneBuilder#setNextUpEnabled
     */
    public void setNextUpEnabled(boolean nextUpEnabled) {
        this.nextUpEnabled = nextUpEnabled;
    }

    public boolean isNextUpEnabled() {
        return nextUpEnabled;
    }
}
