package dev.flomik.ponderlib.foundation;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import dev.flomik.ponderlib.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.ParticleOptions;
import org.joml.Matrix4fStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One scene's live vanilla {@link Particle}s. Owned by {@link PonderScene} and installed into its
 * {@link PonderLevel} as that level's {@link PonderParticleSink}, so anything inside the scene that
 * spawns a particle the ordinary way — {@code level.addParticle(...)} — just works. Particles are
 * deliberately NOT a scene element: they must outlive individual elements and exist from the
 * scene's first tick, before any storyboard instruction runs.
 * <p>
 * Two things feed this: {@code SceneBuilder.effects().emitSparks(...)} (an explicit storyboard
 * call), and each shown block's own {@code Block#animateTick} (see {@code
 * element.WorldSectionElementImpl#tick}) — which is how a lit furnace's smoke/flame appear without
 * the storyboard asking for anything.
 */
public class PonderSceneParticles implements PonderParticleSink {

    private final PonderLevel level;
    private final List<Particle> particles = new ArrayList<>();
    private final PonderSceneCamera camera = new PonderSceneCamera();
    private PonderClientLevelWrapper levelWrapper;
    private boolean cameraConfigured;

    public PonderSceneParticles(PonderLevel level) {
        this.level = level;
    }

    @Override
    public void addParticle(ParticleOptions options, double x, double y, double z, double mx, double my, double mz) {
        if (levelWrapper == null) {
            levelWrapper = PonderClientLevelWrapper.of(level);
        }
        Particle particle = PonderParticleProviders.create(options, levelWrapper, x, y, z, mx, my, mz);
        if (particle != null) {
            particles.add(particle);
        }
    }

    public int size() {
        return particles.size();
    }

    public void clear() {
        particles.clear();
    }

    public void tick() {
        // The real Particle handles its own gravity/drag/lifetime internally (and, since it queries
        // block state through the wrapped level - see PonderClientLevelWrapper - genuine collision
        // against this scene's own blocks too).
        for (Iterator<Particle> iterator = particles.iterator(); iterator.hasNext(); ) {
            Particle particle = iterator.next();
            particle.tick();
            if (!particle.isAlive()) {
                iterator.remove();
            }
        }
    }

    /**
     * Real vanilla particles render through the legacy GL modelview matrix stack ({@code
     * RenderSystem.getModelViewStack()}), not by pre-multiplying each vertex by this scene's pose
     * matrix in Java the way this codebase's custom quads (shadow/flash/outline) do - {@code
     * Particle#render} writes raw local coordinates straight into the {@link BufferBuilder} and
     * relies on that ambient matrix for the rest, so this scene's own pose matrix gets pushed into
     * it here instead, popped back off once done.
     */
    public void render(PoseStack poseStack, float partialTick) {
        if (particles.isEmpty()) {
            return;
        }
        if (!cameraConfigured) {
            // See PonderSceneCamera's javadoc for how these two arguments were calibrated against
            // PonderUI#applySceneTransform's own rotation calls, rather than guessed.
            camera.set(-PonderUI.CAMERA_X_ROTATION, PonderUI.CAMERA_Y_ROTATION + 180);
            cameraConfigured = true;
        }

        Map<ParticleRenderType, List<Particle>> byType = new LinkedHashMap<>();
        for (Particle particle : particles) {
            byType.computeIfAbsent(particle.getRenderType(), type -> new ArrayList<>()).add(particle);
        }

        Minecraft mc = Minecraft.getInstance();
        LightTexture lightTexture = mc.gameRenderer.lightTexture();
        lightTexture.turnOnLightLayer();
        RenderSystem.enableDepthTest();
        Matrix4fStack stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.mul(poseStack.last().pose());
        RenderSystem.applyModelViewMatrix();

        for (Map.Entry<ParticleRenderType, List<Particle>> entry : byType.entrySet()) {
            ParticleRenderType renderType = entry.getKey();
            if (renderType == ParticleRenderType.NO_RENDER) {
                continue;
            }
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.setShader(GameRenderer::getParticleShader);

            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferBuilder = renderType.begin(tesselator, mc.getTextureManager());
            if (bufferBuilder != null) {
                for (Particle particle : entry.getValue()) {
                    particle.render(bufferBuilder, camera, partialTick);
                }
                MeshData meshData = bufferBuilder.build();
                if (meshData != null) {
                    BufferUploader.drawWithShader(meshData);
                }
            }
        }

        stack.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        lightTexture.turnOffLightLayer();
    }
}
