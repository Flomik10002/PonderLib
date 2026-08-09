package dev.flomik.ponderlib.mixin.client;

import dev.flomik.ponderlib.client.PonderNameplateDistance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Removes only the final NeoForge nameplate-distance gate for Ponder scene entities. */
@Mixin(EntityRenderer.class)
abstract class EntityRendererMixin {

    @Redirect(
        method = "renderNameTag(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/client/ClientHooks;"
                + "isNameplateInRenderDistance(Lnet/minecraft/world/entity/Entity;D)Z",
            remap = false
        ),
        require = 1
    )
    private boolean ponderlib$isNameplateInRenderDistance(Entity entity, double distance) {
        return PonderNameplateDistance.shouldBypass(entity)
            || ClientHooks.isNameplateInRenderDistance(entity, distance);
    }
}
