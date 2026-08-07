package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.api.ParticleEmitter;
import dev.flomik.ponderlib.foundation.PonderScene;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Fires {@code emitter} once per tick for {@code cycles} ticks, {@code amountPerCycle} times per
 * tick — the backing of {@code EffectInstructions#emitParticles}. {@code amountPerCycle} is a float
 * so a sub-1-per-tick rate (a light dusting rather than a burst) is expressible: a fractional
 * accumulator carries the remainder from tick to tick, so e.g. 0.5/cycle over 10 cycles still spawns
 * exactly 5 particles overall, not zero. Non-blocking, same as every other overlay/effect
 * instruction.
 */
public class EmitParticlesInstruction extends TickingInstruction {

    private final Vec3 location;
    private final ParticleEmitter emitter;
    private final float amountPerCycle;
    private final RandomSource random = RandomSource.create();
    private float accumulator;

    public EmitParticlesInstruction(Vec3 location, ParticleEmitter emitter, float amountPerCycle, int cycles) {
        super(false, Math.max(cycles, 1));
        this.location = location;
        this.emitter = emitter;
        this.amountPerCycle = amountPerCycle;
    }

    @Override
    public void tick(PonderScene scene) {
        super.tick(scene);
        accumulator += amountPerCycle;
        int count = (int) accumulator;
        accumulator -= count;
        for (int i = 0; i < count; i++) {
            emitter.emit(scene.getLevel(), location, random);
        }
    }
}
