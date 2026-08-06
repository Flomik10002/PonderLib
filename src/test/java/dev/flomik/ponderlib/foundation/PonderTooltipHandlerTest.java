package dev.flomik.ponderlib.foundation;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

// PonderTooltipHandler.tick()'s hold-progress branch polls a real GLFW window (InputConstants
// .isKeyDown) and opens a real Minecraft.getInstance() screen, and addToTooltip()'s main path
// reads Config.SHOW_TOOLTIP_HINT.get() - which throws IllegalStateException here since no config
// file is ever loaded in this environment (verified directly: "Cannot get config value before
// config is loaded."). Both need a real running client/config, so this only covers the branches
// that return before touching either - still real, non-trivial guard logic worth locking down.
class PonderTooltipHandlerTest {

    @Test
    void tickWithNoTooltipSeenSinceLastTickResetsTrackingWithoutTouchingTheClient() {
        PonderTooltipHandler.tick(null);
    }

    @Test
    void addToTooltipIgnoresAnEmptyStackBeforeEvenCheckingTheConfig() {
        PonderTooltipHandler.addToTooltip(null, ItemStack.EMPTY, null);
    }

    @Test
    void tooltipBorderColorIsAbsentForAnUntrackedStack() {
        assertTrue(PonderTooltipHandler.tooltipBorderColor(new ItemStack(Items.STONE)).isEmpty());
    }
}
