package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.api.element.ElementLink;
import dev.flomik.ponderlib.api.element.PonderSceneElement;
import dev.flomik.ponderlib.foundation.PonderScene;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Animates any {@code Vec3}-valued property of a linked element linearly over {@code ticks},
 * snapping to the exact target on the last tick to avoid floating-point drift. Generic so one
 * instruction covers both rotating and moving a {@code WorldSectionElement} (see
 * {@code PonderSceneBuilder}'s {@code rotateSection}/{@code moveSection}) — and anything else with
 * an animatable Vec3 property later, without writing a new instruction class per property.
 */
public class AnimateElementInstruction<T extends PonderSceneElement> extends TickingInstruction {

    private final ElementLink<T> link;
    private final Vec3 totalDelta;
    private final Vec3 deltaPerTick;
    private final BiConsumer<T, Vec3> setter;
    private final Function<T, Vec3> getter;

    private T element;
    private Vec3 target;

    public AnimateElementInstruction(ElementLink<T> link, Vec3 totalDelta, int ticks,
                                      BiConsumer<T, Vec3> setter, Function<T, Vec3> getter) {
        super(false, Math.max(ticks, 1));
        this.link = link;
        this.totalDelta = totalDelta;
        this.deltaPerTick = totalDelta.scale(1.0 / Math.max(ticks, 1));
        this.setter = setter;
        this.getter = getter;
    }

    @Override
    protected void firstTick(PonderScene scene) {
        element = scene.resolve(link);
        if (element == null) {
            return;
        }
        target = getter.apply(element).add(totalDelta);
    }

    @Override
    public void tick(PonderScene scene) {
        super.tick(scene);
        if (element == null) {
            return;
        }
        if (remainingTicks == 0) {
            setter.accept(element, target);
            return;
        }
        setter.accept(element, getter.apply(element).add(deltaPerTick));
    }
}
