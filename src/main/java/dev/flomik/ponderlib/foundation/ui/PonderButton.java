package dev.flomik.ponderlib.foundation.ui;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.flomik.ponderlib.api.PonderColorScheme;
import dev.flomik.ponderlib.api.registration.PonderTag;
import dev.flomik.ponderlib.render.PonderBoxElement;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * One of the scene screen's buttons: a framed box that brightens on hover, an optional toggle
 * "flash" state for buttons that represent something being on, a callback, and an optional
 * keyboard shortcut whose compact key glyph replaces the icon on hover — that label is the whole
 * reason identify mode is discoverable at all rather than being a key nobody knows about.
 * <p>
 * Icons are drawn either from {@link Icon}'s pixel masks or from real item stacks. Tag icons and
 * ordinary item icons deliberately use different transforms: Create's {@code PonderTag#render}
 * uses 1.25x, while {@code PonderButton#showing(ItemStack)} uses 1.5x at (-4,-4).
 */
public class PonderButton extends AbstractWidget {

    public static final int SIZE = 20;

    /**
     * 8x8 pixel masks, drawn centred in the button. {@code '#'} is a lit pixel, anything else is
     * transparent. Deliberately readable as ASCII art so they can be edited by eye.
     */
    public enum Icon {
        CLOSE(
            "##    ##",
            "###  ###",
            " ###### ",
            "  ####  ",
            "  ####  ",
            " ###### ",
            "###  ###",
            "##    ##"),
        LEFT(
            "   ##   ",
            "  ##    ",
            " ##     ",
            "##      ",
            "##      ",
            " ##     ",
            "  ##    ",
            "   ##   "),
        RIGHT(
            "   ##   ",
            "    ##  ",
            "     ## ",
            "      ##",
            "      ##",
            "     ## ",
            "    ##  ",
            "   ##   "),
        // A hooked return glyph, distinct from the scene-paging chevrons.
        RETURN(
            "  ##    ",
            " ##     ",
            "##      ",
            "######  ",
            "##   ## ",
            "     ## ",
            "     ## ",
            "  ####  "),
        // Rewind-to-start: a stop bar the hollow chevron runs into.
        REPLAY(
            "#  ##   ",
            "# ##    ",
            "###     ",
            "##      ",
            "##      ",
            "###     ",
            "# ##    ",
            "#  ##   "),
        // Magnifier, for inspecting blocks.
        IDENTIFY(
            " ####   ",
            "#    #  ",
            "#    #  ",
            "#    #  ",
            "#    #  ",
            " ####   ",
            "     ## ",
            "      ##"),
        // Clock, for the slowed-down reading pace.
        SLOW(
            " ####   ",
            "#    #  ",
            "#  # #  ",
            "#  # #  ",
            "#  ###  ",
            "#    #  ",
            " ####   ",
            "        ");

        private static final int PIXELS = 8;

        private final String[] mask;

        Icon(String... mask) {
            this.mask = mask;
        }
    }

    // Colours come from whichever scene's own PonderColorScheme is active (see #colors) rather
    // than hardcoded constants - the idle border is the same pair the scrubber frame uses.
    // A quick exponential hover chase; rendering interpolates the previous/current samples.
    private static final float FADE_CHASE = .28F;
    private static final float PRESS_DECAY = .45F;
    private static final int CONTROL_ENTRANCE_TICKS = 6;
    private static final float CONTROL_HOVER_SCALE = .035F;
    private static final float CONTROL_PRESS_SCALE = .05F;
    private static final float CONTROL_HOVER_LIFT = 1.25F;
    private static final int SHORTCUT_INNER_WIDTH = 10;
    // Create-layout buttons use BoxWidget z=420; PonderLib's pre-existing controls keep their
    // higher layer because identify overlays and batched mask icons were authored around it.
    private static final int CREATE_FRAME_Z = 420;
    private static final int REGULAR_FRAME_Z = 600;
    private static final int CREATE_CONTENT_Z = 420;
    private static final int REGULAR_CONTENT_Z = 610;

    private static final float TAG_ICON_SCALE = 1.25F;
    private static final float ITEM_ICON_SCALE = 1.5F;
    private static final int CREATE_FRAME_INFLATION = 4;

    @Nullable
    private final Icon icon;
    @Nullable
    private final ItemStack itemIcon;
    private final boolean tagIcon;
    private final Runnable callback;
    // A supplier, not a fixed value: a button is built once in PonderUI#init, but which scene (and
    // so whose PonderColorScheme) is active can change afterwards (paging, the cross-fade slide).
    private final Supplier<PonderColorScheme> colors;
    @Nullable
    private KeyMapping shortcut;
    private boolean flashing;
    private boolean attention;
    private float attentionPrevious;
    private float attentionValue;
    private float fadePrevious;
    private float fade;
    private boolean wasHovered;
    private boolean createGradientInitialized;
    private float createColorPrevious;
    private float createColorValue;
    private int createGradientTop;
    private int createGradientBottom;
    private int createPreviousTop;
    private int createPreviousBottom;
    private int createTargetTop;
    private int createTargetBottom;
    private int createSchemeFrameTop;
    private int createSchemeFrameBottom;
    private int createSchemeHoverTop;
    private int createSchemeHoverBottom;
    private float flashPrevious;
    private float flashValue;
    private float screenFade = 1F;
    private boolean entranceFadeEnabled;
    private int entranceFadeModX;
    private int entranceFadeModY;
    private float entranceFadePrevious = 1F;
    private float entranceFadeValue = 1F;
    private boolean controlMotion;
    private int controlMotionAge;
    private int controlMotionDelay;
    private int controlIconDirection;
    private float pressPrevious;
    private float pressValue;
    // Set via withBorderColors - a fixed border instead of the scheme-driven hover gradient, for
    // e.g. PonderTagScreen's items with no registered scene. The supplied pair still comes from
    // PonderLib's scheme; it simply stays put instead of brightening on hover.
    private int borderOverrideTop;
    private int borderOverrideBottom;
    private boolean borderOverridden;
    private boolean createLayout;

    public PonderButton(int x, int y, Icon icon, Component label, Runnable callback, Supplier<PonderColorScheme> colors) {
        super(x, y, SIZE, SIZE, label);
        this.icon = icon;
        this.itemIcon = null;
        this.tagIcon = false;
        this.callback = callback;
        this.colors = colors;
        // Hand-drawn 8x8 icons can only carry so much meaning - a hover tooltip is what actually
        // makes each button self-describing.
        setTooltip(Tooltip.create(label));
    }

    /**
     * A button showing an arbitrary item's real icon rather than one of {@link Icon}'s pixel masks
     * - what {@link #showingTag} builds a tag's own button out of, and what {@code PonderTagScreen}
     * uses directly for the plain items listed under a tag (which aren't tags themselves, so
     * {@link #showingTag} doesn't apply to them).
     */
    public PonderButton(int x, int y, ItemStack itemIcon, Component label, Runnable callback, Supplier<PonderColorScheme> colors) {
        this(x, y, itemIcon, false, label, callback, colors);
    }

    private PonderButton(int x, int y, ItemStack itemIcon, boolean tagIcon, Component label,
                         Runnable callback, Supplier<PonderColorScheme> colors) {
        super(x, y, SIZE, SIZE, label);
        this.icon = null;
        this.itemIcon = itemIcon;
        this.tagIcon = tagIcon;
        this.callback = callback;
        this.colors = colors;
        this.createLayout = true;
    }

    /**
     * A tag button uses {@code PonderTag#render}'s item transform, not the 1.5x transform used by
     * associated item entries. It intentionally has no vanilla tooltip: Create reveals the title
     * in the separately clipped sidebar streak.
     */
    public static PonderButton showingTag(int x, int y, PonderTag tag, Runnable callback, Supplier<PonderColorScheme> colors) {
        return new PonderButton(x, y, tag.icon(), true, tag.title(), callback, colors);
    }

    /** Uses Create's nominal 20px box with its actual 4px-per-side frame and hit area. */
    public PonderButton withCreateLayout() {
        createLayout = true;
        setTooltip(null);
        return this;
    }

    /** Catnip ElementWidget.enableFade(...).fade(1): .1 EXP alpha/offset entrance. */
    public PonderButton withEntranceFade(int modifierX, int modifierY) {
        return withEntranceFade(modifierX, modifierY, 0);
    }

    /**
     * Resumes the entrance at the owning screen's age, so rebuilding widgets on resize does not
     * replay an animation that already finished.
     */
    public PonderButton withEntranceFade(int modifierX, int modifierY, int elapsedTicks) {
        entranceFadeEnabled = true;
        entranceFadeModX = modifierX;
        entranceFadeModY = modifierY;
        entranceFadePrevious = entranceFadeValue = entranceFadeAfterTicks(elapsedTicks);
        return this;
    }

    /** PonderLib's own six-tick, centre-out motion profile for the controls above the timeline. */
    public PonderButton withControlMotion(int delayTicks, int elapsedTicks, int iconDirection) {
        controlMotion = true;
        controlMotionDelay = Math.max(0, delayTicks);
        controlMotionAge = Math.max(0, elapsedTicks);
        controlIconDirection = Integer.signum(iconDirection);
        entranceFadeEnabled = true;
        entranceFadeModX = 0;
        entranceFadeModY = 6;
        entranceFadePrevious = entranceFadeValue = controlEntrance(controlMotionAge, controlMotionDelay);
        return this;
    }

    void setScreenFade(float screenFade) {
        this.screenFade = Mth.clamp(screenFade, 0F, 1F);
    }

    /**
     * Marks this button as also triggerable by {@code shortcut}, and draws that key's name under the
     * icon. The button does NOT consume the key itself (the screen already handles it), this only
     * advertises it.
     */
    public PonderButton withShortcut(KeyMapping shortcut) {
        this.shortcut = shortcut;
        return this;
    }

    /**
     * Pins the border to a fixed colour pair instead of the scheme-driven idle/hover gradient -
     * real Create's own {@code withBorderColors(...).animateColors(false)}, used for a tag entry
     * with nothing registered to open.
     */
    public PonderButton withBorderColors(int topArgb, int bottomArgb) {
        this.borderOverrideTop = topArgb;
        this.borderOverrideBottom = bottomArgb;
        this.borderOverridden = true;
        return this;
    }

    /**
     * Whether this button's feature is currently ON, used for the toggles (identify mode, slow
     * mode) so the button itself shows the state.
     */
    public void setFlashing(boolean flashing) {
        this.flashing = flashing;
    }

    /** A transient call-to-action pulse, separate from the steady selected/toggle state. */
    public void setAttention(boolean attention) {
        this.attention = attention;
    }

    public void tick() {
        if (entranceFadeEnabled) {
            entranceFadePrevious = entranceFadeValue;
            if (controlMotion) {
                controlMotionAge++;
                entranceFadeValue = controlEntrance(controlMotionAge, controlMotionDelay);
            } else if (Mth.equal(entranceFadeValue, 1F)) {
                entranceFadeValue = 1F;
            } else {
                entranceFadeValue += (1F - entranceFadeValue) * .1F;
            }
        }

        if (createLayout) {
            createColorPrevious = createColorValue;
            if (Mth.equal(createColorValue, 0F)) {
                createColorValue = 0F;
            } else {
                createColorValue += (0F - createColorValue) * .6F;
            }
        } else {
            fadePrevious = fade;
            float hoverTarget = visible && active && isHovered() ? 1F : 0F;
            if (Mth.equal(fade, hoverTarget)) {
                fade = hoverTarget;
            } else {
                fade += (hoverTarget - fade) * FADE_CHASE;
            }
        }

        flashPrevious = flashValue;
        float flashTarget = flashing ? 1F : 0F;
        if (Mth.equal(flashValue, flashTarget)) {
            flashValue = flashTarget;
        } else {
            flashValue += (flashTarget - flashValue) * .1F;
        }

        attentionPrevious = attentionValue;
        float attentionTarget = attention ? 1F : 0F;
        if (Mth.equal(attentionValue, attentionTarget)) {
            attentionValue = attentionTarget;
        } else {
            attentionValue += (attentionTarget - attentionValue) * .1F;
        }

        pressPrevious = pressValue;
        pressValue = decayControlPress(pressValue);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (controlMotion) {
            pressPrevious = pressValue = 1F;
        }
        callback.run();
    }

    /** Clears a call-to-action that belongs to the scene being left, without leaking its glow. */
    void resetAttention() {
        attention = false;
        attentionPrevious = attentionValue = 0F;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!active || !visible) {
            return false;
        }
        if (!createLayout) {
            return super.isMouseOver(mouseX, mouseY);
        }
        return mouseX >= getX() - CREATE_FRAME_INFLATION
            && mouseY >= getY() - CREATE_FRAME_INFLATION
            && mouseX < getX() + width + CREATE_FRAME_INFLATION
            && mouseY < getY() + height + CREATE_FRAME_INFLATION;
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        float interactionFade = screenFade * (entranceFadeEnabled ? entranceFadeValue : 1F);
        if (interactionFade < .1F) {
            return false;
        }
        return createLayout ? isMouseOver(mouseX, mouseY) : super.clicked(mouseX, mouseY);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.enableDepthTest();

        if (createLayout) {
            // Vanilla precomputes isHovered from the nominal 20x20 widget rectangle. Create's
            // ElementWidget deliberately replaces it with BoxWidget's padded 28x28 hit area.
            isHovered = isMouseOver(mouseX, mouseY);
        }
        boolean hovered = isHovered();
        PonderColorScheme scheme = colors.get();
        boolean createSchemeChanged = createLayout && createGradientInitialized
            && (createSchemeFrameTop != scheme.frameBorderTop()
            || createSchemeFrameBottom != scheme.frameBorderBottom()
            || createSchemeHoverTop != scheme.buttonHoverBorderTop()
            || createSchemeHoverBottom != scheme.buttonHoverBorderBottom());
        if (createLayout && !createGradientInitialized) {
            createGradientTop = createPreviousTop = createTargetTop = scheme.frameBorderTop();
            createGradientBottom = createPreviousBottom = createTargetBottom = scheme.frameBorderBottom();
            rememberCreateScheme(scheme);
            createGradientInitialized = true;
        } else if (createSchemeChanged) {
            // Buttons survive scene changes. Discard any interpolation that belongs to the old
            // scene so even a mid-hover page switch immediately uses the new scene's palette.
            int top = hovered ? scheme.buttonHoverBorderTop() : scheme.frameBorderTop();
            int bottom = hovered ? scheme.buttonHoverBorderBottom() : scheme.frameBorderBottom();
            createGradientTop = createPreviousTop = createTargetTop = top;
            createGradientBottom = createPreviousBottom = createTargetBottom = bottom;
            createColorPrevious = createColorValue = 0F;
            rememberCreateScheme(scheme);
        }
        if (createLayout && !createSchemeChanged && hovered != wasHovered && !borderOverridden) {
            // Literal BoxWidget transition: restart from the channel-quantized colour actually
            // drawn last frame, set residual to 1, then immediately apply the first .6 EXP step.
            createPreviousTop = createGradientTop;
            createPreviousBottom = createGradientBottom;
            createTargetTop = hovered ? scheme.buttonHoverBorderTop() : scheme.frameBorderTop();
            createTargetBottom = hovered ? scheme.buttonHoverBorderBottom() : scheme.frameBorderBottom();
            createColorPrevious = 1F;
            createColorValue = .4F;
        }
        wasHovered = hovered;

        float interpolatedHover = createLayout ? (hovered ? 1F : 0F) : Mth.lerp(partialTick, fadePrevious, fade);
        float hoverMotion = easeOutCubic(interpolatedHover);
        float interpolatedAttention = Mth.lerp(partialTick, attentionPrevious, attentionValue);
        float attentionWave = .55F + .45F
            * (.5F + .5F * Mth.sin((Minecraft.getInstance().gui.getGuiTicks() + partialTick) / 7F));
        float attentionGlow = interpolatedAttention * attentionWave;
        int borderTop;
        int borderBottom;
        if (borderOverridden) {
            borderTop = borderOverrideTop;
            borderBottom = borderOverrideBottom;
        } else if (createLayout) {
            if (Mth.equal(createColorPrevious, createColorValue) && Mth.equal(createColorValue, 0F)) {
                borderTop = createTargetTop;
                borderBottom = createTargetBottom;
            } else {
                float mix = 1F - Math.abs(Mth.lerp(partialTick, createColorPrevious, createColorValue));
                borderTop = lerpColor(createPreviousTop, createTargetTop, mix);
                borderBottom = lerpColor(createPreviousBottom, createTargetBottom, mix);
            }
        } else {
            borderTop = lerpColor(scheme.frameBorderTop(), scheme.buttonHoverBorderTop(), interpolatedHover);
            borderBottom = lerpColor(scheme.frameBorderBottom(), scheme.buttonHoverBorderBottom(), interpolatedHover);
        }
        float interpolatedFlash = Mth.lerp(partialTick, flashPrevious, flashValue);
        if (!borderOverridden && interpolatedFlash > .1F) {
            float pulse = (.5F + .5F * Mth.sin((Minecraft.getInstance().gui.getGuiTicks() + partialTick) / 10F))
                * interpolatedFlash;
            borderTop = lerpColor(borderTop, scheme.buttonHoverBorderTop(), pulse);
            borderBottom = lerpColor(borderBottom, scheme.buttonHoverBorderBottom(), pulse);
        }
        if (!borderOverridden && attentionGlow > .01F) {
            borderTop = lerpColor(borderTop, scheme.buttonHoverBorderTop(), attentionGlow * .4F);
            borderBottom = lerpColor(borderBottom, scheme.buttonHoverBorderBottom(), attentionGlow * .4F);
        }
        if (createLayout && !borderOverridden) {
            createGradientTop = borderTop;
            createGradientBottom = borderBottom;
        }
        float entranceFade = entranceFadeEnabled
            ? Mth.lerp(partialTick, entranceFadePrevious, entranceFadeValue)
            : 1F;
        float renderedFade = screenFade * entranceFade;
        if (renderedFade < .1F) {
            return;
        }

        graphics.pose().pushPose();
        if (entranceFade < 1F) {
            graphics.pose().translate((1F - entranceFade) * entranceFadeModX,
                (1F - entranceFade) * entranceFadeModY, 0);
        }
        if (controlMotion) {
            float haloStrength = Math.max(hoverMotion * .18F, attentionGlow * .22F) * renderedFade;
            if (haloStrength > .01F) {
                int halo = scaleAlpha(lerpColor(scheme.buttonHoverBorderTop(),
                    scheme.buttonHoverBorderBottom(), .7F), haloStrength);
                graphics.fill(getX() + 2, getY() + height + 1, getX() + width - 2,
                    getY() + height + 2, REGULAR_FRAME_Z - 10, halo);
                graphics.fill(getX() + 5, getY() + height + 2, getX() + width - 5,
                    getY() + height + 3, REGULAR_FRAME_Z - 10, scaleAlpha(halo, .5F));
            }

            float press = Mth.lerp(partialTick, pressPrevious, pressValue);
            float scale = Mth.lerp(entranceFade, .9F, 1F)
                + CONTROL_HOVER_SCALE * hoverMotion - CONTROL_PRESS_SCALE * press;
            float lift = -CONTROL_HOVER_LIFT * hoverMotion + press;
            float centerX = getX() + width / 2F;
            float centerY = getY() + height / 2F;
            graphics.pose().translate(centerX, centerY + lift, 0);
            graphics.pose().scale(scale, scale, 1F);
            graphics.pose().translate(-centerX, -centerY, 0);
        }
        new PonderBoxElement()
            .withBackground(scheme.buttonBackground())
            .gradientBorder(borderTop, borderBottom)
            .at(createLayout ? getX() : getX() + 3, createLayout ? getY() : getY() + 3,
                createLayout ? CREATE_FRAME_Z : REGULAR_FRAME_Z)
            .withBounds(createLayout ? SIZE : SIZE - 6, createLayout ? SIZE : SIZE - 6)
            .withAlpha(renderedFade)
            .render(graphics);

        float shortcutReveal = shortcut == null ? 0F : smoothStep(interpolatedHover);
        if (icon != null) {
            float iconLight = createLayout
                ? (hovered || flashing ? 1F : 0F)
                : Math.max(interpolatedHover, Math.max(interpolatedFlash, attentionGlow));
            int iconColor = scaleAlpha(lerpColor(scheme.buttonIconDim(), scheme.buttonIconLit(), iconLight),
                renderedFade * (1F - shortcutReveal));
            graphics.pose().pushPose();
            if (controlMotion) {
                graphics.pose().translate(controlIconDirection * .75F * hoverMotion, -.5F * hoverMotion, 0);
            }
            drawIcon(graphics, iconColor);
            graphics.pose().popPose();
        } else if (itemIcon != null && !itemIcon.isEmpty()) {
            // A real item's own texture already carries its colour - unlike the pixel masks above,
            // this is never tinted by buttonIconLit/Dim, only the frame around it brightens on hover.
            if (tagIcon) {
                drawTagIcon(graphics, renderedFade);
            } else {
                drawItemIcon(graphics, renderedFade);
            }
        }

        if (shortcut != null && shortcutReveal > 1 / 512F) {
            // Replace the icon in-place instead of hanging a full font line below the 20px button.
            // Long/rebound key names scale to the 10px inner width; common arrows use one glyph.
            Font font = Minecraft.getInstance().font;
            String key = shortcutLabel();
            int keyWidth = Math.max(1, font.width(key));
            float keyScale = shortcutScale(keyWidth);
            int alpha = Math.max(0x05,
                Math.round(0xFF * Mth.clamp(shortcutReveal * renderedFade, 0F, 1F)));
            graphics.pose().pushPose();
            graphics.pose().translate(getX() + width / 2F,
                getY() + height / 2F + (1F - shortcutReveal) * 2F,
                (createLayout ? CREATE_CONTENT_Z : REGULAR_CONTENT_Z) + 1);
            graphics.pose().scale(keyScale, keyScale, 1F);
            graphics.drawString(font, key, -keyWidth / 2, -font.lineHeight / 2,
                (alpha << 24) | 0xBBBBBB, false);
            graphics.pose().popPose();
        }
        graphics.pose().popPose();
    }

    private void drawIcon(GuiGraphics graphics, int color) {
        int originX = getX() + (width - Icon.PIXELS) / 2;
        int originY = getY() + (height - Icon.PIXELS) / 2;
        for (int row = 0; row < icon.mask.length; row++) {
            String line = icon.mask[row];
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) == '#') {
                    graphics.fill(originX + col, originY + row, originX + col + 1, originY + row + 1,
                        createLayout ? CREATE_CONTENT_Z : REGULAR_CONTENT_Z, color);
                }
            }
        }
    }

    private void drawItemIcon(GuiGraphics graphics, float alpha) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        // Exact ElementWidget transform: widget padding (2,2), element at (-4,-4), then 1.5x.
        poseStack.translate(getX() + 2, getY() + 2, CREATE_CONTENT_Z);
        poseStack.translate(-4, -4, 0);
        poseStack.scale(ITEM_ICON_SCALE, ITEM_ICON_SCALE, 1F);
        renderCreateItem(graphics, itemIcon, alpha);
        poseStack.popPose();
    }

    private void drawTagIcon(GuiGraphics graphics, float alpha) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(getX() + 2, getY() + 2, CREATE_CONTENT_Z);
        renderTagIcon(graphics, itemIcon, alpha);
        poseStack.popPose();
    }

    /** Exact item-backed {@code PonderTag#render} transform at the current pose origin. */
    static void renderTagIcon(GuiGraphics graphics, ItemStack stack) {
        renderTagIcon(graphics, stack, 1F);
    }

    private static void renderTagIcon(GuiGraphics graphics, ItemStack stack, float alpha) {
        if (stack.isEmpty()) {
            return;
        }
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(-2, -2, 0);
        poseStack.scale(TAG_ICON_SCALE, TAG_ICON_SCALE, TAG_ICON_SCALE);
        renderCreateItem(graphics, stack, alpha);
        poseStack.popPose();
    }

    /** GuiGameElement's GUI item path: null model context, z=100 and full-bright buffers. */
    private static void renderCreateItem(GuiGraphics graphics, ItemStack stack, float alpha) {
        Minecraft minecraft = Minecraft.getInstance();
        ItemRenderer renderer = minecraft.getItemRenderer();
        BakedModel model = renderer.getModel(stack, null, null, 0);
        PoseStack poseStack = graphics.pose();

        poseStack.pushPose();
        RenderSystem.setShaderColor(1F, 1F, 1F, Mth.clamp(alpha, 0F, 1F));
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
        Lighting.setupFor3DItems();
        poseStack.scale(1F, -1F, 1F);

        minecraft.getTextureManager().getTexture(InventoryMenu.BLOCK_ATLAS).setFilter(false, false);
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        RenderSystem.enableBlend();
        RenderSystem.enableCull();
        RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1F, 1F, 1F, Mth.clamp(alpha, 0F, 1F));

        poseStack.pushPose();
        poseStack.translate(8F, -8F, 100F);
        poseStack.scale(16F, 16F, 16F);
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        boolean flatLighting = !model.usesBlockLight();
        if (flatLighting) {
            Lighting.setupForFlatItems();
        }
        renderer.render(stack, ItemDisplayContext.GUI, false, poseStack, buffer,
            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
        RenderSystem.disableDepthTest();
        buffer.endBatch();
        RenderSystem.enableDepthTest();
        if (flatLighting) {
            Lighting.setupFor3DItems();
        }
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        poseStack.popPose();
        poseStack.popPose();
    }

    @Nullable
    ItemStack getItem() {
        return itemIcon;
    }

    private void rememberCreateScheme(PonderColorScheme scheme) {
        createSchemeFrameTop = scheme.frameBorderTop();
        createSchemeFrameBottom = scheme.frameBorderBottom();
        createSchemeHoverTop = scheme.buttonHoverBorderTop();
        createSchemeHoverBottom = scheme.buttonHoverBorderBottom();
    }

    private String shortcutLabel() {
        InputConstants.Key key = shortcut.getKey();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return "M" + (key.getValue() + 1);
        }
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return switch (key.getValue()) {
                case InputConstants.KEY_LEFT -> "←";
                case InputConstants.KEY_RIGHT -> "→";
                case InputConstants.KEY_UP -> "↑";
                case InputConstants.KEY_DOWN -> "↓";
                default -> shortcut.getTranslatedKeyMessage().getString();
            };
        }
        return shortcut.getTranslatedKeyMessage().getString();
    }

    static float controlEntrance(int ageTicks, int delayTicks) {
        float progress = Mth.clamp((ageTicks - Math.max(0, delayTicks)) / (float) CONTROL_ENTRANCE_TICKS, 0F, 1F);
        return easeOutCubic(progress);
    }

    static float entranceFadeAfterTicks(int elapsedTicks) {
        int ticks = Math.max(0, elapsedTicks);
        return 1F - (float) Math.pow(.9D, ticks);
    }

    static float shortcutScale(int keyWidth) {
        return Math.min(1F, SHORTCUT_INNER_WIDTH / (float) Math.max(1, keyWidth));
    }

    static float decayControlPress(float value) {
        float next = value + (0F - value) * PRESS_DECAY;
        return next < 1 / 512F ? 0F : next;
    }

    private static float easeOutCubic(float value) {
        float inverse = 1F - Mth.clamp(value, 0F, 1F);
        return 1F - inverse * inverse * inverse;
    }

    private static float smoothStep(float value) {
        float clamped = Mth.clamp(value, 0F, 1F);
        return clamped * clamped * (3F - 2F * clamped);
    }

    private static int scaleAlpha(int argb, float factor) {
        int alpha = (int) (((argb >>> 24) & 0xFF) * Mth.clamp(factor, 0F, 1F));
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    private static int lerpColor(int from, int to, float t) {
        int a = lerpChannel(from >>> 24, to >>> 24, t);
        int r = lerpChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int g = lerpChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = lerpChannel(from & 0xFF, to & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerpChannel(int from, int to, float t) {
        return Mth.clamp((int) (from + (to - from) * t), 0, 0xFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
