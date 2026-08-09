package dev.flomik.ponderlib.mixin.client;

import dev.flomik.ponderlib.client.PonderNameplateDistance;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps the living-entity visibility policy intact while neutralising its camera-distance input. */
@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityRendererMixin {

    @Redirect(
        method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;"
                + "distanceToSqr(Lnet/minecraft/world/entity/Entity;)D"
        ),
        require = 1
    )
    private double ponderlib$distanceToSqr(EntityRenderDispatcher dispatcher, Entity entity) {
        return PonderNameplateDistance.shouldBypass(entity) ? 0.0 : dispatcher.distanceToSqr(entity);
    }
}
