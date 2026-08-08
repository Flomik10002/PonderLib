package dev.flomik.ponderlib.foundation;

import dev.flomik.ponderlib.api.PonderColorScheme;
import dev.flomik.ponderlib.api.registration.PonderPlugin;
import dev.flomik.ponderlib.api.registration.PonderTag;
import dev.flomik.ponderlib.foundation.registration.DefaultPonderSceneRegistrationHelper;
import dev.flomik.ponderlib.foundation.registration.DefaultPonderTagRegistrationHelper;
import dev.flomik.ponderlib.foundation.registration.PonderSceneRegistry;
import dev.flomik.ponderlib.foundation.registration.PonderTagRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Static, explicitly-called registry — no classpath scanning. A consumer mod calls
 * {@code PonderIndex.addPlugin(new MyPlugin())} from its own client init; {@link #registerAll()}
 * runs once, after all mods have had a chance to register (real trigger: FMLLoadCompleteEvent).
 */
public final class PonderIndex {

    private static final List<PonderPlugin> PLUGINS = new ArrayList<>();
    // Keyed the same way a scene's own schematic location is namespaced (see
    // DefaultPonderSceneRegistrationHelper) - lets #colorsFor go straight from "which mod owns this
    // scene" to "that mod's own PonderPlugin#colors()" without a scene needing to keep a reference
    // to the plugin instance itself.
    private static final Map<String, PonderPlugin> PLUGINS_BY_MOD_ID = new HashMap<>();
    private static final PonderSceneRegistry SCENES = new PonderSceneRegistry();
    private static final PonderTagRegistry TAGS = new PonderTagRegistry();
    private static boolean registered;

    private PonderIndex() {
    }

    public static void addPlugin(PonderPlugin plugin) {
        PLUGINS.add(plugin);
        PLUGINS_BY_MOD_ID.put(plugin.getModId(), plugin);
    }

    public static void registerAll() {
        if (registered) {
            return;
        }
        registered = true;
        for (PonderPlugin plugin : PLUGINS) plugin.registerTags(new DefaultPonderTagRegistrationHelper(plugin.getModId(), TAGS));
        for (PonderPlugin plugin : PLUGINS) {
            plugin.registerScenes(new DefaultPonderSceneRegistrationHelper(plugin.getModId(), SCENES));
        }
        SCENES.getAllEntries().forEach(entry -> entry.getTags().forEach(tag -> {
            // Runtime-only sidebar sentinel: it highlights every scene tag, but must never become
            // a visible/navigation tag of its own.
            if (PonderTag.HIGHLIGHT_ALL.equals(tag)) {
                return;
            }
            TAGS.registerLegacyIfAbsent(tag);
            TAGS.addComponent(tag, entry.getComponent());
        }));
    }

    public static PonderSceneRegistry getScenes() {
        return SCENES;
    }
    public static PonderTagRegistry getTags() { return TAGS; }

    /**
     * The colour scheme registered for {@code modId} (via that mod's own {@code
     * PonderPlugin#colors()}), or {@link PonderColorScheme#DEFAULT} if {@code modId} is {@code
     * null} or belongs to no registered plugin - a scene compiled with no known owner (tests, the
     * direct-{@code PonderStoryBoard} path) always gets PonderLib's own base colours, never another
     * mod's.
     */
    public static PonderColorScheme colorsFor(String modId) {
        PonderPlugin plugin = modId == null ? null : PLUGINS_BY_MOD_ID.get(modId);
        return plugin == null ? PonderColorScheme.DEFAULT : plugin.colors();
    }
}
