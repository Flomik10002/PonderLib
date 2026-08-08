package dev.flomik.ponderlib.api.registration;

import dev.flomik.ponderlib.api.PonderColorScheme;

/**
 * The entire extension surface a third-party mod implements to add its own Ponder scenes.
 * Registered once via {@code PonderIndex.addPlugin(new MyPlugin())} from that mod's client init —
 * no classpath scanning, no ServiceLoader.
 */
public interface PonderPlugin {

    /**
     * @return this mod's own mod id — used to namespace lang keys and to resolve {@code
     *         schematicPath}s passed to {@link #registerScenes}
     */
    String getModId();

    /**
     * Called once, after every mod's plugin has been registered (so ordering hints against another
     * mod's scenes can always be resolved regardless of registration order) — register every scene
     * this mod owns here, via calls to {@code helper.addStoryBoard(...)}.
     */
    default void registerScenes(PonderSceneRegistrationHelper helper) {
    }

    /**
     * Registers semantic groups of related components. Unlike scene tags, these groups are
     * navigation: every component keeps its own storyboards while the Ponder UI exposes a tab
     * leading to the other members of the group.
     */
    default void registerTags(PonderTagRegistrationHelper helper) {
    }

    /**
     * @return the colours (base plate shadow/flash, tooltip border, timeline/button chrome) shown
     *         while playing one of THIS plugin's own scenes. Defaults to {@link
     *         PonderColorScheme#DEFAULT} - override only if your mod wants its own palette. This is
     *         per-plugin, never global: it has no effect on any other mod's scenes.
     */
    default PonderColorScheme colors() {
        return PonderColorScheme.DEFAULT;
    }
}
