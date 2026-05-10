package net.goldskinmc.creategeoresonance.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.goldskinmc.creategeoresonance.Config;
import net.goldskinmc.creategeoresonance.client.GeoResonancePartialModels;
import net.goldskinmc.creategeoresonance.seismic.SeismicAnomalyType;
import net.goldskinmc.creategeoresonance.seismic.SeismicProjectorBlockEntity;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;

public class SeismicProjectorRenderer extends KineticBlockEntityRenderer<SeismicProjectorBlockEntity> {
    private static final float BOX_SIZE = 0.45F;
    private static final float BLOCK_EDGE_INSET = 0.0F;
    private static final float DASH_LENGTH = 0.65F;
    private static final float DASH_GAP = 0.4F;
    private static final float GUIDE_START_X = 0.5F;
    private static final float GUIDE_START_Y = 0.06F;
    private static final float GUIDE_START_Z = 0.5F;
    private static final Direction LOCAL_SHAFT_AXIS = Direction.SOUTH;
    private static final float SHAFT_PIVOT_X = 8.0F;
    private static final float SHAFT_PIVOT_Y = 8.0F;
    private static final float SHAFT_PIVOT_Z = 14.0F;
    private static final float SEISMOGRAM_LEFT_OFFSET_X = 13.9F / 16.0F;
    private static final float SEISMOGRAM_LEFT_OFFSET_Y = 11.95F / 16.0F;
    private static final float SEISMOGRAM_LEFT_OFFSET_Z = 2.0F / 16.0F;
    private static final float SEISMOGRAM_RIGHT_OFFSET_X = 17.4F / 16.0F;
    private static final float SEISMOGRAM_RIGHT_OFFSET_Y = 11.95F / 16.0F;
    private static final float SEISMOGRAM_RIGHT_OFFSET_Z = -2.475F / 16.0F;
    private static final TagKey<Block> CREATE_ZINC_ORES = BlockTags.create(
        ResourceLocation.fromNamespaceAndPath("forge", "ores/zinc"));

    public SeismicProjectorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(SeismicProjectorBlockEntity blockEntity, float partialTicks, PoseStack poseStack,
                              MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        BlockState state = getRenderedBlockState(blockEntity);
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        RenderType renderType = getRenderType(blockEntity, state);
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
        SuperByteBuffer shaft = CachedBuffers.partial(GeoResonancePartialModels.SEISMIC_PROJECTOR_SHAFT, state);
        orientToFacing(shaft, facing);
        rotateAroundLocalPivot(shaft, shaftAngle(blockEntity, facing), LOCAL_SHAFT_AXIS, SHAFT_PIVOT_X, SHAFT_PIVOT_Y, SHAFT_PIVOT_Z);
        int shaftLight = LevelRenderer.getLightColor(blockEntity.getLevel(), blockEntity.getBlockPos().relative(facing.getOpposite()));
        shaft.light(shaftLight).renderInto(poseStack, vertexConsumer);
        int loadedNodes = blockEntity.getLoadedNodeCount();
        int seismogramLight = LevelRenderer.getLightColor(blockEntity.getLevel(), blockEntity.getBlockPos().above());
        if (loadedNodes >= 1) {
            SuperByteBuffer leftSeismogram = CachedBuffers.partial(GeoResonancePartialModels.SEISMIC_STATION_OUTPUT_SEISMOGRAM, state);
            orientToFacing(leftSeismogram, facing);
            leftSeismogram.translate(SEISMOGRAM_LEFT_OFFSET_X, SEISMOGRAM_LEFT_OFFSET_Y, SEISMOGRAM_LEFT_OFFSET_Z);
            leftSeismogram.light(seismogramLight).renderInto(poseStack, vertexConsumer);
        }
        if (loadedNodes >= 2) {
            SuperByteBuffer rightSeismogram = CachedBuffers.partial(GeoResonancePartialModels.SEISMIC_STATION_OUTPUT_SEISMOGRAM, state);
            orientToFacing(rightSeismogram, facing);
            rightSeismogram.translate(SEISMOGRAM_RIGHT_OFFSET_X, SEISMOGRAM_RIGHT_OFFSET_Y, SEISMOGRAM_RIGHT_OFFSET_Z);
            rightSeismogram.light(seismogramLight).renderInto(poseStack, vertexConsumer);
        }
        if (!blockEntity.hasRequiredSpeed()) {
            return;
        }

        List<SeismicProjectorBlockEntity.ExactVein> exactVeins = blockEntity.getConfirmedVeins();
        List<RenderableVein> renderableVeins = filterRenderableVeins(blockEntity, exactVeins);
        List<SeismicProjectorBlockEntity.TriangulatedCandidate> candidates = blockEntity.getTriangulatedCandidates();
        int visibleVeinsMax = Math.max(0, Config.PROJECTOR_VISIBLE_VEINS_MAX.get());
        List<RenderableVein> visibleVeins = limitVisibleVeins(blockEntity.getBlockPos(), renderableVeins, visibleVeinsMax);
        boolean hasExactData = !exactVeins.isEmpty();
        boolean renderExact = !visibleVeins.isEmpty();
        if (!hasExactData && candidates.isEmpty()) {
            return;
        }
        if (hasExactData && !renderExact) {
            return;
        }

        boolean fillEnabled = Config.PROJECTOR_FILL_ENABLED.get();
        boolean guideLinesEnabled = Config.PROJECTOR_GUIDE_LINES_ENABLED.get();
        float fillAlpha = Mth.clamp(Config.PROJECTOR_FILL_ALPHA.get() / 255.0F, 0.0F, 1.0F);
        float edgeAlpha = Mth.clamp(Config.PROJECTOR_EDGE_ALPHA.get() / 255.0F, 0.0F, 1.0F);
        if (edgeAlpha <= 0.0F && (!fillEnabled || fillAlpha <= 0.0F)) {
            return;
        }

        BlockPos origin = blockEntity.getBlockPos();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        try {
            if (fillEnabled && fillAlpha > 0.0F) {
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
                bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                if (renderExact) {
                    for (RenderableVein vein : visibleVeins) {
                        float[] color = colorFor(vein.type());
                        List<FaceQuad> surfaceFaces = buildSurfaceFaces(vein.blocks(), origin);
                        for (FaceQuad face : surfaceFaces) {
                            addQuad(bufferBuilder, poseStack.last().pose(),
                                face.x1(), face.y1(), face.z1(),
                                face.x2(), face.y2(), face.z2(),
                                face.x3(), face.y3(), face.z3(),
                                face.x4(), face.y4(), face.z4(),
                                color[0], color[1], color[2], fillAlpha);
                        }
                    }
                } else {
                    for (SeismicProjectorBlockEntity.TriangulatedCandidate candidate : candidates) {
                        float[] color = colorFor(candidate.type());
                        AABB box = toLocalCandidateBox(origin, candidate);
                        drawBoxFaces(bufferBuilder, poseStack, box, color[0], color[1], color[2], fillAlpha);
                    }
                }
                BufferUploader.drawWithShader(bufferBuilder.end());
            }

            if (edgeAlpha > 0.0F) {
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
                bufferBuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
                if (renderExact) {
                    for (RenderableVein vein : visibleVeins) {
                        float[] color = colorFor(vein.type());
                        List<LineSegment> joinedEdges = buildJoinedEdges(vein.blocks(), origin);
                        for (LineSegment edge : joinedEdges) {
                            addEdge(bufferBuilder, poseStack.last().pose(),
                                edge.x1(), edge.y1(), edge.z1(), edge.x2(), edge.y2(), edge.z2(),
                                color[0], color[1], color[2], edgeAlpha);
                        }
                    }
                } else {
                    for (SeismicProjectorBlockEntity.TriangulatedCandidate candidate : candidates) {
                        float[] color = colorFor(candidate.type());
                        AABB box = toLocalCandidateBox(origin, candidate);
                        drawBoxEdges(bufferBuilder, poseStack, box, color[0], color[1], color[2], edgeAlpha);
                    }
                }
                BufferUploader.drawWithShader(bufferBuilder.end());
            }

            if (guideLinesEnabled && edgeAlpha > 0.0F) {
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
                bufferBuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
                float startX = GUIDE_START_X;
                float startY = GUIDE_START_Y;
                float startZ = GUIDE_START_Z;

                if (renderExact) {
                    for (RenderableVein vein : visibleVeins) {
                        float[] centroid = veinCentroid(origin, vein.blocks());
                        float[] color = colorFor(vein.type());
                        drawDashedLine(bufferBuilder, poseStack.last().pose(),
                            startX, startY, startZ, centroid[0], centroid[1], centroid[2],
                            color[0], color[1], color[2], edgeAlpha);
                    }
                } else {
                    for (SeismicProjectorBlockEntity.TriangulatedCandidate candidate : candidates) {
                        float[] color = colorFor(candidate.type());
                        AABB box = toLocalCandidateBox(origin, candidate);
                        float targetX = (float) ((box.minX + box.maxX) * 0.5D);
                        float targetY = (float) ((box.minY + box.maxY) * 0.5D);
                        float targetZ = (float) ((box.minZ + box.maxZ) * 0.5D);
                        drawDashedLine(bufferBuilder, poseStack.last().pose(),
                            startX, startY, startZ, targetX, targetY, targetZ,
                            color[0], color[1], color[2], edgeAlpha);
                    }
                }
                BufferUploader.drawWithShader(bufferBuilder.end());
            }
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
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

    private static List<RenderableVein> filterRenderableVeins(SeismicProjectorBlockEntity blockEntity,
                                                              List<SeismicProjectorBlockEntity.ExactVein> exactVeins) {
        List<RenderableVein> filtered = new ArrayList<>();
        if (blockEntity.getLevel() == null) {
            return filtered;
        }
        for (SeismicProjectorBlockEntity.ExactVein vein : exactVeins) {
            List<BlockPos> liveBlocks = new ArrayList<>();
            for (BlockPos blockPos : vein.blocks()) {
                BlockState state = blockEntity.getLevel().getBlockState(blockPos);
                if (matchesType(state, vein.type())) {
                    liveBlocks.add(blockPos.immutable());
                }
            }
            if (!liveBlocks.isEmpty()) {
                filtered.add(new RenderableVein(vein.type(), List.copyOf(liveBlocks)));
            }
        }
        return filtered;
    }

    private static List<RenderableVein> limitVisibleVeins(BlockPos origin, List<RenderableVein> veins, int maxVisible) {
        if (veins.isEmpty() || maxVisible <= 0) {
            return List.of();
        }
        if (veins.size() <= maxVisible) {
            return veins;
        }
        List<RenderableVeinDistance> ranked = new ArrayList<>(veins.size());
        for (RenderableVein vein : veins) {
            float[] centroid = veinCentroid(origin, vein.blocks());
            float distSq = centroid[0] * centroid[0] + centroid[1] * centroid[1] + centroid[2] * centroid[2];
            ranked.add(new RenderableVeinDistance(vein, distSq));
        }
        ranked.sort(Comparator.comparingDouble(RenderableVeinDistance::distanceSq));

        List<RenderableVein> limited = new ArrayList<>(maxVisible);
        for (int i = 0; i < maxVisible; i++) {
            limited.add(ranked.get(i).vein());
        }
        return limited;
    }

    private static boolean matchesType(BlockState state, SeismicAnomalyType type) {
        return switch (type) {
            case CAVE -> state.isAir();
            case WATER -> state.getFluidState().is(FluidTags.WATER);
            case LAVA -> state.getFluidState().is(FluidTags.LAVA);
            case COAL -> state.is(BlockTags.COAL_ORES);
            case IRON -> state.is(BlockTags.IRON_ORES);
            case COPPER -> state.is(BlockTags.COPPER_ORES);
            case GOLD -> state.is(BlockTags.GOLD_ORES);
            case REDSTONE -> state.is(BlockTags.REDSTONE_ORES);
            case LAPIS -> state.is(BlockTags.LAPIS_ORES);
            case EMERALD -> state.is(BlockTags.EMERALD_ORES);
            case DIAMOND -> state.is(BlockTags.DIAMOND_ORES);
            case ZINC -> state.is(CREATE_ZINC_ORES);
            case AMETHYST -> state.is(Blocks.BUDDING_AMETHYST)
                || state.is(Blocks.AMETHYST_CLUSTER)
                || state.is(Blocks.LARGE_AMETHYST_BUD)
                || state.is(Blocks.MEDIUM_AMETHYST_BUD)
                || state.is(Blocks.SMALL_AMETHYST_BUD);
            case CHEST -> state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST);
            case SPAWNER -> state.is(Blocks.SPAWNER);
            default -> false;
        };
    }

    private static List<LineSegment> buildJoinedEdges(List<BlockPos> blocks, BlockPos origin) {
        Set<BlockPos> occupied = new HashSet<>(blocks);
        Map<FaceEdgeKey, Integer> orientedCounts = new HashMap<>();

        for (BlockPos pos : occupied) {
            int x = pos.getX() - origin.getX();
            int y = pos.getY() - origin.getY();
            int z = pos.getZ() - origin.getZ();
            for (Direction direction : Direction.values()) {
                if (occupied.contains(pos.relative(direction))) {
                    continue;
                }
                addFaceEdges(orientedCounts, x, y, z, direction);
            }
        }

        Set<EdgeKey> deduped = new HashSet<>();
        List<LineSegment> joined = new ArrayList<>();
        for (Map.Entry<FaceEdgeKey, Integer> entry : orientedCounts.entrySet()) {
            if ((entry.getValue() & 1) == 0) {
                continue;
            }
            EdgeKey edge = entry.getKey().edge();
            if (!deduped.add(edge)) {
                continue;
            }
            joined.add(new LineSegment(
                edge.a().x(), edge.a().y(), edge.a().z(),
                edge.b().x(), edge.b().y(), edge.b().z()
            ));
        }
        return joined;
    }

    private static List<FaceQuad> buildSurfaceFaces(List<BlockPos> blocks, BlockPos origin) {
        Set<BlockPos> occupied = new HashSet<>(blocks);
        List<FaceQuad> faces = new ArrayList<>();
        for (BlockPos pos : occupied) {
            int x = pos.getX() - origin.getX();
            int y = pos.getY() - origin.getY();
            int z = pos.getZ() - origin.getZ();
            for (Direction direction : Direction.values()) {
                if (occupied.contains(pos.relative(direction))) {
                    continue;
                }
                addFaceQuad(faces, x, y, z, direction);
            }
        }
        return faces;
    }

    private static void addFaceQuad(List<FaceQuad> faces, int x, int y, int z, Direction direction) {
        float minX = x + BLOCK_EDGE_INSET;
        float minY = y + BLOCK_EDGE_INSET;
        float minZ = z + BLOCK_EDGE_INSET;
        float maxX = x + 1.0F - BLOCK_EDGE_INSET;
        float maxY = y + 1.0F - BLOCK_EDGE_INSET;
        float maxZ = z + 1.0F - BLOCK_EDGE_INSET;

        switch (direction) {
            case DOWN -> faces.add(new FaceQuad(minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ));
            case UP -> faces.add(new FaceQuad(minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ));
            case NORTH -> faces.add(new FaceQuad(minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ));
            case SOUTH -> faces.add(new FaceQuad(minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ));
            case WEST -> faces.add(new FaceQuad(minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ));
            case EAST -> faces.add(new FaceQuad(maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ));
        }
    }

    private static void addFaceEdges(Map<FaceEdgeKey, Integer> counts, int x, int y, int z, Direction direction) {
        switch (direction) {
            case DOWN -> {
                addOrientedEdge(counts, direction, x, y, z, x + 1, y, z);
                addOrientedEdge(counts, direction, x + 1, y, z, x + 1, y, z + 1);
                addOrientedEdge(counts, direction, x + 1, y, z + 1, x, y, z + 1);
                addOrientedEdge(counts, direction, x, y, z + 1, x, y, z);
            }
            case UP -> {
                addOrientedEdge(counts, direction, x, y + 1, z, x + 1, y + 1, z);
                addOrientedEdge(counts, direction, x + 1, y + 1, z, x + 1, y + 1, z + 1);
                addOrientedEdge(counts, direction, x + 1, y + 1, z + 1, x, y + 1, z + 1);
                addOrientedEdge(counts, direction, x, y + 1, z + 1, x, y + 1, z);
            }
            case NORTH -> {
                addOrientedEdge(counts, direction, x, y, z, x + 1, y, z);
                addOrientedEdge(counts, direction, x + 1, y, z, x + 1, y + 1, z);
                addOrientedEdge(counts, direction, x + 1, y + 1, z, x, y + 1, z);
                addOrientedEdge(counts, direction, x, y + 1, z, x, y, z);
            }
            case SOUTH -> {
                addOrientedEdge(counts, direction, x, y, z + 1, x + 1, y, z + 1);
                addOrientedEdge(counts, direction, x + 1, y, z + 1, x + 1, y + 1, z + 1);
                addOrientedEdge(counts, direction, x + 1, y + 1, z + 1, x, y + 1, z + 1);
                addOrientedEdge(counts, direction, x, y + 1, z + 1, x, y, z + 1);
            }
            case WEST -> {
                addOrientedEdge(counts, direction, x, y, z, x, y, z + 1);
                addOrientedEdge(counts, direction, x, y, z + 1, x, y + 1, z + 1);
                addOrientedEdge(counts, direction, x, y + 1, z + 1, x, y + 1, z);
                addOrientedEdge(counts, direction, x, y + 1, z, x, y, z);
            }
            case EAST -> {
                addOrientedEdge(counts, direction, x + 1, y, z, x + 1, y, z + 1);
                addOrientedEdge(counts, direction, x + 1, y, z + 1, x + 1, y + 1, z + 1);
                addOrientedEdge(counts, direction, x + 1, y + 1, z + 1, x + 1, y + 1, z);
                addOrientedEdge(counts, direction, x + 1, y + 1, z, x + 1, y, z);
            }
        }
    }

    private static void addOrientedEdge(Map<FaceEdgeKey, Integer> counts, Direction normal,
                                        int x1, int y1, int z1, int x2, int y2, int z2) {
        EdgeKey edge = EdgeKey.of(x1, y1, z1, x2, y2, z2);
        FaceEdgeKey key = new FaceEdgeKey(normal, edge);
        counts.merge(key, 1, Integer::sum);
    }

    private static AABB toLocalCandidateBox(BlockPos origin, SeismicProjectorBlockEntity.TriangulatedCandidate candidate) {
        double centerX = candidate.worldX() + 0.5D - origin.getX();
        double centerY = candidate.approxY() + 0.5D - origin.getY();
        double centerZ = candidate.worldZ() + 0.5D - origin.getZ();
        return new AABB(
            centerX - BOX_SIZE, centerY - BOX_SIZE, centerZ - BOX_SIZE,
            centerX + BOX_SIZE, centerY + BOX_SIZE, centerZ + BOX_SIZE
        );
    }

    private static void drawBoxFaces(BufferBuilder builder, PoseStack poseStack, AABB box,
                                     float r, float g, float b, float a) {
        Matrix4f matrix = poseStack.last().pose();
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        addQuad(builder, matrix, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, r, g, b, a);
        addQuad(builder, matrix, maxX, minY, maxZ, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        addQuad(builder, matrix, minX, minY, maxZ, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, r, g, b, a);
        addQuad(builder, matrix, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, b, a);
        addQuad(builder, matrix, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        addQuad(builder, matrix, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, minX, minY, minZ, r, g, b, a);
    }

    private static void addQuad(BufferBuilder builder, Matrix4f matrix,
                                float x1, float y1, float z1, float x2, float y2, float z2,
                                float x3, float y3, float z3, float x4, float y4, float z4,
                                float r, float g, float b, float a) {
        builder.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x3, y3, z3).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x4, y4, z4).color(r, g, b, a).endVertex();
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

    private static void drawDashedLine(BufferBuilder builder, Matrix4f matrix,
                                       float x1, float y1, float z1, float x2, float y2, float z2,
                                       float r, float g, float b, float a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float distance = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance <= 0.001F) {
            return;
        }

        float inv = 1.0F / distance;
        float ux = dx * inv;
        float uy = dy * inv;
        float uz = dz * inv;
        float cursor = 0.0F;
        while (cursor < distance) {
            float dashEnd = Math.min(distance, cursor + DASH_LENGTH);
            float ax = x1 + ux * cursor;
            float ay = y1 + uy * cursor;
            float az = z1 + uz * cursor;
            float bx = x1 + ux * dashEnd;
            float by = y1 + uy * dashEnd;
            float bz = z1 + uz * dashEnd;
            addEdge(builder, matrix, ax, ay, az, bx, by, bz, r, g, b, a);
            cursor = dashEnd + DASH_GAP;
        }
    }

    private static float[] veinCentroid(BlockPos origin, List<BlockPos> blocks) {
        if (blocks.isEmpty()) {
            return new float[] {0.5F, 0.5F, 0.5F};
        }
        double sx = 0.0D;
        double sy = 0.0D;
        double sz = 0.0D;
        for (BlockPos blockPos : blocks) {
            sx += blockPos.getX() - origin.getX() + 0.5D;
            sy += blockPos.getY() - origin.getY() + 0.5D;
            sz += blockPos.getZ() - origin.getZ() + 0.5D;
        }
        double invCount = 1.0D / blocks.size();
        return new float[] {(float) (sx * invCount), (float) (sy * invCount), (float) (sz * invCount)};
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

    private record IntPoint(int x, int y, int z) {
    }

    private record EdgeKey(IntPoint a, IntPoint b) {
        private static EdgeKey of(int x1, int y1, int z1, int x2, int y2, int z2) {
            IntPoint p1 = new IntPoint(x1, y1, z1);
            IntPoint p2 = new IntPoint(x2, y2, z2);
            if (compare(p1, p2) <= 0) {
                return new EdgeKey(p1, p2);
            }
            return new EdgeKey(p2, p1);
        }

        private static int compare(IntPoint left, IntPoint right) {
            if (left.x() != right.x()) {
                return Integer.compare(left.x(), right.x());
            }
            if (left.y() != right.y()) {
                return Integer.compare(left.y(), right.y());
            }
            return Integer.compare(left.z(), right.z());
        }
    }

    private record FaceEdgeKey(Direction normal, EdgeKey edge) {
    }

    private record LineSegment(float x1, float y1, float z1, float x2, float y2, float z2) {
    }

    private record FaceQuad(
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,
        float x4, float y4, float z4) {
    }

    private record RenderableVein(SeismicAnomalyType type, List<BlockPos> blocks) {
    }

    private record RenderableVeinDistance(RenderableVein vein, float distanceSq) {
    }

    private static float shaftAngle(SeismicProjectorBlockEntity blockEntity, Direction facing) {
        if (blockEntity.getLevel() == null) {
            return 0.0F;
        }
        Direction.Axis axis = KineticBlockEntityRenderer.getRotationAxisOf(blockEntity);
        float time = AnimationTickHolder.getRenderTime(blockEntity.getLevel());
        float offset = KineticBlockEntityRenderer.getRotationOffsetForPosition(blockEntity, blockEntity.getBlockPos(), axis);
        float rawDegrees = time * blockEntity.getSpeed() * 3f / 10 + offset;
        float rawAngle = (rawDegrees % 360f) / 180f * (float) Math.PI;
        float facingSign = facing.getAxis() == Direction.Axis.X ? -1.0F : 1.0F;
        return -rawAngle * facingSign;
    }

    private static void orientToFacing(SuperByteBuffer buffer, Direction facing) {
        buffer.center()
            .rotateYDegrees(180 + AngleHelper.horizontalAngle(facing))
            .uncenter();
    }

    private static void rotateAroundLocalPivot(SuperByteBuffer buffer, float angle, Direction axisDirection,
                                               float pivotX, float pivotY, float pivotZ) {
        float px = pivotX / 16.0F;
        float py = pivotY / 16.0F;
        float pz = pivotZ / 16.0F;
        buffer.translate(px, py, pz)
            .rotate(angle, axisDirection)
            .translate(-px, -py, -pz);
    }
}
