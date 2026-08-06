package dev.flomik.ponderlib.foundation;

import dev.flomik.ponderlib.api.element.ElementLink;
import dev.flomik.ponderlib.api.element.PonderElement;

import java.util.UUID;

public record SimpleElementLink<T extends PonderElement>(UUID id, Class<T> type) implements ElementLink<T> {

    public SimpleElementLink(Class<T> type) {
        this(UUID.randomUUID(), type);
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public T cast(PonderElement element) {
        return type.cast(element);
    }
}
