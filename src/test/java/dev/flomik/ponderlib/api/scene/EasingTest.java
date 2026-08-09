package dev.flomik.ponderlib.api.scene;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EasingTest {

    @Test
    void builtInCurvesHaveExactEndpoints() {
        for (Easing easing : new Easing[] { Easing.LINEAR, Easing.QUAD_IN, Easing.QUAD_OUT, Easing.QUAD_IN_OUT }) {
            assertEquals(0.0, easing.ease(0.0));
            assertEquals(1.0, easing.ease(1.0));
        }
    }

    @Test
    void quadraticCurvesProduceTheExpectedIntermediateProgress() {
        assertEquals(0.25, Easing.LINEAR.ease(0.25));
        assertEquals(0.0625, Easing.QUAD_IN.ease(0.25));
        assertEquals(0.4375, Easing.QUAD_OUT.ease(0.25));
        assertEquals(0.125, Easing.QUAD_IN_OUT.ease(0.25));
        assertEquals(0.875, Easing.QUAD_IN_OUT.ease(0.75));
    }

    @Test
    void customCurvesAreOrdinaryFunctionalInterfaceValues() {
        Easing cubic = progress -> progress * progress * progress;

        assertEquals(0.125, cubic.ease(0.5));
    }
}
