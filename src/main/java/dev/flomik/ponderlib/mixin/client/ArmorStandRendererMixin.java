package dev.flomik.ponderlib.mixin.client;

import dev.flomik.ponderlib.client.PonderNameplateDistance;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Covers the armor stand's independent nameplate-distance check, which does not call its parent. */
@Mixin(ArmorStandRenderer.class)
abstract class ArmorStandRendererMixin {

    @Redirect(
        method = "shouldShowName(Lnet/minecraft/world/entity/decoration/ArmorStand;)Z",
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
