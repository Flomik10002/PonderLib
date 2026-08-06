package dev.flomik.ponderlib.api;

/**
 * The two decorative colours drawn around a scene's base plate — the glow that flashes up its edges
 * when a scene finishes, and the shadow skirt hanging below it. Call {@link #setFinishingFlash}/
 * {@link #setBasePlateShadow} to match your own mod's palette instead of the defaults (a pale sky
 * blue flash, a black shadow).
 */
public final class PonderSceneColors {

    private static int finishingFlash = 0xC6E9FF;
    private static int basePlateShadow = 0x000000;

    private PonderSceneColors() {
    }

    /**
     * @param rgb 0xRRGGBB — alpha is not taken from here; the glow supplies its own, fading out along
     *            its height and over the flash animation.
     */
    public static void setFinishingFlash(int rgb) {
        finishingFlash = rgb;
    }

    public static int finishingFlash() {
        return finishingFlash;
    }

    /**
     * @param rgb 0xRRGGBB — as with the flash, alpha comes from the shadow's own gradient, not from
     *            this value.
     */
    public static void setBasePlateShadow(int rgb) {
        basePlateShadow = rgb;
    }

    public static int basePlateShadow() {
        return basePlateShadow;
    }
}
