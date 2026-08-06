package dev.flomik.ponderlib.api;

/**
 * The border-color gradient shown on an item's tooltip while holding the Ponder key. Call
 * {@link #set} to use your own palette instead of PonderLib's default.
 */
public final class PonderTooltipColors {

    private static int start = 0x2BC4FF;
    private static int mid = 0x4F7CFF;
    private static int end = 0xFFFFFF;

    private PonderTooltipColors() {
    }

    /**
     * @param start 0xRRGGBB at hold-progress 0
     * @param mid   0xRRGGBB at hold-progress 0.5
     * @param end   0xRRGGBB at hold-progress 1
     */
    public static void set(int start, int mid, int end) {
        PonderTooltipColors.start = start;
        PonderTooltipColors.mid = mid;
        PonderTooltipColors.end = end;
    }

    /**
     * @param progress 0..1
     * @return 0xRRGGBB
     */
    public static int forProgress(float progress) {
        return progress < 0.5f
            ? mix(start, mid, progress * 2f)
            : mix(mid, end, (progress - 0.5f) * 2f);
    }

    private static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
