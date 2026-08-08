package dev.flomik.ponderlib.foundation.registration;

import dev.flomik.ponderlib.api.registration.PonderTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PonderTagRegistrationTest {
    @Test
    void registersMetadataAndManyComponentsInStableOrder() {
        PonderTagRegistry registry = new PonderTagRegistry();
        DefaultPonderTagRegistrationHelper helper = new DefaultPonderTagRegistrationHelper("botania", registry);
        ResourceLocation id = helper.asLocation("functional_flowers");
        PonderTag tag = helper.registerTag(id).title("Functional Flowers").description("World interaction")
            .icon(Items.POPPY).addToIndex().register();
        helper.addToTag(id, Items.POPPY, Items.DANDELION, Items.POPPY);

        assertSame(tag, registry.get(id));
        assertEquals("Functional Flowers", tag.title().getString());
        assertEquals(2, registry.getComponents(id).size());
        assertEquals(java.util.List.of(tag), registry.getTags(BuiltInId.POPPY));
    }

    @Test
    void duplicateTagIdsFailFast() {
        PonderTagRegistry registry = new PonderTagRegistry();
        DefaultPonderTagRegistrationHelper helper = new DefaultPonderTagRegistrationHelper("test", registry);
        helper.registerTag("same").register();
        assertThrows(IllegalArgumentException.class, () -> helper.registerTag("same").register());
    }

    private static final class BuiltInId {
        private static final ResourceLocation POPPY = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(Items.POPPY);
    }
}
