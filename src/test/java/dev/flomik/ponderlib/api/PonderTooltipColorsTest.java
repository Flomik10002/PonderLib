package dev.flomik.ponderlib.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PonderTooltipColorsTest {

    @AfterEach
    void restoreDefaults() {
        PonderTooltipColors.set(0x2BC4FF, 0x4F7CFF, 0xFFFFFF);
    }

    @Test
    void interpolatesAcrossBothColorSegments() {
        PonderTooltipColors.set(0x000000, 0x808080, 0xFFFFFF);

        assertEquals(0x000000, PonderTooltipColors.forProgress(0.0F));
        assertEquals(0x404040, PonderTooltipColors.forProgress(0.25F));
        assertEquals(0x808080, PonderTooltipColors.forProgress(0.5F));
        assertEquals(0xBFBFBF, PonderTooltipColors.forProgress(0.75F));
        assertEquals(0xFFFFFF, PonderTooltipColors.forProgress(1.0F));
    }

    @Test
    void interpolatesEachRgbChannelIndependently() {
        PonderTooltipColors.set(0xFF0000, 0x00FF00, 0x0000FF);

        assertEquals(0x7F7F00, PonderTooltipColors.forProgress(0.25F));
        assertEquals(0x007F7F, PonderTooltipColors.forProgress(0.75F));
    }
}
