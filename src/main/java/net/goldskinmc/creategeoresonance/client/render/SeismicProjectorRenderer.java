package net.goldskinmc.creategeoresonance.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.outliner.Outline;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.ponder.api.level.PonderLevel;
import net.goldskinmc.creategeoresonance.Config;
import net.goldskinmc.creategeoresonance.client.GeoResonancePartialModels;
import net.goldskinmc.creategeoresonance.seismic.SeismicAnomalyType;
import net.goldskinmc.creategeoresonance.seismic.SeismicMineralTags;
import net.goldskinmc.creategeoresonance.seismic.SeismicRockStoneDeposits;
import net.goldskinmc.creategeoresonance.seismic.SeismicFluidTags;
import net.goldskinmc.creategeoresonance.seismic.SeismicProjectorBlockEntity;
import net.goldskinmc.creategeoresonance.seismic.SeismicProjectorBlock;
import net.goldskinmc.creategeoresonance.seismic.SeismicProjectorData;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import java.util.function.Consumer;

public class SeismicProjectorRenderer extends KineticBlockEntityRenderer<SeismicProjectorBlockEntity> {
    private static final float BOX_SIZE = 0.45F;
    private static final float DASH_LENGTH = 0.65F;
    private static final float DASH_GAP = 0.4F;
    private static final float GUIDE_CENTER_X = 0.5F;
    private static final float GUIDE_CENTER_Y = 0.05F;
    private static final float GUIDE_CENTER_Z = 0.5F;
    private static final float GUIDE_FORWARD_OFFSET = 0.0F;
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
    private static final float EXACT_GUIDE_ALPHA_SCALE = 0.82F;
    private static final int MAX_PONDER_OUTLINES = 48;
    private static final int MAX_GEOMETRY_CACHE_ENTRIES = 256;
    private static final Map<Object, DeferredProjection> DEFERRED_PROJECTIONS = new IdentityHashMap<>();
    private static final Map<Object, GeometryCacheEntry> GEOMETRY_CACHE =
        Collections.synchronizedMap(new IdentityHashMap<>());
    private static final TagKey<Block> CREATE_ZINC_ORES = BlockTags.create(
        ResourceLocation.fromNamespaceAndPath("c", "ores/zinc"));

    public SeismicProjectorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(SeismicProjectorBlockEntity blockEntity, float partialTicks, PoseStack poseStack,
                              MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        boolean ponderLevel = blockEntity.getLevel() instanceof PonderLevel;
        BlockState state = getRenderedBlockState(blockEntity);
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        RenderType renderType = getRenderType(blockEntity, state);
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
        int shaftLight = LevelRenderer.getLightColor(blockEntity.getLevel(), blockEntity.getBlockPos().relative(facing.getOpposite()));
        SuperByteBuffer shaft = CachedBuffers.partial(GeoResonancePartialModels.SEISMIC_PROJECTOR_SHAFT, state);
        orientToFacing(shaft, facing);
        rotateAroundLocalPivot(shaft, shaftAngle(blockEntity, facing), LOCAL_SHAFT_AXIS, SHAFT_PIVOT_X, SHAFT_PIVOT_Y, SHAFT_PIVOT_Z);
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
        if (ponderLevel) {
            renderProjection(blockEntity, poseStack);
        } else {
            deferProjection(blockEntity.getLevel(), blockEntity, poseStack, deferredStack -> {
                if (!blockEntity.isRemoved()) {
                    renderProjection(blockEntity, deferredStack);
                }
            });
        }
    }

    private static void renderProjection(SeismicProjectorBlockEntity blockEntity, PoseStack poseStack) {
        boolean ponderLevel = blockEntity.getLevel() instanceof PonderLevel;
        Direction facing = blockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        if (!blockEntity.getBlockState().hasProperty(SeismicProjectorBlock.ACTIVE)
            || !blockEntity.getBlockState().getValue(SeismicProjectorBlock.ACTIVE)) {
            if (ponderLevel) {
                clearPonderOutlines((PonderLevel) blockEntity.getLevel(), blockEntity.getBlockPos());
            }
            return;
        }

        List<SeismicProjectorData.ExactVein> exactVeins = blockEntity.getConfirmedVeins();
        List<RenderableVein> renderableVeins = ponderLevel
            ? toRenderableVeins(exactVeins)
            : filterRenderableVeins(blockEntity.getLevel(), exactVeins);
        List<SeismicProjectorData.TriangulatedCandidate> candidates =
            filterRenderableCandidates(blockEntity.getLevel(), blockEntity.getTriangulatedCandidates());
        int visibleVeinsMax = Math.max(0, Config.PROJECTOR_VISIBLE_VEINS_MAX.get());
        List<RenderableVein> visibleVeins = limitVisibleVeins(blockEntity.getBlockPos(), renderableVeins, visibleVeinsMax);
        boolean hasExactData = !exactVeins.isEmpty();
        boolean renderExact = !visibleVeins.isEmpty();
        if (!hasExactData && candidates.isEmpty()) {
            if (ponderLevel) {
                clearPonderOutlines((PonderLevel) blockEntity.getLevel(), blockEntity.getBlockPos());
            }
            return;
        }
        if (hasExactData && !renderExact) {
            if (ponderLevel) {
                clearPonderOutlines((PonderLevel) blockEntity.getLevel(), blockEntity.getBlockPos());
            }
            return;
        }
        if (ponderLevel) {
            renderPonderOutlines((PonderLevel) blockEntity.getLevel(), blockEntity.getBlockPos(), visibleVeins);
            return;
        }

        boolean fillEnabled = Config.PROJECTOR_FILL_ENABLED.get();
        boolean guideLinesEnabled = Config.PROJECTOR_GUIDE_LINES_ENABLED.get();
        float fillAlpha = Mth.clamp(Config.PROJECTOR_FILL_ALPHA.get() / 255.0F, 0.0F, 1.0F);
        float edgeAlpha = Mth.clamp(Config.PROJECTOR_EDGE_ALPHA.get() / 255.0F, 0.0F, 1.0F);
        if (edgeAlpha <= 0.0F && (!fillEnabled || fillAlpha <= 0.0F)) {
            return;
        }
        float exactBlockInset = projectorBlockInset();

        BlockPos origin = blockEntity.getBlockPos();
        RenderGeometryData geometry = resolveRenderGeometry(blockEntity, origin, visibleVeins, candidates, renderExact, exactBlockInset);
        Tesselator tesselator = Tesselator.getInstance();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        // Cull only filled surfaces; outlines and guide beams stay two-sided.
        RenderSystem.enableCull();

        try {
            if (fillEnabled && fillAlpha > 0.0F) {
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
                BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                if (renderExact) {
                    for (ExactVeinGeometry vein : geometry.exactVeins()) {
                        float[] color = colorFor(vein.type());
                        for (FaceQuad face : vein.faces()) {
                            addQuad(bufferBuilder, poseStack.last().pose(),
                                face.x1(), face.y1(), face.z1(),
                                face.x2(), face.y2(), face.z2(),
                                face.x3(), face.y3(), face.z3(),
                                face.x4(), face.y4(), face.z4(),
                                color[0], color[1], color[2], fillAlpha);
                        }
                    }
                } else {
                    for (CandidateGeometry candidate : geometry.candidates()) {
                        float[] color = colorFor(candidate.type());
                        AABB box = candidate.box();
                        drawBoxFaces(bufferBuilder, poseStack, box, color[0], color[1], color[2], fillAlpha);
                    }
                }
                BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
            }

            RenderSystem.disableCull();

            if (edgeAlpha > 0.0F) {
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
                BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
                if (renderExact) {
                    for (ExactVeinGeometry vein : geometry.exactVeins()) {
                        float[] color = colorFor(vein.type());
                        for (LineSegment edge : vein.edges()) {
                            addEdge(bufferBuilder, poseStack.last().pose(),
                                edge.x1(), edge.y1(), edge.z1(), edge.x2(), edge.y2(), edge.z2(),
                                color[0], color[1], color[2], edgeAlpha);
                        }
                    }
                } else {
                    for (CandidateGeometry candidate : geometry.candidates()) {
                        float[] color = colorFor(candidate.type());
                        AABB box = candidate.box();
                        drawBoxEdges(bufferBuilder, poseStack, box, color[0], color[1], color[2], edgeAlpha);
                    }
                }
                BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
            }

            if (guideLinesEnabled && edgeAlpha > 0.0F) {
                float guideHalfWidth = guideLineHalfWidth(projectorGuideLineWidth());
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
                BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                float[] guideStart = guideStartForFacing(facing);
                float startX = guideStart[0];
                float startY = guideStart[1];
                float startZ = guideStart[2];
                if (renderExact) {
                    float exactGuideAlpha = edgeAlpha * EXACT_GUIDE_ALPHA_SCALE;
                    for (ExactVeinGeometry vein : geometry.exactVeins()) {
                        float[] color = colorFor(vein.type());
                        drawDashedLineThick(bufferBuilder, poseStack.last().pose(),
                            startX, startY, startZ, vein.centroidX(), vein.centroidY(), vein.centroidZ(),
                            color[0], color[1], color[2], exactGuideAlpha, guideHalfWidth);
                    }
                } else {
                    for (CandidateGeometry candidate : geometry.candidates()) {
                        float[] color = colorFor(candidate.type());
                        drawDashedLineThick(bufferBuilder, poseStack.last().pose(),
                            startX, startY, startZ, candidate.centerX(), candidate.centerY(), candidate.centerZ(),
                            color[0], color[1], color[2], edgeAlpha, guideHalfWidth);
                    }
                }
                BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
            }
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    private static void deferProjection(net.minecraft.world.level.Level level, Object key,
                                        PoseStack poseStack, Consumer<PoseStack> render) {
        // Preserve the original view transform; the final pass has different matrices on each loader.
        PoseStack deferredStack = new PoseStack();
        deferredStack.last().pose().set(RenderSystem.getModelViewMatrix()).mul(poseStack.last().pose());
        DEFERRED_PROJECTIONS.put(key, new DeferredProjection(level, deferredStack, render));
    }

    public static void renderDeferredProjectionOverlays(net.minecraft.world.level.Level level) {
        if (DEFERRED_PROJECTIONS.isEmpty()) {
            return;
        }
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();
        try {
            for (DeferredProjection projection : DEFERRED_PROJECTIONS.values()) {
                if (projection.level() == level) {
                    projection.render().accept(projection.poseStack());
                }
            }
        } finally {
            DEFERRED_PROJECTIONS.clear();
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    public static void clearDeferredProjectionOverlays() {
        DEFERRED_PROJECTIONS.clear();
    }

    private record DeferredProjection(net.minecraft.world.level.Level level, PoseStack poseStack,
                                      Consumer<PoseStack> render) {
    }

    public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
                                           ContraptionMatrices matrices, MultiBufferSource buffer) {
        if (context.world == null || context.contraption == null || context.contraption.entity == null) {
            return;
        }

        BlockState state = context.state;
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        VertexConsumer vertexConsumer = buffer.getBuffer(resolveRenderType(state));

        int shaftLight = LevelRenderer.getLightColor(renderWorld, context.localPos.relative(facing.getOpposite()));
        SuperByteBuffer shaft = CachedBuffers.partial(GeoResonancePartialModels.SEISMIC_PROJECTOR_SHAFT, state);
        shaft.transform(matrices.getModel());
        orientToFacing(shaft, facing);
        rotateAroundLocalPivot(shaft, mountedShaftAngle(context, facing), LOCAL_SHAFT_AXIS, SHAFT_PIVOT_X, SHAFT_PIVOT_Y, SHAFT_PIVOT_Z);
        shaft.light(shaftLight)
            .useLevelLight(context.world, matrices.getWorld())
            .renderInto(matrices.getViewProjection(), vertexConsumer);

        if (context.blockEntityData == null) {
            context.blockEntityData = new CompoundTag();
        }
        SeismicProjectorData projectorData = new SeismicProjectorData();
        projectorData.readFromTag(context.blockEntityData);

        int loadedNodes = projectorData.getLoadedNodeCount();
        int seismogramLight = LevelRenderer.getLightColor(renderWorld, context.localPos.above());
        if (loadedNodes >= 1) {
            SuperByteBuffer leftSeismogram = CachedBuffers.partial(GeoResonancePartialModels.SEISMIC_STATION_OUTPUT_SEISMOGRAM, state);
            leftSeismogram.transform(matrices.getModel());
            orientToFacing(leftSeismogram, facing);
            leftSeismogram.translate(SEISMOGRAM_LEFT_OFFSET_X, SEISMOGRAM_LEFT_OFFSET_Y, SEISMOGRAM_LEFT_OFFSET_Z);
            leftSeismogram.light(seismogramLight)
                .useLevelLight(context.world, matrices.getWorld())
                .renderInto(matrices.getViewProjection(), vertexConsumer);
        }
        if (loadedNodes >= 2) {
            SuperByteBuffer rightSeismogram = CachedBuffers.partial(GeoResonancePartialModels.SEISMIC_STATION_OUTPUT_SEISMOGRAM, state);
            rightSeismogram.transform(matrices.getModel());
            orientToFacing(rightSeismogram, facing);
            rightSeismogram.translate(SEISMOGRAM_RIGHT_OFFSET_X, SEISMOGRAM_RIGHT_OFFSET_Y, SEISMOGRAM_RIGHT_OFFSET_Z);
            rightSeismogram.light(seismogramLight)
                .useLevelLight(context.world, matrices.getWorld())
                .renderInto(matrices.getViewProjection(), vertexConsumer);
        }
    }

    public static void queueMountedProjectionOverlay(MovementContext context, PoseStack poseStack, Vec3 camera) {
        deferProjection(context.world, context, poseStack,
            deferredStack -> renderMountedProjectionOverlay(context, deferredStack, camera));
    }

    private static void renderMountedProjectionOverlay(MovementContext context, PoseStack poseStack, Vec3 camera) {
        if (context.world == null || context.contraption == null || context.contraption.entity == null) {
            return;
        }
        if (context.blockEntityData == null) {
            return;
        }

        Vec3 projectorCenter = mountedProjectorCenter(context);
        BlockPos projectorPos = BlockPos.containing(projectorCenter);
        BlockState state = context.state;
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        SeismicProjectorData projectorData = new SeismicProjectorData();
        projectorData.readFromTag(context.blockEntityData);

        if (!projectorData.canProjectFrom(projectorPos, true)) {
            return;
        }

        List<SeismicProjectorData.ExactVein> exactVeins = projectorData.getConfirmedVeins();
        List<RenderableVein> renderableVeins = filterRenderableVeins(context.world, exactVeins);
        List<SeismicProjectorData.TriangulatedCandidate> candidates =
            filterRenderableCandidates(context.world, projectorData.getTriangulatedCandidates());
        int visibleVeinsMax = Math.max(0, Config.PROJECTOR_VISIBLE_VEINS_MAX.get());
        List<RenderableVein> visibleVeins = limitVisibleVeins(projectorPos, renderableVeins, visibleVeinsMax);
        boolean hasExactData = !exactVeins.isEmpty();
        boolean renderExact = !visibleVeins.isEmpty();
        if ((!hasExactData && candidates.isEmpty()) || (hasExactData && !renderExact)) {
            return;
        }

        boolean fillEnabled = Config.PROJECTOR_FILL_ENABLED.get();
        boolean guideLinesEnabled = Config.PROJECTOR_GUIDE_LINES_ENABLED.get();
        float fillAlpha = Mth.clamp(Config.PROJECTOR_FILL_ALPHA.get() / 255.0F, 0.0F, 1.0F);
        float edgeAlpha = Mth.clamp(Config.PROJECTOR_EDGE_ALPHA.get() / 255.0F, 0.0F, 1.0F);
        if (edgeAlpha <= 0.0F && (!fillEnabled || fillAlpha <= 0.0F)) {
            return;
        }

        float exactBlockInset = projectorBlockInset();
        RenderGeometryData geometry = resolveWorldRenderGeometry(context, visibleVeins, candidates, renderExact, exactBlockInset);
        Tesselator tesselator = Tesselator.getInstance();
        PoseStack localRenderStack = new PoseStack();
        ContraptionMatrices.transform(localRenderStack, poseStack);

        float partialTicks = AnimationTickHolder.getPartialTicks();
        Vec3 prevPosition = context.contraption.entity.getPrevPositionVec();
        double entityX = Mth.lerp(partialTicks, prevPosition.x, context.contraption.entity.getX());
        double entityY = Mth.lerp(partialTicks, prevPosition.y, context.contraption.entity.getY());
        double entityZ = Mth.lerp(partialTicks, prevPosition.z, context.contraption.entity.getZ());
        localRenderStack.translate(entityX - camera.x, entityY - camera.y, entityZ - camera.z);
        context.contraption.entity.applyLocalTransforms(localRenderStack, partialTicks);
        localRenderStack.translate(context.localPos.getX(), context.localPos.getY(), context.localPos.getZ());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        // Cull only filled surfaces; outlines and guide beams stay two-sided.
        RenderSystem.enableCull();

        try {
            if (fillEnabled && fillAlpha > 0.0F) {
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
                BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                if (renderExact) {
                    for (ExactVeinGeometry vein : geometry.exactVeins()) {
                        float[] color = colorFor(vein.type());
                        for (FaceQuad face : vein.faces()) {
                            Vec3 p1 = mountedTargetLocal(context, face.x1(), face.y1(), face.z1());
                            Vec3 p2 = mountedTargetLocal(context, face.x2(), face.y2(), face.z2());
                            Vec3 p3 = mountedTargetLocal(context, face.x3(), face.y3(), face.z3());
                            Vec3 p4 = mountedTargetLocal(context, face.x4(), face.y4(), face.z4());
                            addQuad(bufferBuilder, localRenderStack.last().pose(),
                                (float) p1.x, (float) p1.y, (float) p1.z,
                                (float) p2.x, (float) p2.y, (float) p2.z,
                                (float) p3.x, (float) p3.y, (float) p3.z,
                                (float) p4.x, (float) p4.y, (float) p4.z,
                                color[0], color[1], color[2], fillAlpha);
                        }
                    }
                } else {
                    for (CandidateGeometry candidate : geometry.candidates()) {
                        float[] color = colorFor(candidate.type());
                        drawMountedBoxFaces(bufferBuilder, localRenderStack.last().pose(), context, candidate.box(),
                            color[0], color[1], color[2], fillAlpha);
                    }
                }
                BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
            }

            RenderSystem.disableCull();

            if (edgeAlpha > 0.0F) {
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
                BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
                if (renderExact) {
                    for (ExactVeinGeometry vein : geometry.exactVeins()) {
                        float[] color = colorFor(vein.type());
                        for (LineSegment edge : vein.edges()) {
                            Vec3 p1 = mountedTargetLocal(context, edge.x1(), edge.y1(), edge.z1());
                            Vec3 p2 = mountedTargetLocal(context, edge.x2(), edge.y2(), edge.z2());
                            addEdge(bufferBuilder, localRenderStack.last().pose(),
                                (float) p1.x, (float) p1.y, (float) p1.z,
                                (float) p2.x, (float) p2.y, (float) p2.z,
                                color[0], color[1], color[2], edgeAlpha);
                        }
                    }
                } else {
                    for (CandidateGeometry candidate : geometry.candidates()) {
                        float[] color = colorFor(candidate.type());
                        drawMountedBoxEdges(bufferBuilder, localRenderStack.last().pose(), context, candidate.box(),
                            color[0], color[1], color[2], edgeAlpha);
                    }
                }
                BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
            }

            if (guideLinesEnabled && edgeAlpha > 0.0F) {
                float guideHalfWidth = guideLineHalfWidth(projectorGuideLineWidth());
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
                BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                float[] guideStart = guideStartForFacing(facing);
                if (renderExact) {
                    float exactGuideAlpha = edgeAlpha * EXACT_GUIDE_ALPHA_SCALE;
                    for (ExactVeinGeometry vein : geometry.exactVeins()) {
                        float[] color = colorFor(vein.type());
                        Vec3 localTarget = mountedTargetLocal(context, vein.centroidX(), vein.centroidY(), vein.centroidZ());
                        drawDashedLineThick(bufferBuilder, localRenderStack.last().pose(),
                            guideStart[0], guideStart[1], guideStart[2],
                            (float) localTarget.x, (float) localTarget.y, (float) localTarget.z,
                            color[0], color[1], color[2], exactGuideAlpha, guideHalfWidth);
                    }
                } else {
                    for (CandidateGeometry candidate : geometry.candidates()) {
                        float[] color = colorFor(candidate.type());
                        Vec3 localTarget = mountedTargetLocal(context, candidate.centerX(), candidate.centerY(), candidate.centerZ());
                        drawDashedLineThick(bufferBuilder, localRenderStack.last().pose(),
                            guideStart[0], guideStart[1], guideStart[2],
                            (float) localTarget.x, (float) localTarget.y, (float) localTarget.z,
                            color[0], color[1], color[2], edgeAlpha, guideHalfWidth);
                    }
                }
                BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
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

    private static List<RenderableVein> filterRenderableVeins(net.minecraft.world.level.Level level,
                                                              List<SeismicProjectorData.ExactVein> exactVeins) {
        List<RenderableVein> filtered = new ArrayList<>();
        if (level == null) {
            return filtered;
        }
        for (SeismicProjectorData.ExactVein vein : exactVeins) {
            List<BlockPos> liveBlocks = new ArrayList<>();
            for (BlockPos blockPos : vein.blocks()) {
                if (!level.hasChunkAt(blockPos)) {
                    continue;
                }
                BlockState state = level.getBlockState(blockPos);
                if (matchesType(state, vein.type())) {
                    liveBlocks.add(blockPos.immutable());
                }
            }
            if (!liveBlocks.isEmpty()) {
                filtered.add(new RenderableVein(vein.type(), List.copyOf(liveBlocks), vein.blocks()));
            }
        }
        return filtered;
    }

    private static List<RenderableVein> toRenderableVeins(List<SeismicProjectorData.ExactVein> exactVeins) {
        List<RenderableVein> renderable = new ArrayList<>();
        for (SeismicProjectorData.ExactVein vein : exactVeins) {
            if (vein.blocks().isEmpty()) {
                continue;
            }
            renderable.add(new RenderableVein(vein.type(), List.copyOf(vein.blocks())));
        }
        return renderable;
    }

    private static List<SeismicProjectorData.TriangulatedCandidate> filterRenderableCandidates(
        net.minecraft.world.level.Level level, List<SeismicProjectorData.TriangulatedCandidate> candidates) {
        if (level == null || candidates.isEmpty()) {
            return List.of();
        }
        List<SeismicProjectorData.TriangulatedCandidate> filtered = new ArrayList<>(candidates.size());
        for (SeismicProjectorData.TriangulatedCandidate candidate : candidates) {
            BlockPos targetPos = new BlockPos(candidate.worldX(), candidate.approxY(), candidate.worldZ());
            if (level.hasChunkAt(targetPos)) {
                filtered.add(candidate);
            }
        }
        return filtered;
    }

    private static void renderPonderOutlines(PonderLevel level, BlockPos projectorPos, List<RenderableVein> visibleVeins) {
        Outliner outliner = resolvePonderOutliner(level);
        if (outliner == null) {
            return;
        }
        BlockState projectorState = level.getBlockState(projectorPos);
        Direction facing = projectorState.hasProperty(HorizontalDirectionalBlock.FACING)
            ? projectorState.getValue(HorizontalDirectionalBlock.FACING)
            : Direction.NORTH;
        int index = 0;
        for (RenderableVein vein : visibleVeins) {
            if (index >= MAX_PONDER_OUTLINES) {
                break;
            }
            Object key = ponderOutlineKey(projectorPos, index);
            outliner.showOutline(key, new PonderNoDepthVeinOutline(projectorPos, facing, vein.type(), vein.blocks()))
                .lineWidth(0.05F)
                .disableCull();
            outliner.keep(key);
            index++;
        }
        for (int i = index; i < MAX_PONDER_OUTLINES; i++) {
            outliner.remove(ponderOutlineKey(projectorPos, i));
        }
    }

    private static void clearPonderOutlines(PonderLevel level, BlockPos projectorPos) {
        Outliner outliner = resolvePonderOutliner(level);
        if (outliner == null) {
            return;
        }
        for (int i = 0; i < MAX_PONDER_OUTLINES; i++) {
            outliner.remove(ponderOutlineKey(projectorPos, i));
        }
    }

    private static Outliner resolvePonderOutliner(PonderLevel level) {
        if (level == null || level.scene == null) {
            return null;
        }
        return level.scene.getOutliner();
    }

    private static Object ponderOutlineKey(BlockPos projectorPos, int index) {
        return "gr_ponder_projector_" + projectorPos.asLong() + "_" + index;
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
            // Mining must shrink the hologram without changing its distance priority.
            float[] centroid = veinCentroid(origin, vein.surveyedBlocks());
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
            case CRIMSITE -> state.is(SeismicMineralTags.CRIMSITE);
            case ASURINE -> state.is(SeismicMineralTags.ASURINE);
            case OCHRUM -> state.is(SeismicMineralTags.OCHRUM);
            case VERIDIUM -> state.is(SeismicMineralTags.VERIDIUM);
            case THORIUM -> state.is(SeismicMineralTags.THORIUM);
            case MAGNETITE -> state.is(SeismicMineralTags.MAGNETITE);
            case LEAD -> state.is(SeismicMineralTags.LEAD);
            case NICKEL -> state.is(SeismicMineralTags.NICKEL);
            case LITHIUM -> state.is(SeismicMineralTags.LITHIUM);
            case BAUXITE -> state.is(SeismicMineralTags.BAUXITE);
            case GALENA -> state.is(SeismicMineralTags.GALENA);
            case LIGNITE -> state.is(SeismicMineralTags.LIGNITE);
            case SULFUR -> state.is(SeismicMineralTags.SULFUR);
            case WOLFRAMITE -> state.is(SeismicMineralTags.WOLFRAMITE);
            case URANIUM -> state.is(SeismicMineralTags.URANIUM);
            case AUTUNITE -> state.is(SeismicMineralTags.AUTUNITE);
            case NITRATE -> state.is(SeismicMineralTags.NITRATE);
            case QUARTZ -> state.is(Blocks.NETHER_QUARTZ_ORE);
            case ANCIENT_DEBRIS -> state.is(Blocks.ANCIENT_DEBRIS);
            case TIN -> state.is(SeismicMineralTags.TIN);
            case OSMIUM -> state.is(SeismicMineralTags.OSMIUM);
            case FLUORITE -> state.is(SeismicMineralTags.FLUORITE);
            case CERTUS_QUARTZ -> state.is(SeismicMineralTags.CERTUS_QUARTZ);
            case INFERIUM -> state.is(SeismicMineralTags.INFERIUM);
            case PROSPERITY -> state.is(SeismicMineralTags.PROSPERITY);
            case SOULIUM -> state.is(SeismicMineralTags.SOULIUM);
            case ARCANE_CRYSTAL -> state.is(SeismicMineralTags.ARCANE_CRYSTAL);
            case RUNE -> state.is(SeismicMineralTags.RUNE);
            case STELLARITE -> state.is(SeismicMineralTags.STELLARITE);
            case URANINITE -> state.is(SeismicMineralTags.URANINITE);
            case DRACONIUM -> state.is(SeismicMineralTags.DRACONIUM);
            case BLACK_QUARTZ -> state.is(SeismicMineralTags.BLACK_QUARTZ);
            case SILVER -> state.is(SeismicMineralTags.SILVER);
            case PLATINUM -> state.is(SeismicMineralTags.PLATINUM);
            case COBALT, COAL_DEPOSIT, IRON_DEPOSIT, COPPER_DEPOSIT,
                GOLD_DEPOSIT, REDSTONE_DEPOSIT, LAPIS_DEPOSIT, ZINC_DEPOSIT, LEAD_DEPOSIT, NICKEL_DEPOSIT,
                URANIUM_DEPOSIT, THORIUM_DEPOSIT, WOLFRAMITE_DEPOSIT, TIN_DEPOSIT, SILVER_DEPOSIT, PLATINUM_DEPOSIT,
                OSMIUM_DEPOSIT, QUARTZ_DEPOSIT, COBALT_DEPOSIT ->
                SeismicRockStoneDeposits.matches(state, type);
            case CRUDE_OIL -> state.is(SeismicMineralTags.CRUDE_OIL)
                || state.getFluidState().is(SeismicFluidTags.CRUDE_OIL);
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

    private static List<LineSegment> buildJoinedEdges(List<FaceQuad> faces) {
        Map<FloatFaceEdgeKey, Integer> orientedCounts = new HashMap<>();
        for (FaceQuad face : faces) {
            Direction normal = face.normal();
            addFaceEdge(orientedCounts, normal, face.x1(), face.y1(), face.z1(), face.x2(), face.y2(), face.z2());
            addFaceEdge(orientedCounts, normal, face.x2(), face.y2(), face.z2(), face.x3(), face.y3(), face.z3());
            addFaceEdge(orientedCounts, normal, face.x3(), face.y3(), face.z3(), face.x4(), face.y4(), face.z4());
            addFaceEdge(orientedCounts, normal, face.x4(), face.y4(), face.z4(), face.x1(), face.y1(), face.z1());
        }

        Set<FloatEdgeKey> deduped = new HashSet<>();
        List<LineSegment> joined = new ArrayList<>();
        for (Map.Entry<FloatFaceEdgeKey, Integer> entry : orientedCounts.entrySet()) {
            if ((entry.getValue() & 1) == 0) {
                continue;
            }
            FloatEdgeKey edge = entry.getKey().edge();
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

    private static void addFaceEdge(Map<FloatFaceEdgeKey, Integer> counts, Direction normal,
                                    float x1, float y1, float z1, float x2, float y2, float z2) {
        FloatEdgeKey edge = FloatEdgeKey.of(x1, y1, z1, x2, y2, z2);
        FloatFaceEdgeKey key = new FloatFaceEdgeKey(normal, edge);
        counts.merge(key, 1, Integer::sum);
    }

    private static List<FaceQuad> buildSurfaceFaces(List<BlockPos> blocks, BlockPos origin, float inset) {
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
                addFaceQuad(faces, x, y, z, direction, inset);
            }
        }
        return faces;
    }

    private static void addFaceQuad(List<FaceQuad> faces, int x, int y, int z, Direction direction, float inset) {
        float minX = x + inset;
        float minY = y + inset;
        float minZ = z + inset;
        float maxX = x + 1.0F - inset;
        float maxY = y + 1.0F - inset;
        float maxZ = z + 1.0F - inset;

        switch (direction) {
            case DOWN -> faces.add(new FaceQuad(Direction.DOWN, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ));
            case UP -> faces.add(new FaceQuad(Direction.UP, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ));
            case NORTH -> faces.add(new FaceQuad(Direction.NORTH, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ));
            case SOUTH -> faces.add(new FaceQuad(Direction.SOUTH, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ));
            case WEST -> faces.add(new FaceQuad(Direction.WEST, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ));
            case EAST -> faces.add(new FaceQuad(Direction.EAST, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ));
        }
    }

    private static AABB toLocalCandidateBox(BlockPos origin, SeismicProjectorData.TriangulatedCandidate candidate) {
        double centerX = candidate.worldX() + 0.5D - origin.getX();
        double centerY = candidate.approxY() + 0.5D - origin.getY();
        double centerZ = candidate.worldZ() + 0.5D - origin.getZ();
        return new AABB(
            centerX - BOX_SIZE, centerY - BOX_SIZE, centerZ - BOX_SIZE,
            centerX + BOX_SIZE, centerY + BOX_SIZE, centerZ + BOX_SIZE
        );
    }

    private static AABB toWorldCandidateBox(SeismicProjectorData.TriangulatedCandidate candidate) {
        double centerX = candidate.worldX() + 0.5D;
        double centerY = candidate.approxY() + 0.5D;
        double centerZ = candidate.worldZ() + 0.5D;
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

        addQuad(builder, matrix, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, r, g, b, a);
        addQuad(builder, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ, r, g, b, a);
        addQuad(builder, matrix, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ, r, g, b, a);
        addQuad(builder, matrix, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, b, a);
        addQuad(builder, matrix, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, b, a);
        addQuad(builder, matrix, minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
    }

    private static void addQuad(BufferBuilder builder, Matrix4f matrix,
                                float x1, float y1, float z1, float x2, float y2, float z2,
                                float x3, float y3, float z3, float x4, float y4, float z4,
                                float r, float g, float b, float a) {
        builder.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
        builder.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
        builder.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a);
        builder.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a);
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

    private static void drawMountedBoxFaces(BufferBuilder builder, Matrix4f matrix, MovementContext context, AABB box,
                                            float r, float g, float b, float a) {
        Vec3 p000 = mountedTargetLocal(context, (float) box.minX, (float) box.minY, (float) box.minZ);
        Vec3 p100 = mountedTargetLocal(context, (float) box.maxX, (float) box.minY, (float) box.minZ);
        Vec3 p110 = mountedTargetLocal(context, (float) box.maxX, (float) box.maxY, (float) box.minZ);
        Vec3 p010 = mountedTargetLocal(context, (float) box.minX, (float) box.maxY, (float) box.minZ);
        Vec3 p001 = mountedTargetLocal(context, (float) box.minX, (float) box.minY, (float) box.maxZ);
        Vec3 p101 = mountedTargetLocal(context, (float) box.maxX, (float) box.minY, (float) box.maxZ);
        Vec3 p111 = mountedTargetLocal(context, (float) box.maxX, (float) box.maxY, (float) box.maxZ);
        Vec3 p011 = mountedTargetLocal(context, (float) box.minX, (float) box.maxY, (float) box.maxZ);

        addQuad(builder, matrix, (float) p000.x, (float) p000.y, (float) p000.z, (float) p010.x, (float) p010.y, (float) p010.z,
            (float) p110.x, (float) p110.y, (float) p110.z, (float) p100.x, (float) p100.y, (float) p100.z, r, g, b, a);
        addQuad(builder, matrix, (float) p101.x, (float) p101.y, (float) p101.z, (float) p111.x, (float) p111.y, (float) p111.z,
            (float) p011.x, (float) p011.y, (float) p011.z, (float) p001.x, (float) p001.y, (float) p001.z, r, g, b, a);
        addQuad(builder, matrix, (float) p001.x, (float) p001.y, (float) p001.z, (float) p011.x, (float) p011.y, (float) p011.z,
            (float) p010.x, (float) p010.y, (float) p010.z, (float) p000.x, (float) p000.y, (float) p000.z, r, g, b, a);
        addQuad(builder, matrix, (float) p100.x, (float) p100.y, (float) p100.z, (float) p110.x, (float) p110.y, (float) p110.z,
            (float) p111.x, (float) p111.y, (float) p111.z, (float) p101.x, (float) p101.y, (float) p101.z, r, g, b, a);
        addQuad(builder, matrix, (float) p010.x, (float) p010.y, (float) p010.z, (float) p011.x, (float) p011.y, (float) p011.z,
            (float) p111.x, (float) p111.y, (float) p111.z, (float) p110.x, (float) p110.y, (float) p110.z, r, g, b, a);
        addQuad(builder, matrix, (float) p001.x, (float) p001.y, (float) p001.z, (float) p000.x, (float) p000.y, (float) p000.z,
            (float) p100.x, (float) p100.y, (float) p100.z, (float) p101.x, (float) p101.y, (float) p101.z, r, g, b, a);
    }

    private static void drawMountedBoxEdges(BufferBuilder builder, Matrix4f matrix, MovementContext context, AABB box,
                                            float r, float g, float b, float a) {
        Vec3 p000 = mountedTargetLocal(context, (float) box.minX, (float) box.minY, (float) box.minZ);
        Vec3 p100 = mountedTargetLocal(context, (float) box.maxX, (float) box.minY, (float) box.minZ);
        Vec3 p110 = mountedTargetLocal(context, (float) box.maxX, (float) box.maxY, (float) box.minZ);
        Vec3 p010 = mountedTargetLocal(context, (float) box.minX, (float) box.maxY, (float) box.minZ);
        Vec3 p001 = mountedTargetLocal(context, (float) box.minX, (float) box.minY, (float) box.maxZ);
        Vec3 p101 = mountedTargetLocal(context, (float) box.maxX, (float) box.minY, (float) box.maxZ);
        Vec3 p111 = mountedTargetLocal(context, (float) box.maxX, (float) box.maxY, (float) box.maxZ);
        Vec3 p011 = mountedTargetLocal(context, (float) box.minX, (float) box.maxY, (float) box.maxZ);

        addEdge(builder, matrix, (float) p000.x, (float) p000.y, (float) p000.z, (float) p100.x, (float) p100.y, (float) p100.z, r, g, b, a);
        addEdge(builder, matrix, (float) p100.x, (float) p100.y, (float) p100.z, (float) p101.x, (float) p101.y, (float) p101.z, r, g, b, a);
        addEdge(builder, matrix, (float) p101.x, (float) p101.y, (float) p101.z, (float) p001.x, (float) p001.y, (float) p001.z, r, g, b, a);
        addEdge(builder, matrix, (float) p001.x, (float) p001.y, (float) p001.z, (float) p000.x, (float) p000.y, (float) p000.z, r, g, b, a);

        addEdge(builder, matrix, (float) p010.x, (float) p010.y, (float) p010.z, (float) p110.x, (float) p110.y, (float) p110.z, r, g, b, a);
        addEdge(builder, matrix, (float) p110.x, (float) p110.y, (float) p110.z, (float) p111.x, (float) p111.y, (float) p111.z, r, g, b, a);
        addEdge(builder, matrix, (float) p111.x, (float) p111.y, (float) p111.z, (float) p011.x, (float) p011.y, (float) p011.z, r, g, b, a);
        addEdge(builder, matrix, (float) p011.x, (float) p011.y, (float) p011.z, (float) p010.x, (float) p010.y, (float) p010.z, r, g, b, a);

        addEdge(builder, matrix, (float) p000.x, (float) p000.y, (float) p000.z, (float) p010.x, (float) p010.y, (float) p010.z, r, g, b, a);
        addEdge(builder, matrix, (float) p100.x, (float) p100.y, (float) p100.z, (float) p110.x, (float) p110.y, (float) p110.z, r, g, b, a);
        addEdge(builder, matrix, (float) p101.x, (float) p101.y, (float) p101.z, (float) p111.x, (float) p111.y, (float) p111.z, r, g, b, a);
        addEdge(builder, matrix, (float) p001.x, (float) p001.y, (float) p001.z, (float) p011.x, (float) p011.y, (float) p011.z, r, g, b, a);
    }

    private static void addEdge(BufferBuilder builder, Matrix4f matrix,
                                float x1, float y1, float z1, float x2, float y2, float z2,
                                float r, float g, float b, float a) {
        builder.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
        builder.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
    }

    private static void drawDashedLineThick(BufferBuilder builder, Matrix4f matrix,
                                            float x1, float y1, float z1, float x2, float y2, float z2,
                                            float r, float g, float b, float a, float halfWidth) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float distance = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance <= 0.001F || halfWidth <= 0.0F) {
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
            drawPrismSegment(builder, matrix, ax, ay, az, bx, by, bz, halfWidth, r, g, b, a);
            cursor = dashEnd + DASH_GAP;
        }
    }

    private static void drawPrismSegment(BufferBuilder builder, Matrix4f matrix,
                                         float ax, float ay, float az, float bx, float by, float bz,
                                         float halfWidth, float r, float g, float b, float a) {
        float dx = bx - ax;
        float dy = by - ay;
        float dz = bz - az;
        float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (length <= 0.001F) {
            return;
        }
        float inv = 1.0F / length;
        float nx = dx * inv;
        float ny = dy * inv;
        float nz = dz * inv;

        float rx = 0.0F;
        float ry = 1.0F;
        float rz = 0.0F;
        if (Math.abs(ny) > 0.99F) {
            rx = 1.0F;
            ry = 0.0F;
            rz = 0.0F;
        }

        float rightX = ny * rz - nz * ry;
        float rightY = nz * rx - nx * rz;
        float rightZ = nx * ry - ny * rx;
        float rightLen = Mth.sqrt(rightX * rightX + rightY * rightY + rightZ * rightZ);
        if (rightLen <= 0.001F) {
            return;
        }
        float rightInv = halfWidth / rightLen;
        rightX *= rightInv;
        rightY *= rightInv;
        rightZ *= rightInv;

        float upX = rightY * nz - rightZ * ny;
        float upY = rightZ * nx - rightX * nz;
        float upZ = rightX * ny - rightY * nx;
        float upLen = Mth.sqrt(upX * upX + upY * upY + upZ * upZ);
        if (upLen <= 0.001F) {
            return;
        }
        float upInv = halfWidth / upLen;
        upX *= upInv;
        upY *= upInv;
        upZ *= upInv;

        float a1x = ax + rightX + upX;
        float a1y = ay + rightY + upY;
        float a1z = az + rightZ + upZ;
        float a2x = ax - rightX + upX;
        float a2y = ay - rightY + upY;
        float a2z = az - rightZ + upZ;
        float a3x = ax - rightX - upX;
        float a3y = ay - rightY - upY;
        float a3z = az - rightZ - upZ;
        float a4x = ax + rightX - upX;
        float a4y = ay + rightY - upY;
        float a4z = az + rightZ - upZ;

        float b1x = bx + rightX + upX;
        float b1y = by + rightY + upY;
        float b1z = bz + rightZ + upZ;
        float b2x = bx - rightX + upX;
        float b2y = by - rightY + upY;
        float b2z = bz - rightZ + upZ;
        float b3x = bx - rightX - upX;
        float b3y = by - rightY - upY;
        float b3z = bz - rightZ - upZ;
        float b4x = bx + rightX - upX;
        float b4y = by + rightY - upY;
        float b4z = bz + rightZ - upZ;

        addQuad(builder, matrix, a1x, a1y, a1z, a2x, a2y, a2z, b2x, b2y, b2z, b1x, b1y, b1z, r, g, b, a);
        addQuad(builder, matrix, a2x, a2y, a2z, a3x, a3y, a3z, b3x, b3y, b3z, b2x, b2y, b2z, r, g, b, a);
        addQuad(builder, matrix, a3x, a3y, a3z, a4x, a4y, a4z, b4x, b4y, b4z, b3x, b3y, b3z, r, g, b, a);
        addQuad(builder, matrix, a4x, a4y, a4z, a1x, a1y, a1z, b1x, b1y, b1z, b4x, b4y, b4z, r, g, b, a);
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

    private static float[] guideStartForFacing(Direction facing) {
        float startX = GUIDE_CENTER_X + facing.getStepX() * GUIDE_FORWARD_OFFSET;
        float startY = GUIDE_CENTER_Y;
        float startZ = GUIDE_CENTER_Z + facing.getStepZ() * GUIDE_FORWARD_OFFSET;
        return new float[] {startX, startY, startZ};
    }

    private static Vec3 mountedProjectorCenter(MovementContext context) {
        if (context.contraption != null && context.contraption.entity != null) {
            return context.contraption.entity.toGlobalVector(
                Vec3.atCenterOf(context.localPos),
                AnimationTickHolder.getPartialTicks()
            );
        }
        if (context.position != null) {
            return context.position;
        }
        return Vec3.atCenterOf(context.localPos);
    }

    private static Vec3 mountedTargetLocal(MovementContext context, float worldX, float worldY, float worldZ) {
        Vec3 worldTarget = new Vec3(worldX, worldY, worldZ);
        if (context.contraption != null && context.contraption.entity != null) {
            return context.contraption.entity.toLocalVector(worldTarget, AnimationTickHolder.getPartialTicks());
        }
        return worldTarget;
    }

    private static float projectorBlockInset() {
        return Mth.clamp(Config.PROJECTOR_BLOCK_EDGE_INSET.get().floatValue(), -0.45F, 0.45F);
    }

    private static float projectorGuideLineWidth() {
        return Mth.clamp(Config.PROJECTOR_GUIDE_LINE_WIDTH.get().floatValue(), 0.2F, 1.0F);
    }

    private static float guideLineHalfWidth(float configuredWidth) {
        return configuredWidth * 0.0125F;
    }

    private static RenderGeometryData resolveRenderGeometry(Object cacheKey, BlockPos origin,
                                                            List<RenderableVein> visibleVeins,
                                                            List<SeismicProjectorData.TriangulatedCandidate> candidates,
                                                            boolean renderExact, float exactBlockInset) {
        if (GEOMETRY_CACHE.size() > MAX_GEOMETRY_CACHE_ENTRIES) {
            GEOMETRY_CACHE.clear();
        }
        long signature = geometrySignature(renderExact, exactBlockInset, visibleVeins, candidates);
        GeometryCacheEntry cached = GEOMETRY_CACHE.get(cacheKey);
        if (cached != null && cached.signature() == signature) {
            return cached.data();
        }

        List<ExactVeinGeometry> exactGeometries = List.of();
        List<CandidateGeometry> candidateGeometries = List.of();
        if (renderExact) {
            List<ExactVeinGeometry> built = new ArrayList<>(visibleVeins.size());
            for (RenderableVein vein : visibleVeins) {
                List<FaceQuad> faces = buildSurfaceFaces(vein.blocks(), origin, exactBlockInset);
                List<LineSegment> edges = buildJoinedEdges(faces);
                float[] centroid = veinCentroid(origin, vein.blocks());
                built.add(new ExactVeinGeometry(vein.type(), List.copyOf(faces), List.copyOf(edges),
                    centroid[0], centroid[1], centroid[2]));
            }
            exactGeometries = List.copyOf(built);
        } else {
            List<CandidateGeometry> built = new ArrayList<>(candidates.size());
            for (SeismicProjectorData.TriangulatedCandidate candidate : candidates) {
                AABB box = toLocalCandidateBox(origin, candidate);
                built.add(new CandidateGeometry(
                    candidate.type(),
                    box,
                    (float) ((box.minX + box.maxX) * 0.5D),
                    (float) ((box.minY + box.maxY) * 0.5D),
                    (float) ((box.minZ + box.maxZ) * 0.5D)
                ));
            }
            candidateGeometries = List.copyOf(built);
        }

        RenderGeometryData data = new RenderGeometryData(exactGeometries, candidateGeometries);
        GEOMETRY_CACHE.put(cacheKey, new GeometryCacheEntry(signature, data));
        return data;
    }

    private static RenderGeometryData resolveWorldRenderGeometry(Object cacheKey,
                                                                 List<RenderableVein> visibleVeins,
                                                                 List<SeismicProjectorData.TriangulatedCandidate> candidates,
                                                                 boolean renderExact, float exactBlockInset) {
        if (GEOMETRY_CACHE.size() > MAX_GEOMETRY_CACHE_ENTRIES) {
            GEOMETRY_CACHE.clear();
        }
        long signature = geometrySignature(renderExact, exactBlockInset, visibleVeins, candidates);
        GeometryCacheEntry cached = GEOMETRY_CACHE.get(cacheKey);
        if (cached != null && cached.signature() == signature) {
            return cached.data();
        }

        List<ExactVeinGeometry> exactGeometries = List.of();
        List<CandidateGeometry> candidateGeometries = List.of();
        if (renderExact) {
            List<ExactVeinGeometry> built = new ArrayList<>(visibleVeins.size());
            for (RenderableVein vein : visibleVeins) {
                List<FaceQuad> faces = buildSurfaceFaces(vein.blocks(), BlockPos.ZERO, exactBlockInset);
                List<LineSegment> edges = buildJoinedEdges(faces);
                float[] centroid = veinCentroid(BlockPos.ZERO, vein.blocks());
                built.add(new ExactVeinGeometry(vein.type(), List.copyOf(faces), List.copyOf(edges),
                    centroid[0], centroid[1], centroid[2]));
            }
            exactGeometries = List.copyOf(built);
        } else {
            List<CandidateGeometry> built = new ArrayList<>(candidates.size());
            for (SeismicProjectorData.TriangulatedCandidate candidate : candidates) {
                AABB box = toWorldCandidateBox(candidate);
                built.add(new CandidateGeometry(
                    candidate.type(),
                    box,
                    (float) ((box.minX + box.maxX) * 0.5D),
                    (float) ((box.minY + box.maxY) * 0.5D),
                    (float) ((box.minZ + box.maxZ) * 0.5D)
                ));
            }
            candidateGeometries = List.copyOf(built);
        }

        RenderGeometryData data = new RenderGeometryData(exactGeometries, candidateGeometries);
        GEOMETRY_CACHE.put(cacheKey, new GeometryCacheEntry(signature, data));
        return data;
    }

    private static long geometrySignature(boolean renderExact, float exactBlockInset,
                                          List<RenderableVein> visibleVeins,
                                          List<SeismicProjectorData.TriangulatedCandidate> candidates) {
        long hash = 17L;
        hash = 31L * hash + (renderExact ? 1L : 0L);
        hash = 31L * hash + Float.floatToIntBits(exactBlockInset);
        hash = 31L * hash + visibleVeins.size();
        for (RenderableVein vein : visibleVeins) {
            hash = 31L * hash + vein.type().ordinal();
            hash = 31L * hash + vein.blocks().size();
            for (BlockPos blockPos : vein.blocks()) {
                hash = 31L * hash + blockPos.asLong();
            }
        }
        hash = 31L * hash + candidates.size();
        for (SeismicProjectorData.TriangulatedCandidate candidate : candidates) {
            hash = 31L * hash + candidate.type().ordinal();
            hash = 31L * hash + candidate.worldX();
            hash = 31L * hash + candidate.worldZ();
            hash = 31L * hash + candidate.approxY();
            hash = 31L * hash + candidate.error();
        }
        return hash;
    }

    private static float ponderBlockInset() {
        return Mth.clamp(Config.PROJECTOR_PONDER_BLOCK_EDGE_INSET.get().floatValue(), -0.45F, 0.45F);
    }

    private static float ponderGuideLineWidth() {
        return Mth.clamp(Config.PROJECTOR_PONDER_GUIDE_LINE_WIDTH.get().floatValue(), 0.2F, 1.0F);
    }

    private static final class PonderNoDepthVeinOutline extends Outline {
        private final BlockPos projectorPos;
        private final Direction facing;
        private final SeismicAnomalyType type;
        private final List<BlockPos> blocks;

        private PonderNoDepthVeinOutline(BlockPos projectorPos, Direction facing, SeismicAnomalyType type, List<BlockPos> blocks) {
            this.projectorPos = projectorPos.immutable();
            this.facing = facing;
            this.type = type;
            this.blocks = List.copyOf(blocks);
        }

        @Override
        public void render(PoseStack poseStack, net.createmod.catnip.render.SuperRenderTypeBuffer buffer, Vec3 camera, float partialTicks) {
            if (blocks.isEmpty()) {
                return;
            }
            float[] color = colorFor(type);
            float fillAlpha = Mth.clamp(Config.PROJECTOR_FILL_ALPHA.get() / 255.0F, 0.0F, 1.0F);
            float edgeAlpha = Mth.clamp(Config.PROJECTOR_EDGE_ALPHA.get() / 255.0F, 0.0F, 1.0F);
            float inset = ponderBlockInset();
            List<FaceQuad> surfaceFaces = buildSurfaceFaces(blocks, projectorPos, inset);
            List<LineSegment> surfaceEdges = buildJoinedEdges(surfaceFaces);

            poseStack.pushPose();
            poseStack.translate(
                projectorPos.getX() - camera.x,
                projectorPos.getY() - camera.y,
                projectorPos.getZ() - camera.z
            );
            Matrix4f matrix = poseStack.last().pose();

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            // Cull only filled surfaces; outlines and guide beams stay two-sided.
            RenderSystem.enableCull();

            try {
                if (Config.PROJECTOR_FILL_ENABLED.get() && fillAlpha > 0.0F) {
                    Tesselator tesselator = Tesselator.getInstance();
                    BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                    RenderSystem.setShader(GameRenderer::getPositionColorShader);
                    for (FaceQuad face : surfaceFaces) {
                        addQuad(builder, matrix,
                            face.x1(), face.y1(), face.z1(),
                            face.x2(), face.y2(), face.z2(),
                            face.x3(), face.y3(), face.z3(),
                            face.x4(), face.y4(), face.z4(),
                            color[0], color[1], color[2], fillAlpha);
                    }
                    BufferUploader.drawWithShader(builder.buildOrThrow());
                }

                RenderSystem.disableCull();

                if (edgeAlpha > 0.0F) {
                    Tesselator tesselator = Tesselator.getInstance();
                    BufferBuilder builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
                    RenderSystem.setShader(GameRenderer::getPositionColorShader);
                    for (LineSegment edge : surfaceEdges) {
                        addEdge(builder, matrix,
                            edge.x1(), edge.y1(), edge.z1(),
                            edge.x2(), edge.y2(), edge.z2(),
                            color[0], color[1], color[2], edgeAlpha);
                    }
                    BufferUploader.drawWithShader(builder.buildOrThrow());
                }

                if (Config.PROJECTOR_GUIDE_LINES_ENABLED.get() && edgeAlpha > 0.0F) {
                    Tesselator tesselator = Tesselator.getInstance();
                    BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                    RenderSystem.setShader(GameRenderer::getPositionColorShader);
                    float[] guideStart = guideStartForFacing(facing);
                    float guideHalfWidth = guideLineHalfWidth(ponderGuideLineWidth());
                    float guideAlpha = edgeAlpha * EXACT_GUIDE_ALPHA_SCALE;
                    for (RenderableVein vein : List.of(new RenderableVein(type, blocks))) {
                        float[] centroid = veinCentroid(projectorPos, vein.blocks());
                        drawDashedLineThick(builder, matrix,
                            guideStart[0], guideStart[1], guideStart[2],
                            centroid[0], centroid[1], centroid[2],
                            color[0], color[1], color[2], guideAlpha, guideHalfWidth);
                    }
                    BufferUploader.drawWithShader(builder.buildOrThrow());
                }

            } finally {
                RenderSystem.depthMask(true);
                RenderSystem.enableDepthTest();
                RenderSystem.enableCull();
                RenderSystem.disableBlend();
            }

            poseStack.popPose();
        }
    }

    private static float[] colorFor(SeismicAnomalyType type) {
        return switch (type) {
            case WATER -> rgb(80, 170, 255);
            case LAVA -> rgb(255, 120, 40);
            case CAVE -> rgb(180, 200, 220);
            case COAL, COAL_DEPOSIT -> rgb(70, 70, 70);
            case IRON, IRON_DEPOSIT -> rgb(216, 166, 129);
            case COPPER, COPPER_DEPOSIT -> rgb(211, 132, 75);
            case GOLD, GOLD_DEPOSIT -> rgb(255, 215, 61);
            case REDSTONE, REDSTONE_DEPOSIT -> rgb(229, 49, 49);
            case LAPIS, LAPIS_DEPOSIT -> rgb(66, 108, 208);
            case EMERALD -> rgb(49, 210, 113);
            case DIAMOND -> rgb(87, 238, 220);
            case ZINC, ZINC_DEPOSIT -> rgb(156, 190, 205);
            case CRIMSITE -> rgb(184, 92, 112);
            case ASURINE -> rgb(102, 159, 208);
            case OCHRUM -> rgb(217, 172, 85);
            case VERIDIUM -> rgb(121, 184, 131);
            case THORIUM, THORIUM_DEPOSIT -> rgb(198, 216, 107);
            case MAGNETITE -> rgb(116, 126, 138);
            case LEAD, LEAD_DEPOSIT -> rgb(132, 140, 184);
            case NICKEL, NICKEL_DEPOSIT -> rgb(175, 195, 141);
            case LITHIUM -> rgb(216, 150, 182);
            case BAUXITE -> rgb(209, 139, 91);
            case GALENA -> rgb(158, 123, 184);
            case LIGNITE -> rgb(130, 96, 70);
            case SULFUR -> rgb(223, 216, 90);
            case CRUDE_OIL -> rgb(104, 78, 125);
            case WOLFRAMITE, WOLFRAMITE_DEPOSIT -> rgb(144, 169, 191);
            case URANIUM, URANIUM_DEPOSIT -> rgb(100, 206, 103);
            case AUTUNITE -> rgb(173, 204, 87);
            case NITRATE -> rgb(220, 230, 240);
            case TIN, TIN_DEPOSIT -> rgb(166, 196, 190);
            case SILVER, SILVER_DEPOSIT -> rgb(202, 220, 236);
            case PLATINUM, PLATINUM_DEPOSIT -> rgb(193, 187, 232);
            case OSMIUM, OSMIUM_DEPOSIT -> rgb(117, 146, 190);
            case QUARTZ, QUARTZ_DEPOSIT -> rgb(242, 231, 214);
            case ANCIENT_DEBRIS -> rgb(169, 127, 108);
            case FLUORITE -> rgb(174, 184, 239);
            case CERTUS_QUARTZ -> rgb(155, 205, 232);
            case INFERIUM -> rgb(131, 183, 77);
            case PROSPERITY -> rgb(217, 230, 233);
            case SOULIUM -> rgb(128, 102, 74);
            case ARCANE_CRYSTAL -> rgb(111, 148, 241);
            case RUNE -> rgb(180, 128, 217);
            case STELLARITE -> rgb(241, 196, 93);
            case URANINITE -> rgb(157, 190, 85);
            case DRACONIUM -> rgb(165, 124, 230);
            case BLACK_QUARTZ -> rgb(139, 147, 166);
            case COBALT, COBALT_DEPOSIT -> rgb(72, 109, 219);
            case AMETHYST -> rgb(192, 138, 230);
            case CHEST -> rgb(171, 119, 66);
            case SPAWNER -> rgb(96, 168, 198);
            case SOLID -> rgb(160, 160, 160);
        };
    }

    private static float[] rgb(int r, int g, int b) {
        return new float[] {r / 255.0F, g / 255.0F, b / 255.0F};
    }

    private static RenderType resolveRenderType(BlockState state) {
        BakedModel model = net.minecraft.client.Minecraft.getInstance()
            .getBlockRenderer()
            .getBlockModel(state);
        ChunkRenderTypeSet typeSet = model.getRenderTypes(state, net.minecraft.util.RandomSource.create(42L), ModelData.EMPTY);
        for (RenderType type : KineticBlockEntityRenderer.REVERSED_CHUNK_BUFFER_LAYERS) {
            if (typeSet.contains(type)) {
                return type;
            }
        }
        return RenderType.cutoutMipped();
    }

    private record FloatPoint(float x, float y, float z) {
    }

    private record FloatEdgeKey(FloatPoint a, FloatPoint b) {
        private static FloatEdgeKey of(float x1, float y1, float z1, float x2, float y2, float z2) {
            FloatPoint p1 = new FloatPoint(x1, y1, z1);
            FloatPoint p2 = new FloatPoint(x2, y2, z2);
            if (compare(p1, p2) <= 0) {
                return new FloatEdgeKey(p1, p2);
            }
            return new FloatEdgeKey(p2, p1);
        }

        private static int compare(FloatPoint left, FloatPoint right) {
            int x = Float.compare(left.x(), right.x());
            if (x != 0) {
                return x;
            }
            int y = Float.compare(left.y(), right.y());
            if (y != 0) {
                return y;
            }
            return Float.compare(left.z(), right.z());
        }
    }

    private record FloatFaceEdgeKey(Direction normal, FloatEdgeKey edge) {
    }

    private record LineSegment(float x1, float y1, float z1, float x2, float y2, float z2) {
    }

    private record FaceQuad(
        Direction normal,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,
        float x4, float y4, float z4) {
    }

    private record RenderableVein(SeismicAnomalyType type, List<BlockPos> blocks, List<BlockPos> surveyedBlocks) {
        private RenderableVein(SeismicAnomalyType type, List<BlockPos> blocks) {
            this(type, blocks, blocks);
        }
    }

    private record RenderableVeinDistance(RenderableVein vein, float distanceSq) {
    }

    private record ExactVeinGeometry(SeismicAnomalyType type, List<FaceQuad> faces, List<LineSegment> edges,
                                     float centroidX, float centroidY, float centroidZ) {
    }

    private record CandidateGeometry(SeismicAnomalyType type, AABB box, float centerX, float centerY, float centerZ) {
    }

    private record RenderGeometryData(List<ExactVeinGeometry> exactVeins, List<CandidateGeometry> candidates) {
    }

    private record GeometryCacheEntry(long signature, RenderGeometryData data) {
    }

    private static float shaftAngle(SeismicProjectorBlockEntity blockEntity, Direction facing) {
        if (blockEntity.getLevel() == null) {
            return 0.0F;
        }
        float sourceAngle = sourceShaftAngle(blockEntity, facing);
        Direction shaftSide = facing.getOpposite();
        float sideSign = shaftSide.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0F : -1.0F;
        return sourceAngle * sideSign;
    }

    private static float sourceShaftAngle(SeismicProjectorBlockEntity blockEntity, Direction facing) {
        Direction.Axis projectorAxis = KineticBlockEntityRenderer.getRotationAxisOf(blockEntity);
        BlockPos sourcePos = blockEntity.getBlockPos().relative(facing.getOpposite());
        if (blockEntity.getLevel() != null && blockEntity.getLevel().getBlockEntity(sourcePos) instanceof KineticBlockEntity source) {
            Direction.Axis sourceAxis = KineticBlockEntityRenderer.getRotationAxisOf(source);
            if (sourceAxis == projectorAxis) {
                return KineticBlockEntityRenderer.getAngleForBe(source, sourcePos, sourceAxis);
            }
        }

        // Fallback when no kinetic source BE is present (e.g. some ponder/runtime states).
        return KineticBlockEntityRenderer.getAngleForBe(blockEntity, blockEntity.getBlockPos(), projectorAxis);
    }

    private static float mountedShaftAngle(MovementContext context, Direction facing) {
        Direction.Axis projectorAxis = KineticBlockEntityVisual.rotationAxis(context.state);
        BlockPos anglePos = context.localPos;
        BlockState angleState = context.state;
        float speed = readMountedSpeed(context.blockEntityData);

        if (context.contraption != null) {
            BlockPos sourcePos = context.localPos.relative(facing.getOpposite());
            StructureTemplate.StructureBlockInfo sourceInfo = context.contraption.getBlocks().get(sourcePos);
            if (sourceInfo != null) {
                Direction.Axis sourceAxis = KineticBlockEntityVisual.rotationAxis(sourceInfo.state());
                if (sourceAxis == projectorAxis) {
                    anglePos = sourcePos;
                    angleState = sourceInfo.state();
                    speed = readMountedSpeed(sourceInfo.nbt(), speed);
                }
            }
        }

        float time = AnimationTickHolder.getRenderTime(context.world);
        float offset = KineticBlockEntityVisual.rotationOffset(angleState, projectorAxis, anglePos);
        float degrees = time * speed * 3.0F / 10.0F + offset;
        float angle = (degrees % 360.0F) / 180.0F * Mth.PI;
        Direction shaftSide = facing.getOpposite();
        float sideSign = shaftSide.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0F : -1.0F;
        return angle * sideSign;
    }

    private static float readMountedSpeed(CompoundTag tag) {
        return readMountedSpeed(tag, 0.0F);
    }

    private static float readMountedSpeed(CompoundTag tag, float fallback) {
        if (tag == null || !tag.contains("Speed")) {
            return fallback;
        }
        return tag.getFloat("Speed");
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
