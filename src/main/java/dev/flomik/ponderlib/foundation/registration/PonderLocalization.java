package dev.flomik.ponderlib.foundation.registration;

/**
 * Lang-key naming scheme for per-scene text: {@code <modid>.ponder.<sceneId>.<key>}, "header" for
 * the title and "text_N" for the Nth {@code showText} call. {@code PonderSceneBuilder} uses this to
 * build a translatable {@code Component} at play time; a mod's own datagen lang provider can call
 * the same methods to generate matching keys for its scenes' English defaults.
 */
public final class PonderLocalization {

    private static final String LANG_PREFIX = "ponder";

    private PonderLocalization() {
    }

    public static String keyForTitle(String modId, String sceneId) {
        return modId + "." + LANG_PREFIX + "." + sceneId + ".header";
    }

    public static String keyForText(String modId, String sceneId, int index) {
        return modId + "." + LANG_PREFIX + "." + sceneId + ".text_" + index;
    }
}
