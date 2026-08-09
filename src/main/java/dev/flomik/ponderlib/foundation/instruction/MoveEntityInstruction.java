package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.api.element.ElementLink;
import dev.flomik.ponderlib.api.element.EntityElement;
import dev.flomik.ponderlib.api.scene.CollisionMode;
import dev.flomik.ponderlib.api.scene.Easing;
import dev.flomik.ponderlib.foundation.PonderScene;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/**
 * Drives one linked entity from its runtime position to an absolute target. Unlike a storyboard
 * loop made from {@code modifyEntity + idle}, this owns the complete duration as one non-blocking
 * instruction and evaluates every sample against its captured start, so nonlinear easing cannot
 * accumulate per-tick error.
 */
public final class MoveEntityInstruction extends TickingInstruction {

    private final ElementLink<EntityElement> link;
    private final Vec3 target;
    private final Easing easing;
    private final CollisionMode collisionMode;

    private Entity entity;
    private Vec3 start;
    private Vec3 originalDeltaMovement;
    private boolean originalNoGravity;
    private boolean originalNoPhysics;
    private boolean stateCaptured;

    /**
     * Creates an entity movement that captures its start and physics state when first ticked.
     *
     * @param link          entity link to resolve at runtime
     * @param target        absolute scene-space destination
     * @param ticks         duration, clamped to at least one tick
     * @param easing        normalized progress curve
     * @param collisionMode whether vanilla block collision constrains each desired displacement
     */
    public MoveEntityInstruction(ElementLink<EntityElement> link, Vec3 target, int ticks, Easing easing,
                                 CollisionMode collisionMode) {
        super(false, Math.max(ticks, 1));
        this.link = Objects.requireNonNull(link, "link");
        this.target = Objects.requireNonNull(target, "target");
        this.easing = Objects.requireNonNull(easing, "easing");
        this.collisionMode = Objects.requireNonNull(collisionMode, "collisionMode");
    }

    @Override
    protected void firstTick(PonderScene scene) {
        EntityElement element = scene.resolve(link);
        if (element == null) {
            return;
        }
        element.ifPresent(value -> entity = value);
        if (entity == null) {
            return;
        }

        start = entity.position();
        originalDeltaMovement = entity.getDeltaMovement();
        originalNoGravity = entity.isNoGravity();
        originalNoPhysics = entity.noPhysics;
        stateCaptured = true;
    }

    @Override
    public void tick(PonderScene scene) {
        super.tick(scene);
        if (!stateCaptured) {
            return;
        }

        // Entity ticks happen before instructions in PonderScene, and ItemEntity/modded tick code
        // may rewrite any of these values. Reassert the movement contract on every sample, not
        // just firstTick, so autonomous motion cannot leak into the authored path and RESPECT
        // cannot accidentally retain an entity's original noPhysics=true state.
        entity.setDeltaMovement(Vec3.ZERO);
        entity.setNoGravity(true);
        entity.noPhysics = collisionMode == CollisionMode.IGNORE;

        int elapsedTicks = totalTicks - remainingTicks;
        double progress = Math.max(0.0, Math.min(1.0, elapsedTicks / (double) totalTicks));
        // The requested target remains authoritative at the endpoint even for a custom easing
        // whose author accidentally returns something other than 1 for progress=1. RESPECT still
        // routes that final desired delta through collision and therefore may legitimately stop
        // short; IGNORE reaches it exactly.
        Vec3 desired = remainingTicks == 0 ? target : start.lerp(target, easing.ease(progress));

        if (collisionMode == CollisionMode.IGNORE) {
            // setPos is intentionally authoritative: unlike Entity#move it does not ask the block
            // collision system to trim the delta, which is what lets a scripted item enter a pool
            // even when the pool block's shape would stop a normal throw.
            entity.setPos(desired);
        } else {
            entity.move(MoverType.SELF, desired.subtract(entity.position()));
        }

        if (remainingTicks == 0) {
            restoreEntityState();
        }
    }

    @Override
    public void reset(PonderScene scene) {
        // Also make an interrupted lifecycle harmless. PonderScene normally runs an instruction to
        // completion, but a replay resets all instructions after dropping the old scene entities.
        restoreEntityState();
        entity = null;
        start = null;
        super.reset(scene);
    }

    private void restoreEntityState() {
        if (!stateCaptured) {
            return;
        }
        entity.noPhysics = originalNoPhysics;
        entity.setNoGravity(originalNoGravity);
        entity.setDeltaMovement(originalDeltaMovement);
        stateCaptured = false;
    }
}
