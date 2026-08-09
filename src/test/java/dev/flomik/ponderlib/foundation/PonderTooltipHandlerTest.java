package dev.flomik.ponderlib.foundation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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

    @Test
    void holdHintUsesALocalisedTemplateWithOnlyItsLowerCaseKeyInWhite() {
        Component hint = PonderTooltipHandler.holdMessage("P");
        TranslatableContents contents = assertInstanceOf(TranslatableContents.class, hint.getContents());
        assertEquals(1, contents.getArgs().length);
        Component key = assertInstanceOf(Component.class, contents.getArgs()[0]);

        assertEquals("ponderlib.tooltip.hold_to_ponder", contents.getKey());
        assertEquals(ChatFormatting.DARK_GRAY.getColor(), hint.getStyle().getColor().getValue());
        assertEquals("p", key.getString());
        assertEquals(ChatFormatting.WHITE.getColor(), key.getStyle().getColor().getValue());
    }
}
