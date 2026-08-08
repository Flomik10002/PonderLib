package dev.flomik.ponderlib.foundation.registration;
import dev.flomik.ponderlib.api.registration.PonderTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PonderTagRegistrationTest {
    @Test void registersMetadataAndComponents(){PonderTagRegistry r=new PonderTagRegistry();DefaultPonderTagRegistrationHelper h=new DefaultPonderTagRegistrationHelper("botania",r);ResourceLocation id=h.asLocation("flowers");PonderTag t=h.registerTag(id).title("Flowers").description("Mana").icon(Items.POPPY).addToIndex().register();h.addToTag(id,Items.POPPY,Items.DANDELION,Items.POPPY);assertSame(t,r.get(id));assertEquals(2,r.getComponents(id).size());}
    @Test void duplicateIdsFail(){PonderTagRegistry r=new PonderTagRegistry();DefaultPonderTagRegistrationHelper h=new DefaultPonderTagRegistrationHelper("test",r);h.registerTag("same").register();assertThrows(IllegalArgumentException.class,()->h.registerTag("same").register());}
}
