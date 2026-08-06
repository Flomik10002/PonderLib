package dev.flomik.ponderlib.foundation;

import dev.flomik.ponderlib.api.registration.PonderPlugin;
import dev.flomik.ponderlib.foundation.registration.DefaultPonderSceneRegistrationHelper;
import dev.flomik.ponderlib.foundation.registration.PonderSceneRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Static, explicitly-called registry — no classpath scanning. A consumer mod calls
 * {@code PonderIndex.addPlugin(new MyPlugin())} from its own client init; {@link #registerAll()}
 * runs once, after all mods have had a chance to register (real trigger: FMLLoadCompleteEvent).
 */
public final class PonderIndex {

    private static final List<PonderPlugin> PLUGINS = new ArrayList<>();
    private static final PonderSceneRegistry SCENES = new PonderSceneRegistry();
    private static boolean registered;

    private PonderIndex() {
    }

    public static void addPlugin(PonderPlugin plugin) {
        PLUGINS.add(plugin);
    }

    public static void registerAll() {
        if (registered) {
            return;
        }
        registered = true;
        for (PonderPlugin plugin : PLUGINS) {
            plugin.registerScenes(new DefaultPonderSceneRegistrationHelper(plugin.getModId(), SCENES));
        }
    }

    public static PonderSceneRegistry getScenes() {
        return SCENES;
    }
}
