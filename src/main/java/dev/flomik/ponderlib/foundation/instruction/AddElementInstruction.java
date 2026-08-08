package dev.flomik.ponderlib.foundation.instruction;
import dev.flomik.ponderlib.api.element.ElementLink;
import dev.flomik.ponderlib.api.element.PonderElement;
import dev.flomik.ponderlib.foundation.PonderScene;
import java.util.function.Supplier;
public final class AddElementInstruction<E extends PonderElement> extends PonderInstruction {
    private final Supplier<? extends E> factory; private final ElementLink<E> link;
    public AddElementInstruction(Supplier<? extends E> factory, ElementLink<E> link) { this.factory=factory; this.link=link; }
    @Override public boolean isComplete() { return true; }
    @Override public void tick(PonderScene scene) { E element=factory.get(); if(element==null)throw new IllegalStateException("Custom Ponder element factory returned null"); element.setVisible(true); scene.addElement(element); scene.linkElement(element,link); }
}
