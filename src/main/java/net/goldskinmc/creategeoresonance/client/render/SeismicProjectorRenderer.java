package net.goldskinmc.creategeoresonance.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import net.goldskinmc.creategeoresonance.seismic.SeismicAnomalyType;
import net.goldskinmc.creategeoresonance.seismic.SeismicProjectorBlockEntity;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

import java.util.List;

public class SeismicProjectorRenderer implements BlockEntityRenderer<SeismicProjectorBlockEntity> {
    private static final float BOX_SIZE = 0.45F;

    public SeismicProjectorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SeismicProjectorBlockEntity blockEntity, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        List<SeismicProjectorBlockEntity.TriangulatedCandidate> candidates = blockEntity.getTriangulatedCandidates();
        if (candidates.isEmpty()) {
            return;
        }

        BlockPos origin = blockEntity.getBlockPos();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        try {
            bufferBuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            for (SeismicProjectorBlockEntity.TriangulatedCandidate candidate : candidates) {
                float[] color = colorFor(candidate.type());
                double centerX = candidate.worldX() + 0.5D - origin.getX();
                double centerY = candidate.approxY() + 0.5D - origin.getY();
                double centerZ = candidate.worldZ() + 0.5D - origin.getZ();
                AABB box = new AABB(
                    centerX - BOX_SIZE, centerY - BOX_SIZE, centerZ - BOX_SIZE,
                    centerX + BOX_SIZE, centerY + BOX_SIZE, centerZ + BOX_SIZE
                );
                drawBoxEdges(bufferBuilder, poseStack, box, color[0], color[1], color[2], 1.0F);
            }
            BufferUploader.drawWithShader(bufferBuilder.end());
        } finally {
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(SeismicProjectorBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    private static void drawBoxEdges(BufferBuilder builder, PoseStack poseStack, AABB box,
                                     float r, float g, float b, float a) {
        Matrix4f matrix = poseStack.last().pose();
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        addEdge(builder, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        addEdge(builder, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        addEdge(builder, matrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        addEdge(builder, matrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

        addEdge(builder, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        addEdge(builder, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        addEdge(builder, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        addEdge(builder, matrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

        addEdge(builder, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        addEdge(builder, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        addEdge(builder, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        addEdge(builder, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    private static void addEdge(BufferBuilder builder, Matrix4f matrix,
                                float x1, float y1, float z1, float x2, float y2, float z2,
                                float r, float g, float b, float a) {
        builder.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
    }

    private static float[] colorFor(SeismicAnomalyType type) {
        return switch (type) {
            case WATER -> rgb(80, 170, 255);
            case LAVA -> rgb(255, 120, 40);
            case CAVE -> rgb(180, 200, 220);
            case COAL -> rgb(70, 70, 70);
            case IRON -> rgb(216, 166, 129);
            case COPPER -> rgb(211, 132, 75);
            case GOLD -> rgb(255, 215, 61);
            case REDSTONE -> rgb(229, 49, 49);
            case LAPIS -> rgb(66, 108, 208);
            case EMERALD -> rgb(49, 210, 113);
            case DIAMOND -> rgb(87, 238, 220);
            case ZINC -> rgb(156, 190, 205);
            case AMETHYST -> rgb(192, 138, 230);
            case CHEST -> rgb(171, 119, 66);
            case SPAWNER -> rgb(96, 168, 198);
            case SOLID -> rgb(160, 160, 160);
        };
    }

    private static float[] rgb(int r, int g, int b) {
        return new float[] {r / 255.0F, g / 255.0F, b / 255.0F};
    }
}
