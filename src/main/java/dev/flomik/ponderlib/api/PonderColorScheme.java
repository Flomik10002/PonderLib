package dev.flomik.ponderlib.api;

/**
 * Every colour PonderLib itself draws outside of a storyboard's own per-call choices (see {@code
 * api.PonderPalette} for those) — the base plate's shadow/flash, the tooltip hold-progress border,
 * and the scene screen's own chrome (timeline scrubber, buttons). Immutable and built via
 * {@link #builder()}; unset fields keep {@link #DEFAULT}'s values, so overriding one colour never
 * requires specifying the rest.
 * <p>
 * This is deliberately per-{@code PonderPlugin}, not global: override {@link
 * dev.flomik.ponderlib.api.registration.PonderPlugin#colors()} to return your own scheme, and only
 * scenes registered by your plugin pick it up. There is no way to change another mod's colours, or
 * PonderLib's own defaults, from outside your plugin - the whole point is that installing one mod
 * can never repaint every other mod's Ponder scenes.
 */
public final class PonderColorScheme {

    public static final PonderColorScheme DEFAULT = builder().build();

    private final int finishingFlash;
    private final int basePlateShadow;
    private final int tooltipBorderStart;
    private final int tooltipBorderMid;
    private final int tooltipBorderEnd;
    private final int frameBorderTop;
    private final int frameBorderBottom;
    private final int timelineFillTop;
    private final int timelineFillBottom;
    private final int keyframeTint;
    private final int keyframeAlphaIdle;
    private final int keyframeAlphaHover;
    private final int buttonHoverBorderTop;
    private final int buttonHoverBorderBottom;
    private final int buttonBackground;
    private final int buttonIconDim;
    private final int buttonIconLit;

    private PonderColorScheme(Builder builder) {
        this.finishingFlash = builder.finishingFlash;
        this.basePlateShadow = builder.basePlateShadow;
        this.tooltipBorderStart = builder.tooltipBorderStart;
        this.tooltipBorderMid = builder.tooltipBorderMid;
        this.tooltipBorderEnd = builder.tooltipBorderEnd;
        this.frameBorderTop = builder.frameBorderTop;
        this.frameBorderBottom = builder.frameBorderBottom;
        this.timelineFillTop = builder.timelineFillTop;
        this.timelineFillBottom = builder.timelineFillBottom;
        this.keyframeTint = builder.keyframeTint;
        this.keyframeAlphaIdle = builder.keyframeAlphaIdle;
        this.keyframeAlphaHover = builder.keyframeAlphaHover;
        this.buttonHoverBorderTop = builder.buttonHoverBorderTop;
        this.buttonHoverBorderBottom = builder.buttonHoverBorderBottom;
        this.buttonBackground = builder.buttonBackground;
        this.buttonIconDim = builder.buttonIconDim;
        this.buttonIconLit = builder.buttonIconLit;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return 0xRRGGBB — alpha is not taken from here; the glow supplies its own, fading out along
     *         its height and over the flash animation.
     */
    public int finishingFlash() {
        return finishingFlash;
    }

    /**
     * @return 0xRRGGBB — as with the flash, alpha comes from the shadow's own gradient, not from
     *         this value.
     */
    public int basePlateShadow() {
        return basePlateShadow;
    }

    /**
     * The border-colour gradient shown on an item's tooltip while holding the Ponder key, at
     * hold-progress 0 → 0.5 → 1.
     *
     * @param progress 0..1
     * @return 0xRRGGBB
     */
    public int tooltipBorderForProgress(float progress) {
        return progress < 0.5F
            ? mix(tooltipBorderStart, tooltipBorderMid, progress * 2F)
            : mix(tooltipBorderMid, tooltipBorderEnd, (progress - 0.5F) * 2F);
    }

    private static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /**
     * @return 0xAARRGGBB — shared by the timeline's own frame, the "next up" teaser box, and every
     *         button's idle-state border.
     */
    public int frameBorderTop() {
        return frameBorderTop;
    }

    public int frameBorderBottom() {
        return frameBorderBottom;
    }

    /**
     * @return 0xAARRGGBB for the timeline fill's top/bottom band
     */
    public int timelineFillTop() {
        return timelineFillTop;
    }

    public int timelineFillBottom() {
        return timelineFillBottom;
    }

    /**
     * @return 0xRRGGBB — alpha comes from {@link #keyframeAlphaIdle()}/{@link #keyframeAlphaHover()}
     *         instead, not from here.
     */
    public int keyframeTint() {
        return keyframeTint;
    }

    /**
     * @return 0x00..0xFF alpha for a timeline tick mark the cursor isn't near
     */
    public int keyframeAlphaIdle() {
        return keyframeAlphaIdle;
    }

    /**
     * @return 0x00..0xFF alpha for the tick mark the cursor would currently snap to
     */
    public int keyframeAlphaHover() {
        return keyframeAlphaHover;
    }

    /**
     * @return 0xAARRGGBB for a button's border while hovered or toggled on
     */
    public int buttonHoverBorderTop() {
        return buttonHoverBorderTop;
    }

    public int buttonHoverBorderBottom() {
        return buttonHoverBorderBottom;
    }

    /**
     * @return 0xAARRGGBB, every button's solid background
     */
    public int buttonBackground() {
        return buttonBackground;
    }

    /**
     * @return 0xAARRGGBB for an idle button's icon
     */
    public int buttonIconDim() {
        return buttonIconDim;
    }

    /**
     * @return 0xAARRGGBB for a hovered or flashing (toggled-on) button's icon
     */
    public int buttonIconLit() {
        return buttonIconLit;
    }

    public static final class Builder {

        private int finishingFlash = 0xC6E9FF;
        private int basePlateShadow = 0x000000;
        private int tooltipBorderStart = 0x2BC4FF;
        private int tooltipBorderMid = 0x4F7CFF;
        private int tooltipBorderEnd = 0xFFFFFF;
        // Shared quiet, slightly-warm-white chrome pair - see PonderColorScheme's own javadoc.
        private int frameBorderTop = 0x40FFEEDD;
        private int frameBorderBottom = 0x20FFEEDD;
        private int timelineFillTop = 0x80AAAADD;
        private int timelineFillBottom = 0x50AAAADD;
        private int keyframeTint = 0xFFFFFF;
        private int keyframeAlphaIdle = 0x70;
        private int keyframeAlphaHover = 0xE0;
        private int buttonHoverBorderTop = 0x70FFFFFF;
        private int buttonHoverBorderBottom = 0x30FFFFFF;
        private int buttonBackground = 0xFF000000;
        private int buttonIconDim = 0xFFAAAAAA;
        private int buttonIconLit = 0xFFFFFFFF;

        private Builder() {
        }

        public Builder finishingFlash(int rgb) {
            this.finishingFlash = rgb;
            return this;
        }

        public Builder basePlateShadow(int rgb) {
            this.basePlateShadow = rgb;
            return this;
        }

        public Builder tooltipBorder(int startRgb, int midRgb, int endRgb) {
            this.tooltipBorderStart = startRgb;
            this.tooltipBorderMid = midRgb;
            this.tooltipBorderEnd = endRgb;
            return this;
        }

        public Builder frameBorder(int topArgb, int bottomArgb) {
            this.frameBorderTop = topArgb;
            this.frameBorderBottom = bottomArgb;
            return this;
        }

        public Builder timelineFill(int topArgb, int bottomArgb) {
            this.timelineFillTop = topArgb;
            this.timelineFillBottom = bottomArgb;
            return this;
        }

        public Builder keyframeTint(int rgb) {
            this.keyframeTint = rgb;
            return this;
        }

        public Builder keyframeAlpha(int idle, int hover) {
            this.keyframeAlphaIdle = idle;
            this.keyframeAlphaHover = hover;
            return this;
        }

        public Builder buttonHoverBorder(int topArgb, int bottomArgb) {
            this.buttonHoverBorderTop = topArgb;
            this.buttonHoverBorderBottom = bottomArgb;
            return this;
        }

        public Builder buttonBackground(int argb) {
            this.buttonBackground = argb;
            return this;
        }

        public Builder buttonIcon(int dimArgb, int litArgb) {
            this.buttonIconDim = dimArgb;
            this.buttonIconLit = litArgb;
            return this;
        }

        public PonderColorScheme build() {
            return new PonderColorScheme(this);
        }
    }
}
