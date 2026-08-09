package dev.flomik.ponderlib.mixin.client;

import dev.flomik.ponderlib.client.PonderNameplateDistance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Removes only the final Forge nameplate-distance gate for Ponder scene entities. */
@Mixin(EntityRenderer.class)
abstract class EntityRendererMixin {

    @Redirect(
        method = "renderNameTag(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/client/ForgeHooksClient;"
                + "isNameplateInRenderDistance(Lnet/minecraft/world/entity/Entity;D)Z",
            remap = false
        ),
        require = 1
    )
    private boolean ponderlib$isNameplateInRenderDistance(Entity entity, double distance) {
        return PonderNameplateDistance.shouldBypass(entity)
            || ForgeHooksClient.isNameplateInRenderDistance(entity, distance);
    }
}
