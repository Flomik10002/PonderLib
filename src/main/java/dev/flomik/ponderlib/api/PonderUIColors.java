package dev.flomik.ponderlib.api;

/**
 * The scene screen's own UI chrome — the timeline scrubber, the button row, and the "next up"
 * teaser box — as opposed to {@link PonderSceneColors}, which covers colours drawn as part of the
 * scene itself (the base plate's shadow/flash). Call the setters below to match your own mod's
 * palette instead of the defaults.
 */
public final class PonderUIColors {

    // Shared by the timeline's own frame, the "next up" teaser box, and every button's idle-state
    // border - the same quiet, slightly-warm-white chrome tying every non-scene UI element together
    // is deliberate, not a coincidence worth losing by giving each element its own colour.
    private static int frameBorderTop = 0x40FFEEDD;
    private static int frameBorderBottom = 0x20FFEEDD;

    private static int timelineFillTop = 0x80AAAADD;
    private static int timelineFillBottom = 0x50AAAADD;

    // Idle/hover differ only in alpha, not hue - a mark reads as "the same UI material, dimmer or
    // brighter" rather than two unrelated colours.
    private static int keyframeTint = 0xFFFFFF;
    private static int keyframeAlphaIdle = 0x70;
    private static int keyframeAlphaHover = 0xE0;

    private static int buttonHoverBorderTop = 0x70FFFFFF;
    private static int buttonHoverBorderBottom = 0x30FFFFFF;
    private static int buttonBackground = 0xFF000000;
    private static int buttonIconDim = 0xFFAAAAAA;
    private static int buttonIconLit = 0xFFFFFFFF;

    private PonderUIColors() {
    }

    /**
     * @param topArgb    0xAARRGGBB drawn along the top edge and the upper half of the sides
     * @param bottomArgb 0xAARRGGBB drawn along the bottom edge and the lower half of the sides
     */
    public static void setFrameBorder(int topArgb, int bottomArgb) {
        frameBorderTop = topArgb;
        frameBorderBottom = bottomArgb;
    }

    public static int frameBorderTop() {
        return frameBorderTop;
    }

    public static int frameBorderBottom() {
        return frameBorderBottom;
    }

    /**
     * @param topArgb    0xAARRGGBB for the fill's top band
     * @param bottomArgb 0xAARRGGBB for the fill's bottom band
     */
    public static void setTimelineFill(int topArgb, int bottomArgb) {
        timelineFillTop = topArgb;
        timelineFillBottom = bottomArgb;
    }

    public static int timelineFillTop() {
        return timelineFillTop;
    }

    public static int timelineFillBottom() {
        return timelineFillBottom;
    }

    /**
     * @param rgb 0xRRGGBB — alpha comes from {@link #setKeyframeAlpha} instead, not from here.
     */
    public static void setKeyframeTint(int rgb) {
        keyframeTint = rgb;
    }

    public static int keyframeTint() {
        return keyframeTint;
    }

    /**
     * @param idle  0x00..0xFF alpha for a tick mark the cursor isn't near
     * @param hover 0x00..0xFF alpha for the mark the cursor would currently snap to
     */
    public static void setKeyframeAlpha(int idle, int hover) {
        keyframeAlphaIdle = idle;
        keyframeAlphaHover = hover;
    }

    public static int keyframeAlphaIdle() {
        return keyframeAlphaIdle;
    }

    public static int keyframeAlphaHover() {
        return keyframeAlphaHover;
    }

    /**
     * @param topArgb    0xAARRGGBB drawn along the top edge and the upper half of the sides
     * @param bottomArgb 0xAARRGGBB drawn along the bottom edge and the lower half of the sides
     */
    public static void setButtonHoverBorder(int topArgb, int bottomArgb) {
        buttonHoverBorderTop = topArgb;
        buttonHoverBorderBottom = bottomArgb;
    }

    public static int buttonHoverBorderTop() {
        return buttonHoverBorderTop;
    }

    public static int buttonHoverBorderBottom() {
        return buttonHoverBorderBottom;
    }

    /**
     * @param argb 0xAARRGGBB
     */
    public static void setButtonBackground(int argb) {
        buttonBackground = argb;
    }

    public static int buttonBackground() {
        return buttonBackground;
    }

    /**
     * @param dimArgb 0xAARRGGBB for an idle button's icon
     * @param litArgb 0xAARRGGBB for a hovered or flashing (toggled-on) button's icon
     */
    public static void setButtonIcon(int dimArgb, int litArgb) {
        buttonIconDim = dimArgb;
        buttonIconLit = litArgb;
    }

    public static int buttonIconDim() {
        return buttonIconDim;
    }

    public static int buttonIconLit() {
        return buttonIconLit;
    }
}
