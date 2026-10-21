package net.goldskinmc.creategeoresonance.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

/** A private, scene-depth-tested ore mask. Never writes to the world's depth or alpha. */
public final class SeismicProjectorOcclusion {
    private static ShaderInstance hologramShader;
    private static RenderTarget mask;
    private static boolean active;
    private static float[] previousColor;

    private SeismicProjectorOcclusion() {
    }

    public static void registerShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID, "projector_hologram"),
                DefaultVertexFormat.POSITION_COLOR), shader -> hologramShader = shader);
        } catch (IOException error) {
            throw new UncheckedIOException("Unable to load the projector hologram shader", error);
        }
    }

    public static void beginMask() {
        RenderSystem.assertOnRenderThread();
        previousColor = RenderSystem.getShaderColor().clone();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderTarget scene = Minecraft.getInstance().getMainRenderTarget();
        if (mask != null && mask.isStencilEnabled() != scene.isStencilEnabled()) {
            mask.destroyBuffers();
            mask = null;
        }
        if (mask == null) {
            mask = new TextureTarget(scene.width, scene.height, true, Minecraft.ON_OSX);
            mask.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        } else if (mask.width != scene.width || mask.height != scene.height) {
            mask.resize(scene.width, scene.height, Minecraft.ON_OSX);
        }
        // Depth blits require matching formats when another mod enables the scene stencil attachment.
        if (scene.isStencilEnabled() && !mask.isStencilEnabled()) {
            mask.enableStencil();
        }
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        mask.clear(Minecraft.ON_OSX);
        mask.copyDepthFrom(scene);
        mask.bindWrite(true);
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(-1.0F, -1.0F);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
    }

    public static void finishMask() {
        restoreColor();
        RenderSystem.polygonOffset(0.0F, 0.0F);
        RenderSystem.disablePolygonOffset();
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        active = true;
    }

    public static ShaderInstance shader() {
        if (!active) {
            throw new IllegalStateException("Projector occlusion mask was not prepared for this frame");
        }
        ShaderInstance shader = Objects.requireNonNull(hologramShader, "Projector hologram shader was not registered");
        shader.setSampler("OreMask", mask.getColorTextureId());
        shader.setSampler("SceneDepth", mask.getDepthTextureId());
        return shader;
    }

    public static void endFrame() {
        restoreColor();
        active = false;
        RenderSystem.polygonOffset(0.0F, 0.0F);
        RenderSystem.disablePolygonOffset();
        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    }

    public static void release() {
        active = false;
        if (mask != null) {
            mask.destroyBuffers();
            mask = null;
        }
    }

    private static void restoreColor() {
        if (previousColor != null) {
            RenderSystem.setShaderColor(previousColor[0], previousColor[1], previousColor[2], previousColor[3]);
            previousColor = null;
        }
    }
}
