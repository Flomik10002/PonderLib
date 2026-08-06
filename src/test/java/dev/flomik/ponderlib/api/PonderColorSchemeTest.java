package dev.flomik.ponderlib.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PonderColorSchemeTest {

    @Test
    void tooltipBorderInterpolatesAcrossBothColorSegments() {
        PonderColorScheme colors = PonderColorScheme.builder()
            .tooltipBorder(0x000000, 0x808080, 0xFFFFFF)
            .build();

        assertEquals(0x000000, colors.tooltipBorderForProgress(0.0F));
        assertEquals(0x404040, colors.tooltipBorderForProgress(0.25F));
        assertEquals(0x808080, colors.tooltipBorderForProgress(0.5F));
        assertEquals(0xBFBFBF, colors.tooltipBorderForProgress(0.75F));
        assertEquals(0xFFFFFF, colors.tooltipBorderForProgress(1.0F));
    }

    @Test
    void tooltipBorderInterpolatesEachRgbChannelIndependently() {
        PonderColorScheme colors = PonderColorScheme.builder()
            .tooltipBorder(0xFF0000, 0x00FF00, 0x0000FF)
            .build();

        assertEquals(0x7F7F00, colors.tooltipBorderForProgress(0.25F));
        assertEquals(0x007F7F, colors.tooltipBorderForProgress(0.75F));
    }

    @Test
    void unsetFieldsKeepDefaultsValues() {
        PonderColorScheme colors = PonderColorScheme.builder()
            .finishingFlash(0x123456)
            .build();

        assertEquals(0x123456, colors.finishingFlash());
        assertEquals(PonderColorScheme.DEFAULT.basePlateShadow(), colors.basePlateShadow());
        assertEquals(PonderColorScheme.DEFAULT.frameBorderTop(), colors.frameBorderTop());
    }
}
