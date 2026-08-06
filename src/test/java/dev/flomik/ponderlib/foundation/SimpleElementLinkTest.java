package dev.flomik.ponderlib.foundation;

import dev.flomik.ponderlib.api.element.PonderElement;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimpleElementLinkTest {

    @Test
    void retainsIdentityAndCastsMatchingElements() {
        UUID id = UUID.randomUUID();
        SimpleElementLink<TestElement> link = new SimpleElementLink<>(id, TestElement.class);
        TestElement element = new TestElement();

        assertEquals(id, link.getId());
        assertSame(element, link.cast(element));
    }

    @Test
    void rejectsAnElementOfTheWrongType() {
        SimpleElementLink<TestElement> link = new SimpleElementLink<>(TestElement.class);

        assertThrows(ClassCastException.class, () -> link.cast(new OtherElement()));
    }

    private static class TestElement extends BaseElement {
    }

    private static class OtherElement extends BaseElement {
    }

    private abstract static class BaseElement implements PonderElement {
        private boolean visible;

        @Override
        public boolean isVisible() {
            return visible;
        }

        @Override
        public void setVisible(boolean visible) {
            this.visible = visible;
        }
    }
}
