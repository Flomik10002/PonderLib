package dev.flomik.ponderlib.client;

import net.minecraft.client.KeyMapping;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientEventsTest {

    @Test
    void ponderKeyMappingUsesTheDocumentedKeyAndTranslationKeys() throws ReflectiveOperationException {
        Field field = ClientEvents.class.getDeclaredField("PONDER_KEY");
        assertTrue(java.lang.reflect.Modifier.isStatic(field.getModifiers()));
        field.setAccessible(true);
        KeyMapping mapping = (KeyMapping) field.get(null);

        assertEquals("key.ponderlib.ponder", mapping.getName());
        assertEquals("key.categories.ponderlib", mapping.getCategory());
        assertEquals(GLFW.GLFW_KEY_P, mapping.getDefaultKey().getValue());
    }
}
