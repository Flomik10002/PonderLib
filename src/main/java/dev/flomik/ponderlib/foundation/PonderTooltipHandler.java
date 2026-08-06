package dev.flomik.ponderlib.foundation;

import com.google.common.base.Strings;
import com.mojang.blaze3d.platform.InputConstants;
import dev.flomik.ponderlib.Config;
import dev.flomik.ponderlib.api.PonderColorScheme;
import dev.flomik.ponderlib.api.registration.StoryBoardEntry;
import dev.flomik.ponderlib.foundation.registration.PonderSceneRegistry;
import dev.flomik.ponderlib.foundation.ui.PonderUI;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * The item-tooltip hold-key trigger (see {@code client.ClientEvents} for the event wiring): a
 * plain float advanced once per game tick, no inter-tick smoothing needed at 20 ticks/sec. Also
 * shows a green "this item is the subject of the currently-open PonderUI" hint (see {@link
 * #addToTooltip}) via {@code foundation.ui.PonderUI#getSubject()}.
 */
public final class PonderTooltipHandler {

    private static final int PROGRESS_BAR_LENGTH = 40;

    private static float holdKeyProgress;
    private static ItemStack trackingStack = ItemStack.EMPTY;
    private static boolean sawTooltipThisTick;

    private static float worldLookupProgress;
    private static ResourceLocation worldLookupComponent;
    private static boolean showingActionBarHint;

    private PonderTooltipHandler() {
    }

    /**
     * Called once per client tick. Advances (or decays) {@link #holdKeyProgress} based on whether
     * an {@link net.minecraftforge.event.entity.player.ItemTooltipEvent} for a ponder-able item
     * fired since the last tick (tracked via {@link #sawTooltipThisTick}, since tooltip events only
     * fire while a screen is actually drawing one — there's no other way to ask "is a tooltip
     * currently shown").
     */
    public static void tick(KeyMapping ponderKey) {
        if (!sawTooltipThisTick || trackingStack.isEmpty()) {
            trackingStack = ItemStack.EMPTY;
            holdKeyProgress = 0;
            sawTooltipThisTick = false;
            return;
        }
        sawTooltipThisTick = false;

        // KeyMapping#isDown() is not usable here: KeyboardHandler#keyPress only calls
        // KeyMapping.set(key, true) when Minecraft.screen == null, so it never updates while any
        // screen (e.g. the inventory the tooltip is shown in) is open. Poll the raw GLFW state
        // directly instead.
        boolean down = InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), ponderKey.getKey().getValue());
        if (down) {
            if (holdKeyProgress >= 1) {
                Minecraft.getInstance().setScreen(PonderUI.of(trackingStack));
                holdKeyProgress = 0;
                return;
            }
            holdKeyProgress = Math.min(1, holdKeyProgress + Math.max(0.25f, holdKeyProgress) * 0.25f);
        } else {
            holdKeyProgress = Math.max(0, holdKeyProgress - 0.05f);
        }
    }

    /**
     * A world-lookup trigger, not to be confused with {@code foundation.ui.PonderUI}'s own {@code
     * identifyMode} field (a pause-and-inspect toggle INSIDE an already-open scene) - this one
     * instead triggers OPENING a scene: hold the Ponder key while looking at a placed block (not a
     * screen/tooltip) that has a registered scene. Deliberately mutually exclusive with {@link
     * #tick} - only does anything while no screen is open, which the tooltip flow's hold-key can
     * only ever fire during (a tooltip needs a screen to draw itself in), so the two never fight
     * over the held key.
     * <p>
     * There's no screen to draw a tooltip into here (that's the whole reason this exists
     * separately from {@link #tick}), so the hold hint/progress bar is shown in the action bar
     * ({@code Gui#setOverlayMessage}, the same HUD line vanilla uses for e.g. the resurrect/keep-
     * inventory message) instead, refreshed every tick the target block stays under the crosshair
     * and explicitly cleared the moment it isn't (so it doesn't linger for its normal ~3s fade-out
     * after looking away).
     */
    public static void tickWorldLookup(KeyMapping ponderKey) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) {
            clearActionBarHint(mc);
            worldLookupProgress = 0;
            worldLookupComponent = null;
            return;
        }

        ResourceLocation target = lookedAtSceneBlock(mc);
        if (target == null) {
            clearActionBarHint(mc);
            worldLookupProgress = 0;
            worldLookupComponent = null;
            return;
        }
        if (!target.equals(worldLookupComponent)) {
            worldLookupComponent = target;
            worldLookupProgress = 0;
        }

        boolean down = InputConstants.isKeyDown(mc.getWindow().getWindow(), ponderKey.getKey().getValue());
        if (down) {
            if (worldLookupProgress >= 1) {
                openFirstScene(mc, target);
                worldLookupProgress = 0;
                clearActionBarHint(mc);
                return;
            }
            worldLookupProgress = Math.min(1, worldLookupProgress + Math.max(0.25f, worldLookupProgress) * 0.25f);
        } else {
            worldLookupProgress = Math.max(0, worldLookupProgress - 0.05f);
        }

        mc.gui.setOverlayMessage(progressLine(worldLookupProgress, ponderKey), false);
        showingActionBarHint = true;
    }

    private static void clearActionBarHint(Minecraft mc) {
        if (showingActionBarHint) {
            mc.gui.setOverlayMessage(CommonComponents.EMPTY, false);
            showingActionBarHint = false;
        }
    }

    private static ResourceLocation lookedAtSceneBlock(Minecraft mc) {
        if (!(mc.hitResult instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK || mc.level == null) {
            return null;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(blockHit.getBlockPos()).getBlock());
        return PonderIndex.getScenes().doScenesExistForId(id) ? id : null;
    }

    private static void openFirstScene(Minecraft mc, ResourceLocation component) {
        Collection<StoryBoardEntry> entries = PonderIndex.getScenes().getScenes(component);
        Iterator<StoryBoardEntry> iterator = entries.iterator();
        if (iterator.hasNext()) {
            mc.setScreen(PonderUI.of(iterator.next()));
        }
    }

    public static void addToTooltip(List<Component> toolTip, ItemStack stack, KeyMapping ponderKey) {
        if (stack.isEmpty() || !Config.SHOW_TOOLTIP_HINT.get()) {
            return;
        }
        ResourceLocation component = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!PonderIndex.getScenes().doScenesExistForId(component)) {
            return;
        }

        // If this exact item is what the currently-open PonderUI's scene is about, show a plain
        // "already open" hint instead of the hold-to-open bar, and don't touch the hold-progress
        // state at all - holding the key to re-open something already open would be pointless.
        if (Minecraft.getInstance().screen instanceof PonderUI ponderUI) {
            ItemStack subject = ponderUI.getSubject();
            if (!subject.isEmpty() && stack.is(subject.getItem())) {
                toolTip.add(Math.min(1, toolTip.size()), Component.literal("Subject of this scene").withStyle(ChatFormatting.GREEN));
                return;
            }
        }

        sawTooltipThisTick = true;
        if (!trackingStack.is(stack.getItem())) {
            trackingStack = stack;
            holdKeyProgress = 0;
        }

        toolTip.add(Math.min(1, toolTip.size()), progressLine(holdKeyProgress, ponderKey));
    }

    public static Optional<Integer> tooltipBorderColor(ItemStack stack) {
        if (!trackingStack.is(stack.getItem()) || holdKeyProgress <= 0) {
            return Optional.empty();
        }
        ResourceLocation component = BuiltInRegistries.ITEM.getKey(stack.getItem());
        PonderColorScheme colors = PonderIndex.colorsFor(modIdFor(component));
        // Same *8/7 display remap as progressLine, applied before computing the border colour -
        // keeps the border's colour ramp in step with the bar instead of visibly lagging behind it.
        return Optional.of(0xFF000000 | colors.tooltipBorderForProgress(Math.min(1, holdKeyProgress * 8 / 7F)));
    }

    /**
     * The mod id owning {@code component}'s scene(s) - whichever mod's {@code PonderPlugin}
     * registered the FIRST entry for it (see {@link PonderSceneRegistry#getScenes}'s ordering).
     * {@code null} if nothing is registered for it at all, which {@link PonderIndex#colorsFor}
     * treats as "use PonderLib's own defaults".
     */
    @Nullable
    private static String modIdFor(ResourceLocation component) {
        Iterator<StoryBoardEntry> entries = PonderIndex.getScenes().getScenes(component).iterator();
        return entries.hasNext() ? entries.next().getSchematicLocation().getNamespace() : null;
    }

    private static Component progressLine(float progress, KeyMapping ponderKey) {
        Component holdMessage = Component.literal("Hold ")
            .append(ponderKey.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.GRAY))
            .append(" to Ponder")
            .withStyle(ChatFormatting.DARK_GRAY);

        if (progress <= 0) {
            return holdMessage;
        }

        // The raw hold value is remapped by *8/7 for display only (the trigger itself still fires
        // at the raw value reaching 1) - the bar (and border colour) visually fills up slightly
        // ahead of the hold actually completing, reading as snappier than a linear fill would.
        float displayProgress = Math.min(1, progress * 8 / 7F);
        int total = PROGRESS_BAR_LENGTH;
        int current = (int) (displayProgress * total);
        String bars = ChatFormatting.GRAY + Strings.repeat("|", current)
            + ChatFormatting.DARK_GRAY + Strings.repeat("|", total - current);
        return Component.literal(bars);
    }
}
