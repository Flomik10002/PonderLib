package dev.flomik.ponderlib.testsupport;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * Bootstraps vanilla's static registries ({@code Blocks}, {@code Items}, ...) once, before any
 * test class loads. ForgeGradle's {@code test} task runs bare JUnit - unlike the NeoForge/moddev
 * toolchain, there's no modded launch bootstrapping this automatically, so referencing a vanilla
 * registry field from a test would otherwise fail with {@code ExceptionInInitializerError}.
 * Registered via {@code META-INF/services/org.junit.platform.launcher.LauncherSessionListener} so
 * the JUnit Platform picks it up for every test run without any test needing to know it exists.
 */
public class VanillaBootstrapListener implements LauncherSessionListener {

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        SharedConstants.tryDetectVersion();
        try {
            Bootstrap.bootStrap();
        } catch (Throwable t) {
            // Bootstrap.bootStrap() is patched by Forge to also initialize its own networking
            // (NetworkHooks.init()), which needs a running FML environment this bare JUnit process
            // never has - that specific failure happens after vanilla's own registries are already
            // populated, which is all a unit test actually needs, so it's safe to ignore here.
        }
    }
}
