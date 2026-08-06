package dev.flomik.ponderlib.foundation.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import dev.flomik.ponderlib.Config;
import dev.flomik.ponderlib.api.PonderColorScheme;
import dev.flomik.ponderlib.api.element.PonderElement;
import dev.flomik.ponderlib.api.element.PonderOverlayElement;
import dev.flomik.ponderlib.api.element.PonderSceneElement;
import dev.flomik.ponderlib.api.registration.StoryBoardEntry;
import dev.flomik.ponderlib.foundation.PonderIndex;
import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.foundation.element.TextWindowElement;
import dev.flomik.ponderlib.foundation.element.WorldSectionElementImpl;
import dev.flomik.ponderlib.render.PonderBoxElement;
import dev.flomik.ponderlib.render.PonderRenderStateShards;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * The screen a Ponder scene plays in: a fixed isometric-ish camera over one or more {@link
 * PonderScene}s (see {@link #scenes}/{@link #index}), a timeline scrubber that snaps clicks/drags
 * to a keyframe (see {@link #renderTimeline}/{@link #hoveredKeyframeIndex}, falling back to free
 * proportional seeking when a scene has no keyframes), a button row (identify/page/close/replay/
 * slow-mode), and a cross-fade slide when paging between scenes (see {@link #tickLazyIndex}).
 * Index/tag browsing across ALL registered scenes lives in {@link PonderIndexScreen}/{@code
 * PonderTagIndexScreen}, which open instances of this screen rather than being part of it.
 */
public class PonderUI extends Screen {

    // Fixed light directions for scene rendering - without these, blocks are lit by whatever
    // direction was left over from the last world render, which reads as wrong shading on faces
    // meant to be flat-lit.
    private static final Vector3f DIFFUSE_LIGHT_0 = new Vector3f(0.4F, -1.0F, 0.7F).normalize();
    private static final Vector3f DIFFUSE_LIGHT_1 = new Vector3f(-0.4F, -0.5F, 0.7F).normalize();

    // Same as RenderType.debugQuads() except depth WRITE is off (COLOR_WRITE, not the default
    // COLOR_DEPTH_WRITE): depth TEST stays on, so real geometry in front of the shadow/flash still
    // correctly hides it, but neither effect leaves depth values behind for anything drawn later
    // (the identify-mode hover outline, or each other) to get wrongly occluded by - these are
    // decorative ground overlays, not solid geometry, and must not act as an occluder themselves.
    private static final RenderType OVERLAY_RENDER_TYPE = RenderType.create(
        "ponderlib_scene_overlay",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        true,
        RenderType.CompositeState.builder()
            .setShaderState(PonderRenderStateShards.POSITION_COLOR_SHADER)
            .setTransparencyState(PonderRenderStateShards.TRANSLUCENT_TRANSPARENCY)
            .setCullState(PonderRenderStateShards.NO_CULL)
            .setWriteMaskState(PonderRenderStateShards.COLOR_WRITE)
            .createCompositeState(false)
    );

    // Warmup delay before the finishing flash pops in, 30 ticks after the scene's last counted
    // tick (see #tickFinishingFlash). Its colours come from the active scene's own
    // PonderColorScheme (see PonderScene#getColors) rather than a hardcoded constant.
    private static final int FLASH_WARMUP_TICKS = 30;
    private static final int NEXT_UP_WARMUP_TICKS = 50;
    private static final Component NEXT_UP_LABEL =
        Component.literal("Next up:").withStyle(ChatFormatting.GRAY);

    // The scrubber's own placement: NOT a screen-width-relative margin - it's always exactly 220px
    // wide and centred (x = width/2 - 110), sitting near the bottom edge.
    private static final int TIMELINE_WIDTH = 220;
    private static final int TIMELINE_BOTTOM_OFFSET = 27;
    // The bar is a 1px-tall frame with a 4px gradient fill drawn over it (two bands, 0..3 and
    // 3..4), offset by (-2,-2) from the frame's own origin.
    private static final int TIMELINE_FILL_HEIGHT = 4;
    private static final int TIMELINE_FILL_SPLIT = 3;
    // Fill/border/keyframe colours come from the active scene's own PonderColorScheme (see
    // PonderScene#getColors) rather than hardcoded constants.
    private static final int KEYFRAME_HEIGHT_IDLE = 4;
    private static final int KEYFRAME_HEIGHT_HOVER = 8;
    // Exponential chase factor for the bar fill - see #chaseTimelineProgress.
    private static final float TIMELINE_CHASE_FACTOR = 0.5F;
    // Extra tick delay when comfy reading is on: tick the scene every 3rd tick instead of every one.
    private static final int EXTENDED_TICK_LENGTH = 2;
    // z values for the fill and the keyframe marks, relative to the translate(..., 100) frame they're
    // drawn in - so both land in front of the frame drawn at 400.
    private static final int FILL_Z = 310;
    private static final int MARK_Z = 320;

    private static final Component IDENTIFY_HINT =
        Component.literal("Hover a block to inspect it").withStyle(ChatFormatting.GRAY);

    // One item can have several registered scenes, and the left/right buttons page between them.
    // `scene` is a view onto the active one so the rest of this class reads unchanged.
    private final List<PonderScene> scenes;
    private int index;
    private boolean scrubbingTimeline;
    private boolean identifyMode;

    // The scene-switch slide: `index` jumps straight to the target scene the instant scroll() is
    // called, and this chases it every tick (same chase-and-snap shape as timelineProgressValue
    // below, just a different factor). While it hasn't caught up yet, render() draws BOTH the
    // active scene and whichever neighbour it's still chasing away from, each offset by
    // #slideOffset - see render()/renderScene(int)/renderOverlay(PonderScene).
    private static final float LAZY_INDEX_CHASE_FACTOR = 0.25F;
    private float lazyIndexValue;

    // Slow mode ("comfy reading"): ticks the scene only every (extendedTickLength + 1) ticks, and
    // ONLY while a text window is on screen - the point is to give reading time, not to slow the
    // whole scene down. See #tick.
    private int extendedTickLength;
    private int extendedTickTimer;

    private PonderButton identifyButton;
    private PonderButton slowModeButton;
    private PonderButton leftButton;
    private PonderButton rightButton;

    private float timelineProgressValue;

    // finishingFlash's previous/value pair only ever changes via a direct assignment (see
    // #tickFinishingFlash), never a target-chase - Mth.lerp(partialTick, previous, value) is all
    // the interpolation this needs.
    private int finishingFlashWarmup;
    private float finishingFlashPrevious;
    private float finishingFlashValue;

    // The "next up" teaser: waits 50 ticks after a scene reaches its end, then eases a speech box
    // in above the right arrow naming the scene that follows. Same previous/value pair as the flash
    // above, for the same reason.
    private int nextUpWarmup;
    private float nextUpPrevious;
    private float nextUpValue;

    public PonderUI(PonderScene scene) {
        this(List.of(scene));
    }

    public PonderUI(List<PonderScene> scenes) {
        super(scenes.get(0).getTitle());
        if (scenes.isEmpty()) {
            throw new IllegalArgumentException("A PonderUI needs at least one scene");
        }
        this.scenes = List.copyOf(scenes);
    }

    /**
     * The scene currently being shown. Everything else in this class works through this rather than a
     * fixed field, so paging with {@link #scroll} needs no other bookkeeping.
     */
    PonderScene scene() {
        return scenes.get(index);
    }

    /**
     * Compiles EVERY scene registered for {@code stack}'s item, in the registry's own order (see
     * {@code PonderSceneRegistry}'s topological sort), and opens the first — the left/right buttons
     * page between the rest. Taking only the first entry would silently hide every scene after it
     * once an item has more than one registered. The raw entry point any trigger (tooltip hold-key,
     * a command, another mod's UI) should call — see {@code foundation.PonderTooltipHandler} for the
     * tooltip-driven trigger.
     */
    public static PonderUI of(ItemStack stack) {
        ResourceLocation component = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Collection<StoryBoardEntry> entries = PonderIndex.getScenes().getScenes(component);
        if (entries.isEmpty()) {
            throw new IllegalStateException("No ponder scenes registered for " + component);
        }
        List<PonderScene> compiled = new ArrayList<>();
        for (StoryBoardEntry entry : entries) {
            compiled.add(PonderScene.compile(entry));
        }
        return new PonderUI(compiled);
    }

    /**
     * Compiles and opens a specific, already-resolved entry directly - used by
     * {@code PonderIndexScreen}'s scene list and the world-block-lookup trigger (see
     * {@code PonderTooltipHandler#tickWorldLookup}), where the entry is already known rather than
     * looked up by an {@link ItemStack}'s registered component.
     */
    public static PonderUI of(StoryBoardEntry entry) {
        // Opens every sibling scene registered for the same item, positioned on the requested one -
        // not just that one scene in isolation, so the left/right buttons always have something
        // real to page between regardless of which entry point opened the screen.
        Collection<StoryBoardEntry> siblings = PonderIndex.getScenes().getScenes(entry.getComponent());
        List<PonderScene> compiled = new ArrayList<>();
        int start = 0;
        for (StoryBoardEntry sibling : siblings) {
            if (sibling.getSchematicLocation().equals(entry.getSchematicLocation())) {
                start = compiled.size();
            }
            compiled.add(PonderScene.compile(sibling));
        }
        if (compiled.isEmpty()) {
            // An entry that isn't in the registry at all (direct API use) - still open it on its own.
            compiled.add(PonderScene.compile(entry));
        }
        PonderUI ui = new PonderUI(compiled);
        ui.index = start;
        ui.lazyIndexValue = start;
        return ui;
    }

    /**
     * The item this screen's scene is "about", so {@code foundation.PonderTooltipHandler} can show
     * a green "already open" hint instead of the hold-progress bar when hovering that exact item
     * while this screen is open. Empty for a scene with no registered component (compiled directly
     * from a {@link dev.flomik.ponderlib.api.scene.PonderStoryBoard}, not a {@link StoryBoardEntry}
     * - tests, direct API use) or whose component doesn't resolve to a real registered item.
     */
    public ItemStack getSubject() {
        ResourceLocation component = scene().getComponent();
        if (component == null || !BuiltInRegistries.ITEM.containsKey(component)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(component));
    }

    /**
     * Builds the button row: {@code bY = height - 20 - 31}, a spacing of 8, identify on the left of
     * a centred cluster, then left/close/right, then replay, with slow mode pinned to the right
     * edge. The scene itself is NOT touched here — see the long note below on why this method must
     * stay free of scene state.
     */
    @Override
    protected void init() {
        super.init();
        int spacing = 8;
        int bY = height - PonderButton.SIZE - 31;
        int bX = (width - PonderButton.SIZE) / 2 - (70 + 2 * spacing);

        identifyButton = addRenderableWidget(new PonderButton(bX, bY, PonderButton.Icon.IDENTIFY,
            Component.literal("Identify"), this::toggleIdentifyMode, this::activeColors)
            .withShortcut(minecraft.options.keyDrop));

        bX += 50 + spacing;
        leftButton = addRenderableWidget(new PonderButton(bX, bY, PonderButton.Icon.LEFT,
            Component.literal("Previous scene"), () -> scroll(false), this::activeColors)
            .withShortcut(minecraft.options.keyLeft));

        bX += PonderButton.SIZE + spacing;
        addRenderableWidget(new PonderButton(bX, bY, PonderButton.Icon.CLOSE,
            Component.literal("Close"), this::onClose, this::activeColors)
            .withShortcut(minecraft.options.keyInventory));

        bX += PonderButton.SIZE + spacing;
        rightButton = addRenderableWidget(new PonderButton(bX, bY, PonderButton.Icon.RIGHT,
            Component.literal("Next scene"), () -> scroll(true), this::activeColors)
            .withShortcut(minecraft.options.keyRight));

        bX += 50 + spacing;
        addRenderableWidget(new PonderButton(bX, bY, PonderButton.Icon.REPLAY,
            Component.literal("Replay"), this::replay, this::activeColors)
            .withShortcut(minecraft.options.keyDown));

        slowModeButton = addRenderableWidget(new PonderButton(width - 20 - 31, bY, PonderButton.Icon.SLOW,
            Component.literal("Slow reading pace"), this::toggleComfyReading, this::activeColors));

        updateButtonStates();
    }

    /**
     * The currently active scene's own {@link PonderColorScheme} - buttons are built once here in
     * {@link #init}, but read this fresh every frame (see {@code PonderButton}'s own {@code
     * colors} supplier) since which scene is active can change afterwards (paging, the cross-fade
     * slide).
     */
    private PonderColorScheme activeColors() {
        return scene().getColors();
    }

    private void toggleIdentifyMode() {
        identifyMode = !identifyMode;
        updateButtonStates();
    }

    private void toggleComfyReading() {
        Config.COMFY_READING.set(!Config.COMFY_READING.get());
        updateButtonStates();
    }

    /**
     * Restarts the active scene from the beginning. Goes through {@link #seekTo} so the finishing
     * flash and the scrubber fill reset with it.
     */
    private void replay() {
        identifyMode = false;
        seekTo(0);
        updateButtonStates();
    }

    /**
     * Pages to the previous/next scene of this item: clamp, restart the scene being moved to, and
     * drop identify mode (it pauses ticking, which would otherwise leave the newly-shown scene
     * frozen on its first frame). {@code index} jumps straight to the target; {@code
     * lazyIndexValue} is left exactly where it was and simply keeps chasing it every tick (see
     * {@link #tickLazyIndex}), which is what makes the slide happen and what makes paging again
     * mid-slide redirect smoothly instead of snapping.
     * <p>
     * The scene being left is simply left ticking no further while it slides away, rather than
     * fading its own content out first - it slides off as a frozen snapshot of wherever it was.
     * Only the camera slide itself happens.
     */
    boolean scroll(boolean forward) {
        int previous = index;
        index = Mth.clamp(index + (forward ? 1 : -1), 0, scenes.size() - 1);
        if (previous == index) {
            return false;
        }
        identifyMode = false;
        scene().begin();
        timelineProgressValue = 0;
        finishingFlashWarmup = 0;
        finishingFlashPrevious = 0F;
        finishingFlashValue = 0F;
        nextUpWarmup = 0;
        nextUpPrevious = 0F;
        nextUpValue = 0F;
        updateButtonStates();
        return true;
    }

    /**
     * Scenes can also be paged with the scroll wheel, not just the buttons.
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (scenes.size() > 1 && scroll(delta < 0)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    /**
     * The two toggle buttons show whether their feature is on, and the paging buttons are disabled
     * at the ends (or entirely, for a single-scene item — most items, so the row shouldn't imply
     * paging that does not exist).
     */
    private void updateButtonStates() {
        if (identifyButton != null) {
            identifyButton.setFlashing(identifyMode);
        }
        if (slowModeButton != null) {
            slowModeButton.setFlashing(Config.COMFY_READING.get());
        }
        // An arrow only exists while it can actually be used, rather than staying mounted with the
        // dead one greyed out - a permanently dead button is noise. Positions stay fixed rather
        // than closing the gap, so `close` stays dead centre and doesn't shift under the cursor as
        // you page.
        if (leftButton != null) {
            leftButton.visible = leftButton.active = index > 0;
        }
        if (rightButton != null) {
            rightButton.visible = rightButton.active = index < scenes.size() - 1;
            // Flashes the right arrow once the scene is over, as the nudge to move on.
            rightButton.setFlashing(rightButton.visible && scene().isFinished());
        }
    }

    // init() must never touch scene state beyond building buttons - it's not just the screen's
    // first-open hook. Screen.rebuildWidgets() (called from resize()) invokes this same overridable
    // init() on EVERY window resize/fullscreen-mode toggle, and GLFW can fire a whole burst of
    // framebuffer-resize callbacks during one such transition. If init() ever called scene().begin()
    // again, that would clear the scene's elements on every one of those calls; they normally refill
    // on the very next scene().tick() (see RevealSectionInstruction#firstTick, which only reruns
    // right after a reset()), invisible during normal playback since tick() runs again within the
    // same or next frame regardless. But identify mode gates scene().tick() off entirely (see #tick
    // below) - with it held, ticking never resumes, so a resize while identify mode is active would
    // leave the scene empty (only the element-independent base-plate shadow still rendering) until
    // identify mode is toggled off again.

    @Override
    public void tick() {
        // Slow mode ("comfy reading"): while it's on AND a text window is currently on screen, the
        // scene ticks once every (extendedTickLength + 1) ticks instead of every tick. The gate on
        // "text is visible" is the whole point - this buys reading time, it doesn't just make the
        // whole scene sluggish regardless of what's happening.
        extendedTickLength = Config.COMFY_READING.get() && hasVisibleText() ? EXTENDED_TICK_LENGTH : 0;

        if (extendedTickTimer == 0) {
            // identifyMode pauses the scene so you can inspect it without it moving under you.
            if (!identifyMode) {
                scene().tick();
            }
            extendedTickTimer = extendedTickLength;
        } else {
            extendedTickTimer--;
        }

        tickLazyIndex();
        tickTimelineProgress();
        tickFinishingFlash();
        tickNextUp();
        for (var child : children()) {
            if (child instanceof PonderButton button) {
                button.tick();
            }
        }
        updateButtonStates();
    }

    /**
     * Whether a text window is on screen right now — walks the active scene's elements directly.
     */
    private boolean hasVisibleText() {
        for (PonderElement element : scene().getElements()) {
            if (element instanceof TextWindowElement && element.isVisible()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Identify mode: a pause-and-inspect toggle INSIDE an already-open scene - not to be confused
     * with {@code PonderTooltipHandler#tickWorldLookup}, an unrelated trigger for opening a scene
     * from the world in the first place. Toggled by the vanilla "drop item" key; the identify
     * button also advertises the same shortcut, but this key check works independently of it.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Minecraft.getInstance().options.keyDrop.matches(keyCode, scanCode)) {
            identifyMode = !identifyMode;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * {@code currentTime == totalTime - 1} fires for exactly one tick, right before {@code
     * currentTime} caps out, starting a 30-tick warmup before the flash pops in. Once triggered it
     * locks at full opacity permanently - nothing ever eases it back down, so this is a one-tick
     * flash-IN, not a pulse.
     */
    private void tickFinishingFlash() {
        finishingFlashPrevious = finishingFlashValue;

        if (scene().getTotalTime() > 0 && scene().getCurrentTime() == scene().getTotalTime() - 1) {
            finishingFlashWarmup = FLASH_WARMUP_TICKS;
        }
        if (finishingFlashWarmup > 0) {
            finishingFlashWarmup--;
            if (finishingFlashWarmup == 0) {
                finishingFlashPrevious = finishingFlashValue;
                finishingFlashValue = 1F;
            }
        }
    }

    /**
     * The teaser only starts warming up once the scene hits its last tick, and it drops straight
     * back to 0 the moment the scene is no longer finished (a rewind, or paging away).
     */
    private void tickNextUp() {
        nextUpPrevious = nextUpValue;

        if (!scene().isFinished() || nextScene() == null) {
            nextUpWarmup = 0;
            nextUpValue = 0F;
            return;
        }
        if (scene().getTotalTime() > 0 && scene().getCurrentTime() == scene().getTotalTime() - 1) {
            nextUpWarmup = NEXT_UP_WARMUP_TICKS;
        }
        if (nextUpWarmup > 0) {
            nextUpWarmup--;
            if (nextUpWarmup == 0) {
                nextUpPrevious = nextUpValue;
                nextUpValue = 1F;
            }
        }
    }

    private PonderScene nextScene() {
        return index < scenes.size() - 1 ? scenes.get(index + 1) : null;
    }

    private float getNextUp(float partialTick) {
        return Mth.lerp(partialTick, nextUpPrevious, nextUpValue);
    }

    /**
     * The "next up" speech box: it sits just above the right arrow (which is also what it points
     * at), rises slightly as it fades in, and names the scene that follows. Only ever shown when
     * the current scene has finished AND there is a next one - there's no per-scene opt-out, every
     * scene participates. The downward tail is a small triangle of pixel fills on top of {@link
     * PonderBoxElement}.
     */
    private void renderNextUp(GuiGraphics graphics, float partialTick) {
        PonderScene next = nextScene();
        if (next == null || rightButton == null || !rightButton.visible) {
            return;
        }
        float value = getNextUp(partialTick);
        if (value <= 1 / 16F) {
            return;
        }

        Component title = next.getTitle();
        int boxWidth = Math.max(font.width(title), font.width(NEXT_UP_LABEL)) + 5;
        int boxHeight = 20;
        int centreX = rightButton.getX() + PonderButton.SIZE / 2;
        // Rises by 5px as it eases in.
        int boxBottom = rightButton.getY() - 6 + Math.round(value * 5);
        int boxLeft = centreX - boxWidth / 2;
        int boxTop = boxBottom - boxHeight;

        new PonderBoxElement()
            .withBackground(0xFF000000)
            .gradientBorder(scene().getColors().frameBorderTop(), scene().getColors().frameBorderBottom())
            .at(boxLeft, boxTop, 400)
            .withBounds(boxWidth, boxHeight)
            .withAlpha(value)
            .render(graphics);

        // The downward tail, so the box reads as belonging to the arrow underneath it.
        int alpha = Math.round(0xFF * Mth.clamp(value, 0F, 1F));
        int tailColor = (alpha << 24) | 0x000000;
        for (int i = 0; i < 4; i++) {
            graphics.fill(centreX - 4 + i, boxBottom + i, centreX + 4 - i, boxBottom + i + 1, 410, tailColor);
        }

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 410);
        int textAlpha = Math.max(0x05, alpha);
        graphics.drawCenteredString(font, NEXT_UP_LABEL, centreX, boxTop + 3, (textAlpha << 24) | 0xBBBBBB);
        graphics.drawCenteredString(font, title, centreX, boxTop + 13, (textAlpha << 24) | 0xFFFFFF);
        graphics.pose().popPose();
    }

    private float getFinishingFlash(float partialTick) {
        return Mth.lerp(partialTick, finishingFlashPrevious, finishingFlashValue);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Dims the real world behind the scene with the same gradient vanilla uses behind e.g.
        // the furnace/crafting screens - without this the scene floats over a raw, distracting
        // view of whatever the player happens to be standing in.
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        // While lazyIndexValue hasn't caught up to index yet, the scene it's chasing AWAY from is
        // still on screen too (sliding out the opposite side from the one the active scene slides
        // in from) - see #slideOffset.
        boolean transitioning = lazyIndexValue != (float) index;
        int otherIndex = Mth.clamp(lazyIndexValue < index ? index - 1 : index + 1, 0, scenes.size() - 1);

        renderScene(graphics, partialTick, index);
        if (transitioning && otherIndex != index) {
            renderScene(graphics, partialTick, otherIndex);
        }
        renderOverlay(graphics, partialTick, scene());
        if (transitioning && otherIndex != index) {
            renderOverlay(graphics, partialTick, scenes.get(otherIndex));
        }
        renderTimeline(graphics, mouseX, mouseY);
        renderNextUp(graphics, partialTick);
        if (identifyMode) {
            renderIdentifyHover(graphics, mouseX, mouseY);
        }
    }

    /**
     * The click/drag-to-seek scrubber with keyframe tick marks: a framed box exactly
     * {@link #TIMELINE_WIDTH} wide and centred (not a near-full-width strip), with click-snapping to
     * the nearest keyframe (see {@link #hoveredKeyframeIndex}), a two-band fill ({@code BAR_COLORS})
     * eased toward the scene's progress rather than snapped once per tick, and a hover highlight: the
     * tick mark the cursor would currently snap to (see {@link #renderKeyframeMarks}) grows and
     * brightens, including the two "virtual" marks at the very start/end that otherwise never render
     * at all.
     * <p>
     * Stays inline in the screen rather than as a separate widget, since PonderLib has no widget
     * base class with this hover/click behaviour built in. Drag-to-scrub snaps the same way a click
     * would on every dragged frame. Hidden while the scene has no measurable duration yet (right at
     * scene start, before {@code begin()} sizes it).
     */
    private void renderTimeline(GuiGraphics graphics, int mouseX, int mouseY) {
        if (scene().getTotalTime() <= 0) {
            return;
        }
        int barX = timelineX();
        int barY = timelineY();
        int barWidth = timelineWidth();

        new PonderBoxElement()
            .withBackground(0xFF000000)
            .gradientBorder(scene().getColors().frameBorderTop(), scene().getColors().frameBorderBottom())
            .at(barX, barY, 400)
            .withBounds(barWidth, 1)
            .render(graphics);

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        // The fill and marks are drawn in a frame offset by (-2,-2) from the bar's own origin, so
        // every coordinate below is relative to that.
        poseStack.translate(barX - 2, barY - 2, 100);

        // z matters here: the frame above is drawn IMMEDIATE-mode at z=400, while these fills are
        // queued into the batched gui render type and flushed later - at the default z=0 they would
        // lose the depth test against the frame's opaque background and never appear at all.
        // FILL_Z/MARK_Z (310/320) land at 410/420 absolute inside this translate(..., 100) frame,
        // in front of the 400 frame.
        int filled = Math.round((barWidth + 4) * timelineProgressValue);
        graphics.fill(0, 1, filled, TIMELINE_FILL_SPLIT, FILL_Z, scene().getColors().timelineFillTop());
        graphics.fill(0, TIMELINE_FILL_SPLIT, filled, TIMELINE_FILL_HEIGHT, FILL_Z, scene().getColors().timelineFillBottom());

        // -2: no valid hover index (nothing to ever equal) - the sentinel for "cursor isn't over the
        // bar at all" (also covers "no keyframes at all", since hoveredKeyframeIndex assumes at
        // least one exists).
        int hoverIndex = scene().getKeyframeCount() > 0 && isOverTimeline(mouseX, mouseY) ? hoveredKeyframeIndex(mouseX) : -2;
        renderKeyframeMarks(graphics, barWidth, hoverIndex);

        poseStack.popPose();
    }

    /**
     * Advances the eased bar fill one tick towards the scene's real progress: an exponential
     * approach with a 0.5 factor per tick, kept as a plain field rather than a general lerped-value
     * abstraction. Without it the fill would jump a whole tick's worth at a time instead of easing
     * smoothly.
     */
    private void tickTimelineProgress() {
        timelineProgressValue = chaseTimelineProgress(timelineProgressValue, scene().getSceneProgress());
    }

    /**
     * One tick of the bar fill's easing: move {@code current} a {@link #TIMELINE_CHASE_FACTOR} share
     * of the way towards {@code target}, snapping once it's within a sub-pixel of it so the bar
     * actually reaches its end instead of asymptoting towards it forever. Package-visible and pure
     * so {@code PonderTimelineProgressTest} can assert this directly.
     */
    static float chaseTimelineProgress(float current, float target) {
        float next = current + (target - current) * TIMELINE_CHASE_FACTOR;
        return Math.abs(target - next) < 1 / 512F ? target : next;
    }

    /**
     * One tick of the scene-switch slide: {@code lazyIndexValue} chases {@code index} exactly the way
     * {@link #tickTimelineProgress} chases the scene's progress — same shape, its own factor
     * ({@code 1/4} here).
     */
    private void tickLazyIndex() {
        lazyIndexValue = chaseLazyIndex(lazyIndexValue, index);
    }

    /**
     * {@link #chaseTimelineProgress}'s exact shape with the slide's own factor - kept as its own pure
     * method (rather than a shared helper taking the factor as a parameter) so both stay directly
     * testable against their own distinct hardcoded constants without a mismatch being possible to
     * introduce by threading the wrong one through by mistake.
     */
    static float chaseLazyIndex(float current, float target) {
        float next = current + (target - current) * LAZY_INDEX_CHASE_FACTOR;
        return Math.abs(target - next) < 1 / 512F ? target : next;
    }

    /**
     * The camera-space push a scene at index {@code i} gets while the slide is under way: {@code diff}
     * is how many scene-widths away index {@code i} currently sits from the animated
     * {@code lazyIndexValue}, and the offset grows super-linearly with it
     * ({@code Mth.lerp(diff * diff, 200, 600) * diff}) — a small nudge as a scene starts sliding,
     * accelerating as it goes, so it clears the screen instead of drifting off slowly. Zero exactly when
     * {@code i == lazyIndexValue} (nothing to slide), which is always eventually true for the active
     * scene once the chase in {@link #tickLazyIndex} snaps.
     */
    static double slideOffset(int i, float lazyIndexValue) {
        double diff = i - lazyIndexValue;
        return Mth.lerp(diff * diff, 200, 600) * diff;
    }

    private void renderKeyframeMarks(GuiGraphics graphics, int barWidth, int hoverIndex) {
        int totalTime = scene().getTotalTime();
        int keyframeCount = scene().getKeyframeCount();

        // Two pseudo-keyframes, drawn only while hovered: "rewind to the very start" and
        // "skip to the very end".
        if (hoverIndex == -1) {
            drawKeyframeMark(graphics, 0, 0, true);
        } else if (hoverIndex == keyframeCount) {
            drawKeyframeMark(graphics, barWidth + 4, totalTime, true);
        }

        for (int i = 0; i < keyframeCount; i++) {
            int keyframeTime = scene().getKeyframeTime(i);
            // The denominator here is (width + 2) while the click math in hoveredKeyframeIndex uses
            // (width + 4) - kept as two separate constants rather than unified, since a mark's drawn
            // position and its click-snap target don't need to be pixel-identical.
            int x = Math.round(keyframeTime / (float) totalTime * (barWidth + 2));
            drawKeyframeMark(graphics, x, keyframeTime, i == hoverIndex);
        }
    }

    /**
     * One 2px-wide mark hanging down from the bar - {@code KEYFRAME_HEIGHT_IDLE} tall normally,
     * {@code KEYFRAME_HEIGHT_HOVER} when hovered, in which case a second bar and a {@code <}/{@code >}
     * glyph appear below it showing whether seeking there means going back or forward.
     */
    private void drawKeyframeMark(GuiGraphics graphics, int x, int keyframeTime, boolean hovered) {
        int alpha = hovered ? scene().getColors().keyframeAlphaHover() : scene().getColors().keyframeAlphaIdle();
        int markHeight = hovered ? KEYFRAME_HEIGHT_HOVER : KEYFRAME_HEIGHT_IDLE;
        int color = (alpha << 24) | scene().getColors().keyframeTint();

        graphics.fill(x, 0, x + 2, 1 + markHeight, MARK_Z, color);

        if (!hovered) {
            return;
        }
        graphics.fill(x, 9, x + 2, 9 + markHeight, MARK_Z, color);
        boolean forward = scene().getCurrentTime() < keyframeTime;
        String glyph = forward ? ">" : "<";
        int glyphX = forward ? x - 2 - font.width(glyph) : x + 4;
        // drawString has no z parameter, so the glyph gets there by translating the pose instead.
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, MARK_Z);
        graphics.drawString(font, Component.literal(glyph).withStyle(ChatFormatting.BOLD), glyphX, 10, color, false);
        poseStack.popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && scene().getTotalTime() > 0 && isOverTimeline(mouseX, mouseY)) {
            scrubbingTimeline = true;
            seekToMouseX(mouseX);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrubbingTimeline && button == 0) {
            seekToMouseX(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            scrubbingTimeline = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void seekToMouseX(double mouseX) {
        int totalTime = scene().getTotalTime();
        if (totalTime <= 0) {
            return;
        }
        if (scene().getKeyframeCount() == 0) {
            // No chapters to snap to - free proportional seeking is all there is.
            double fraction = Mth.clamp((mouseX - timelineX()) / timelineWidth(), 0, 1);
            seekTo((int) Math.round(fraction * totalTime));
            return;
        }

        int keyframeIndex = hoveredKeyframeIndex(mouseX);
        if (keyframeIndex == -1) {
            seekTo(0);
        } else if (keyframeIndex == scene().getKeyframeCount()) {
            seekTo(totalTime);
        } else {
            seekTo(scene().getKeyframeTime(keyframeIndex));
        }
    }

    // scene().seekToTime(time) calls PonderScene#begin() - resetting every bit of the SCENE's own
    // state - whenever time < currentTime (rewinding). But the finishing-flash glow (see
    // #tickFinishingFlash/#renderBasePlateShadowAndFlash) lives on THIS class, not PonderScene, and
    // by design never fades back out on its own once triggered (see #tickFinishingFlash). Without
    // this explicit reset, a scene that had ever finished once would keep the flash stuck at full
    // opacity forever after, even right after rewinding all the way back to the start. Mirrors
    // PonderScene#seekToTime's own "time < currentTime means this is a rewind" condition exactly,
    // so the two resets can never drift out of sync with each other.
    private void seekTo(int time) {
        if (time < scene().getCurrentTime()) {
            finishingFlashWarmup = 0;
            finishingFlashPrevious = 0F;
            finishingFlashValue = 0F;
            nextUpWarmup = 0;
            nextUpPrevious = 0F;
            nextUpValue = 0F;
        }
        scene().seekToTime(time);
    }

    /**
     * The index of the keyframe the given mouse X snaps to. Note this floors to the PREVIOUS keyframe for
     * any click between two keyframes - the only place it picks the "next" one is the final
     * segment past the last keyframe, where {@code diffToEnd < diffToLast / 2} special-cases a
     * click closer to the end. {@code -1} means "before the first" (seek to 0),
     * {@code getKeyframeCount()} means "past the last" (seek to the end).
     */
    private int hoveredKeyframeIndex(double mouseX) {
        int totalTime = scene().getTotalTime();
        int clickedAtTime = (int) ((mouseX - timelineX()) / (double) timelineWidth() * totalTime);

        int lastKeyframeTime = scene().getKeyframeTime(scene().getKeyframeCount() - 1);
        int diffToEnd = totalTime - clickedAtTime;
        int diffToLast = clickedAtTime - lastKeyframeTime;
        if (diffToEnd > 0 && diffToEnd < diffToLast / 2) {
            return scene().getKeyframeCount();
        }

        int index = -1;
        for (int i = 0; i < scene().getKeyframeCount(); i++) {
            int keyframeTime = scene().getKeyframeTime(i);
            if (keyframeTime > clickedAtTime) {
                break;
            }
            index = i;
        }
        return index;
    }

    private boolean isOverTimeline(double mouseX, double mouseY) {
        int barX = timelineX();
        int barY = timelineY();
        int barWidth = timelineWidth();
        // Deliberately generous hitbox - it reaches 4px past the right end and a full 20px BELOW the
        // bar, so the hovered keyframe's mark and its </> glyph (drawn down there) stay grabbable
        // instead of the cursor falling off the widget the moment it leaves the 1px bar.
        return mouseX >= barX && mouseX < barX + barWidth + 4 && mouseY >= barY - 3 && mouseY < barY + 21;
    }

    private int timelineX() {
        return (width - TIMELINE_WIDTH) / 2;
    }

    private int timelineY() {
        return height - TIMELINE_BOTTOM_OFFSET;
    }

    private int timelineWidth() {
        return TIMELINE_WIDTH;
    }

    // Public so foundation.PonderSceneParticles can calibrate its own PonderSceneCamera's
    // rotation against these exact same values instead of duplicating the -35/55 literals - see
    // PonderSceneCamera's javadoc for the derivation.
    public static final float CAMERA_X_ROTATION = -35F;
    public static final float CAMERA_Y_ROTATION = 55F;

    /**
     * This screen's font — {@link Screen#font} is protected, and overlay elements live in another
     * package.
     */
    public Font getFont() {
        return font;
    }

    /**
     * Projects a point in scene (block) space to the screen position it renders at. This is what
     * lets an overlay ATTACH to a place in the scene rather than sit at a fixed screen position —
     * see {@code element.TextWindowElement}'s {@code pointAt}. {@code sceneAt} matters, not just {@code
     * scenePos}: during the scene-switch slide, each scene has its own {@link #slideOffset}, so an
     * overlay anchored to the scene sliding away projects through a different matrix than one
     * anchored to the active scene.
     */
    public Vec2 sceneToScreen(PonderScene sceneAt, Vec3 scenePos) {
        int i = scenes.indexOf(sceneAt);
        double offset = i >= 0 ? slideOffset(i, lazyIndexValue) : 0;
        PoseStack poseStack = new PoseStack();
        applySceneTransform(poseStack, sceneAt, offset);
        Vector4f projected = new Vector4f((float) scenePos.x, (float) scenePos.y, (float) scenePos.z, 1F)
            .mul(poseStack.last().pose());
        return new Vec2(projected.x(), projected.y());
    }

    /**
     * The fixed isometric-ish camera transform every scene renders through, plus the scene-switch
     * slide's own horizontal+depth push - factored out of {@link #renderScene(GuiGraphics, float, int)}
     * so identify mode's hover picking ({@link #renderIdentifyHover}) and {@link #sceneToScreen} can
     * forward-project through the exact same matrix instead of duplicating (and risking a mismatch
     * with) this math.
     * <p>
     * {@code offset} is inserted between the fixed {@code -35}/{@code 55} rotation and the scale/
     * focus-point translation that follows it, so the slide pushes the scene sideways in camera
     * space rather than world space - its direction stays the same regardless of which way the
     * camera itself is tilted.
     */
    private void applySceneTransform(PoseStack poseStack, PonderScene sceneAt, double offset) {
        poseStack.translate(width / 2.0, height / 2.0 - 20, 200 + offset);
        poseStack.mulPose(Axis.XP.rotationDegrees(CAMERA_X_ROTATION));
        poseStack.mulPose(Axis.YP.rotationDegrees(CAMERA_Y_ROTATION));
        poseStack.translate(offset, 0, 0);
        poseStack.scale(1, -1, 1);
        poseStack.scale(30, 30, 30);
        Vec3 focus = sceneAt.getFocusPoint();
        poseStack.translate(-focus.x, -focus.y, -focus.z);
    }

    /**
     * The active scene's own camera transform at zero-or-current slide offset - the convenience
     * identify-mode hover picking uses, since hover picking only ever targets the active scene.
     */
    private void applySceneTransform(PoseStack poseStack) {
        applySceneTransform(poseStack, scene(), slideOffset(index, lazyIndexValue));
    }

    private void renderScene(GuiGraphics graphics, float partialTick, int i) {
        PonderScene sceneAt = scenes.get(i);
        double offset = slideOffset(i, lazyIndexValue);

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        applySceneTransform(poseStack, sceneAt, offset);

        RenderSystem.enableDepthTest();
        RenderSystem.setupLevelDiffuseLighting(DIFFUSE_LIGHT_0, DIFFUSE_LIGHT_1, new Matrix4f());
        for (PonderElement element : sceneAt.getElements()) {
            if (element instanceof PonderSceneElement sceneElement && element.isVisible()) {
                sceneElement.render(poseStack, graphics.bufferSource(), partialTick);
            }
        }
        // Entities aren't PonderSceneElements (see EntityElement's javadoc) - PonderLevel renders
        // them directly through the real EntityRenderDispatcher, queued into this same buffer source
        // so the single endBatch() below flushes blocks and entities together.
        sceneAt.getLevel().renderEntities(poseStack, graphics.bufferSource(), partialTick);
        renderBasePlateShadowAndFlash(poseStack, graphics.bufferSource(), partialTick, sceneAt);
        graphics.bufferSource().endBatch();
        // After endBatch, not before: particles draw immediately (their own shader + BufferUploader,
        // see PonderSceneParticles#render) rather than being queued into the buffer source, so
        // drawing them first would put them behind geometry that hasn't been flushed yet.
        sceneAt.getParticles().render(poseStack, partialTick);
        RenderSystem.disableDepthTest();

        poseStack.popPose();
    }

    /**
     * Shows a vanilla item tooltip for whichever block is under the cursor while identify mode is
     * active, found via 3D unprojection against this codebase's own block bookkeeping rather than a
     * depth buffer: the camera transform ({@link #applySceneTransform}) plus each section's own
     * transform ({@link WorldSectionElementImpl#getSectionTransform()}) is inverted, turning the
     * on-screen cursor position into a genuine 3D ray through that section's local block-space,
     * which is then intersected against each block's actual unit-cube {@link AABB} - the same
     * ray/box routine vanilla's own block picking uses ({@link AABB#clip}).
     * <p>
     * Among every block the ray hits, the one whose hit point re-projects to the LARGEST GUI-space
     * depth wins - "closest to the viewer" means larger Z here, not smaller. Easy to get backwards:
     * this matches the same ordering {@code RenderSystem.enableDepthTest()} enforces when the scene
     * itself is drawn (see {@link #renderScene}), but is not the intuitive "smaller wins" one might
     * assume from screen-space distance alone.
     * <p>
     * Skips every {@link WorldSectionElementImpl#isBasePlate()} section entirely - the floor is
     * scenery, not a subject, so hovering it falls through to whatever's behind it, or to the plain
     * "hold [key] to identify" hint if nothing else is under the cursor, exactly as if nothing were
     * being hovered at all.
     */
    private void renderIdentifyHover(GuiGraphics graphics, int mouseX, int mouseY) {
        if (mouseY > height - 80) {
            return;
        }

        PoseStack cameraPose = new PoseStack();
        applySceneTransform(cameraPose);
        Matrix4f camera = new Matrix4f(cameraPose.last().pose());

        // Two points at the same screen position but different GUI-space depths define the ray
        // through the cursor; -10000/+10000 is arbitrary but comfortably spans any scene's local
        // (schematic-sized) coordinate range once unprojected, so the ray's local-space endpoints
        // always fall well outside every block it could possibly hit.
        Vector4f guiNear = new Vector4f(mouseX + 0.5F, mouseY + 0.5F, -10000F, 1F);
        Vector4f guiFar = new Vector4f(mouseX + 0.5F, mouseY + 0.5F, 10000F, 1F);

        WorldSectionElementImpl hoveredSection = null;
        BlockPos hoveredPos = null;
        Matrix4f hoveredBlockBase = null;
        float bestDepth = Float.NEGATIVE_INFINITY;

        for (PonderElement element : scene().getElements()) {
            if (!(element instanceof WorldSectionElementImpl section) || !element.isVisible() || section.isBasePlate()) {
                continue;
            }
            Matrix4f blockBase = new Matrix4f(camera).mul(section.getSectionTransform());
            Matrix4f inverse = new Matrix4f(blockBase).invert();

            Vector4f localNear = new Vector4f(guiNear).mul(inverse);
            Vector4f localFar = new Vector4f(guiFar).mul(inverse);
            Vec3 rayFrom = new Vec3(localNear.x(), localNear.y(), localNear.z());
            Vec3 rayTo = new Vec3(localFar.x(), localFar.y(), localFar.z());

            for (BlockPos pos : section.getBlockPositions()) {
                Optional<Vec3> hit = new AABB(pos).clip(rayFrom, rayTo);
                if (hit.isEmpty()) {
                    continue;
                }
                Vec3 local = hit.get();
                float depth = new Vector4f((float) local.x, (float) local.y, (float) local.z, 1F).mul(blockBase).z();
                // Larger Z is closer to the viewer in this projection - see this method's javadoc.
                if (depth > bestDepth) {
                    bestDepth = depth;
                    hoveredSection = section;
                    hoveredPos = pos;
                    hoveredBlockBase = blockBase;
                }
            }
        }

        if (hoveredSection == null) {
            graphics.renderTooltip(font, IDENTIFY_HINT, mouseX, mouseY);
            return;
        }
        renderHoveredBlockOutline(graphics, hoveredBlockBase, hoveredPos);
        BlockState state = hoveredSection.getBlockState(hoveredPos);
        ItemStack stack = new ItemStack(state.getBlock());
        // stack.getHoverName() as a plain Component, NOT graphics.renderTooltip(font, stack, ...):
        // that overload fires ItemTooltipEvent for the stack, which PonderTooltipHandler#addToTooltip
        // listens to globally - hovering the very block THIS ponder is about would nest that scene's
        // own "Hold [key] to Ponder" hint and hold-progress inside identify mode's tooltip, which
        // makes no sense (you're already looking at its ponder) and could fire setScreen() out from
        // under the identify-mode session it's nested in once held long enough.
        if (stack.isEmpty()) {
            // No item form (e.g. fluids, some technical blocks) - fall back to its registry name
            // instead of rendering an empty/blank tooltip.
            Component fallback = Component.literal(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            graphics.renderTooltip(font, fallback, mouseX, mouseY);
        } else {
            graphics.renderTooltip(font, stack.getHoverName(), mouseX, mouseY);
        }
    }

    // Blocks in world-space (fraction of one block) - a 1px GL line stays a fixed 1px regardless of
    // how big the block reads on screen, so at this scene's zoom it would look like a thin scratch
    // rather than a bold cube outline. Each of the cube's 12 edges is instead its own small solid
    // box (rendered the same hand-quad way as the shadow/flash effects elsewhere in this class),
    // which stays visually thick regardless of GL line-width support.
    private static final float OUTLINE_THICKNESS = 0.05F;

    // Depth-tests normally against the rest of the scene (LEQUAL_DEPTH_TEST + depth write, same as
    // real geometry) rather than forcing itself always-on-top: a highlight box should only show the
    // part of itself that's actually visible from the camera, not X-ray through solid blocks in
    // front of it. NO_TRANSPARENCY (not TRANSLUCENT_TRANSPARENCY) keeps color writes an opaque
    // overwrite instead of blending, avoiding a partial-coverage seam at polygon edges.
    private static final RenderType OUTLINE_RENDER_TYPE = RenderType.create(
        "ponderlib_identify_outline",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(PonderRenderStateShards.POSITION_COLOR_SHADER)
            .setTransparencyState(PonderRenderStateShards.NO_TRANSPARENCY)
            .setCullState(PonderRenderStateShards.NO_CULL)
            .createCompositeState(false)
    );

    /**
     * Highlights the block a tooltip in identify mode is currently describing - with multiple blocks
     * in a scene it's otherwise genuinely ambiguous which one a given tooltip belongs to. Driven
     * through the exact {@code blockBase} matrix ({@code camera × sectionTransform}) that both
     * rendering and the hover ray-cast already use, against the same {@code [pos, pos+1]} unit cube
     * the ray-cast tests against - so the outline can never drift out of sync with either.
     */
    private void renderHoveredBlockOutline(GuiGraphics graphics, Matrix4f blockBase, BlockPos pos) {
        VertexConsumer consumer = graphics.bufferSource().getBuffer(OUTLINE_RENDER_TYPE);

        float minX = pos.getX(), minY = pos.getY(), minZ = pos.getZ();
        float maxX = minX + 1, maxY = minY + 1, maxZ = minZ + 1;
        float t = OUTLINE_THICKNESS / 2F;

        // Each of the cube's 12 edges connects two corners differing in exactly one coordinate -
        // render every edge as its own box, inflated by +-t on its two CONSTANT axes and left at
        // the full corner-to-corner span on its one VARYING axis (so adjoining edges' boxes meet
        // exactly at the shared corner with no gap and no overlap beyond it).
        for (float y : new float[]{minY, maxY}) {
            for (float z : new float[]{minZ, maxZ}) {
                addOutlineBox(consumer, blockBase, minX, y - t, z - t, maxX, y + t, z + t);
            }
        }
        for (float x : new float[]{minX, maxX}) {
            for (float z : new float[]{minZ, maxZ}) {
                addOutlineBox(consumer, blockBase, x - t, minY, z - t, x + t, maxY, z + t);
            }
        }
        for (float x : new float[]{minX, maxX}) {
            for (float y : new float[]{minY, maxY}) {
                addOutlineBox(consumer, blockBase, x - t, y - t, minZ, x + t, y + t, maxZ);
            }
        }

        graphics.bufferSource().endBatch();
    }

    private static void addOutlineBox(VertexConsumer consumer, Matrix4f pose,
                                       float x0, float y0, float z0, float x1, float y1, float z1) {
        outlineQuad(consumer, pose, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1);
        outlineQuad(consumer, pose, x1, y0, z1, x1, y1, z1, x1, y1, z0, x1, y0, z0);
        outlineQuad(consumer, pose, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        outlineQuad(consumer, pose, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
        outlineQuad(consumer, pose, x1, y0, z0, x1, y1, z0, x0, y1, z0, x0, y0, z0);
        outlineQuad(consumer, pose, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1);
    }

    private static void outlineQuad(VertexConsumer consumer, Matrix4f pose,
                                     float x1, float y1, float z1, float x2, float y2, float z2,
                                     float x3, float y3, float z3, float x4, float y4, float z4) {
        outlineVertex(consumer, pose, x1, y1, z1);
        outlineVertex(consumer, pose, x2, y2, z2);
        outlineVertex(consumer, pose, x3, y3, z3);
        outlineVertex(consumer, pose, x4, y4, z4);
    }

    private static void outlineVertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z) {
        Vector4f pos = new Vector4f(x, y, z, 1F).mul(pose);
        consumer.vertex(pos.x(), pos.y(), pos.z()).color(255, 255, 255, 255).endVertex();
    }

    /**
     * One loop walking the base plate's 4 perimeter edges, drawing a vertical gradient plane on
     * each — a dark skirt hanging DOWN below the plate, and (once the scene finishes) a pale glow
     * rising UP from the same edge. Both drawn in the same pass, in the same rotating local frame.
     * <p>
     * Two things here are load-bearing and easy to get wrong:
     * <ul>
     * <li>The flip applied by the scene transform (see {@link #applySceneTransform}) is undone a
     * SECOND time here, so inside this block +Y points DOWN again like it does in a GUI. That's why
     * the shadow's rect is y {@code 0..4} (downward) and the flash's is y {@code -1..0} (upward).
     * <li>The two planes are separated only by {@code ∓1/1024} of a block along the edge's own
     * outward normal — the flash a hair outside the plate's wall, the shadow a hair inside it. That
     * sub-pixel split is what keeps them from z-fighting each other and lets the shadow clip cleanly
     * against the plate's own side face.
     * </ul>
     * The four vertices and their two gradient colours are emitted by hand rather than through a
     * screen-space fill helper, since those typically take {@code int} coordinates and this base
     * plate's footprint (derived from whichever sections a scene reveals) isn't necessarily a whole
     * number of blocks across.
     */
    private void renderBasePlateShadowAndFlash(PoseStack poseStack, MultiBufferSource buffer, float partialTick,
                                                PonderScene sceneAt) {
        double minX = sceneAt.getBasePlateMinX();
        double maxX = sceneAt.getBasePlateMaxX();
        double minZ = sceneAt.getBasePlateMinZ();
        double maxZ = sceneAt.getBasePlateMaxZ();

        // alpha is the raw ramp; `flash` is squared/remapped/squared/inverted into a bell that
        // drives only the glow's HEIGHT, so it grows past its resting size and settles back rather
        // than just fading in.
        float raw = getFinishingFlash(partialTick) * 0.9F;
        final float alpha = raw;
        float curve = raw;
        curve *= curve;
        curve = curve * 2 - 1;
        curve *= curve;
        final float flash = 1 - curve;

        // Scale the skirt by the least-faded visible section, so it doesn't snap to full strength
        // while the section it grounds is still fading in.
        int shadowAlpha = Math.round(0x66 * sectionFade(sceneAt));

        int flashColor = sceneAt.getColors().finishingFlash();
        int shadowColor = sceneAt.getColors().basePlateShadow();
        VertexConsumer consumer = buffer.getBuffer(OVERLAY_RENDER_TYPE);

        forEachPerimeterSide(poseStack, minX, maxX, minZ, maxZ, (pose, span) -> {
            pose.translate(0, 0, -1 / 1024F);
            if (flash > 0 && alpha > 0) {
                pose.pushPose();
                pose.scale(1, 0.5F + flash * 0.75F, 1);
                // Transparent at the top (y=-1, i.e. up), brightest at the plate's edge (y=0).
                gradientQuad(consumer, pose.last().pose(), -span,
                    -1F, flashColor, 0,
                    0F, flashColor, Math.round(0xAA * alpha));
                pose.popPose();
            }

            pose.translate(0, 0, 2 / 1024F);
            // Dark at the plate's edge (y=0), fading out 4 blocks below it (y=4, i.e. down).
            gradientQuad(consumer, pose.last().pose(), -span,
                0F, shadowColor, shadowAlpha,
                4F, shadowColor, 0);
        });
    }

    @FunctionalInterface
    interface PerimeterSideRenderer {
        /**
         * Draws one side, in a frame whose origin is that side's FAR end, whose +X runs back along the
         * side (so the side spans x {@code 0..-span}), whose +Y points DOWN, and whose -Z points out
         * of the plate.
         */
        void draw(PoseStack pose, float span);
    }

    /**
     * Walks the base plate's 4 perimeter edges: translate along the current side, hand the frame to
     * {@code renderer}, then {@code mulPose(YP.rotationDegrees(-90))} for the next one — so the
     * caller only has to describe what one side looks like.
     * <p>
     * Split out from {@link #renderBasePlateShadowAndFlash} to be directly testable: this is pure
     * matrix bookkeeping with a sign that's easy to get backwards (the enclosing flip means "rotate
     * -90 about Y" isn't obviously the direction that traces the plate rather than walking off it),
     * and it can't be checked by eye from here. {@code PonderBasePlateShadowGeometryTest} asserts the
     * four sides land exactly on the footprint's four edges. Spans alternate X/Z rather than a single
     * hardcoded size, so a non-square footprint still closes.
     */
    static void forEachPerimeterSide(PoseStack poseStack, double minX, double maxX, double minZ, double maxZ,
                                      PerimeterSideRenderer renderer) {
        poseStack.pushPose();
        // The second flip - see renderBasePlateShadowAndFlash's javadoc. +Y is DOWN from here on.
        poseStack.scale(1, -1, 1);
        poseStack.translate(minX, 0, minZ);

        double[] spans = {maxX - minX, maxZ - minZ, maxX - minX, maxZ - minZ};
        for (double span : spans) {
            poseStack.translate(span, 0, 0);
            poseStack.pushPose();
            renderer.draw(poseStack, (float) span);
            poseStack.popPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-90));
        }
        poseStack.popPose();
    }

    /**
     * The four vertices {@code GuiGraphics.fillGradient} would emit for the rect spanning x
     * {@code 0..x2} and y {@code yFrom..yTo}, in the same order and with the same per-corner colours
     * (both {@code yFrom} corners get {@code colorFrom}/{@code alphaFrom}, both {@code yTo} corners
     * get {@code colorTo}/{@code alphaTo}), at z = 0 in the current pose.
     */
    private static void gradientQuad(VertexConsumer consumer, Matrix4f pose, float x2,
                                      float yFrom, int colorFrom, int alphaFrom,
                                      float yTo, int colorTo, int alphaTo) {
        gradientVertex(consumer, pose, 0, yFrom, colorFrom, alphaFrom);
        gradientVertex(consumer, pose, 0, yTo, colorTo, alphaTo);
        gradientVertex(consumer, pose, x2, yTo, colorTo, alphaTo);
        gradientVertex(consumer, pose, x2, yFrom, colorFrom, alphaFrom);
    }

    /**
     * The minimum {@link WorldSectionElementImpl#getFade()} across every currently-shown section —
     * 1 once everything has landed, dropping toward 0 while anything is still fading in. Defaults to
     * 1 when there's no section yet (nothing to sync against).
     */
    private float sectionFade(PonderScene sceneAt) {
        float minFade = 1F;
        boolean any = false;
        for (PonderElement element : sceneAt.getElements()) {
            if (element instanceof WorldSectionElementImpl section) {
                minFade = Math.min(minFade, section.getFade());
                any = true;
            }
        }
        return any ? minFade : 1F;
    }

    private static void gradientVertex(VertexConsumer consumer, Matrix4f pose, float x, float y, int rgb, int alpha) {
        Vector4f pos = new Vector4f(x, y, 0F, 1F).mul(pose);
        consumer.vertex(pos.x(), pos.y(), pos.z())
            .color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, alpha)
            .endVertex();
    }

    private void renderOverlay(GuiGraphics graphics, float partialTick, PonderScene sceneAt) {
        for (PonderElement element : sceneAt.getElements()) {
            if (element instanceof PonderOverlayElement overlayElement && element.isVisible()) {
                overlayElement.render(sceneAt, graphics, partialTick);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
