package dev.flomik.ponderlib.foundation.element;

import dev.flomik.ponderlib.api.element.EntityElement;
import net.minecraft.world.entity.Entity;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;

/**
 * A weak reference to the wrapped entity, not a strong one. The entity's own real lifetime is
 * {@code PonderLevel}'s entity list (removed there the tick it dies or falls out of the scene, see
 * {@code PonderLevel#tickEntities}) - this element's slot in {@code PonderScene#linkedElements}
 * otherwise outlives that until the next {@code begin()}, so a strong reference here would keep a
 * dead entity alive in memory for no reason until then.
 */
public class EntityElementImpl implements EntityElement {

    private final WeakReference<Entity> reference;
    private boolean visible = true;

    public EntityElementImpl(Entity entity) {
        this.reference = new WeakReference<>(entity);
    }

    @Override
    public void ifPresent(Consumer<Entity> action) {
        Entity entity = reference.get();
        if (entity != null) {
            action.accept(entity);
        }
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
