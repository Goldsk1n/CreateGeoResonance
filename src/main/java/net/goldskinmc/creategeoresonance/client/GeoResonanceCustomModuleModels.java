package net.goldskinmc.creategeoresonance.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.goldskinmc.creategeoresonance.seismic.SeismicAnomalyType;
import net.goldskinmc.creategeoresonance.seismic.SeismicCustomModuleItem;
import net.goldskinmc.creategeoresonance.seismic.SeismicCustomModuleDefinition.IconSize;
import net.goldskinmc.creategeoresonance.seismic.SeismicCustomModules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.event.ModelEvent;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GeoResonanceCustomModuleModels {
    private static int creativeRevision = Integer.MIN_VALUE;

    private GeoResonanceCustomModuleModels() {
    }

    public static void register(IEventBus bus) {
        bus.addListener((ModelEvent.RegisterAdditional event) -> event.register(smallIconModel()));
        bus.addListener((ModelEvent.ModifyBakingResult event) -> {
            ModelResourceLocation id = new ModelResourceLocation(
                ResourceLocation.fromNamespaceAndPath("creategeoresonance", "custom_module"), "inventory");
            BakedModel base = event.getModels().get(id);
            BakedModel small = event.getModels().get(smallIconModel());
            if (base == null || small == null) {
                throw new IllegalStateException("Missing standard or small custom seismic module model");
            }
            event.getModels().put(id, new ModuleModel(base, small));
        });
    }

    private static ResourceLocation smallIconModel() {
        return ResourceLocation.fromNamespaceAndPath(
            "creategeoresonance", "item/custom_module_small_icon");
    }

    public static boolean refreshCreativeContents() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || creativeRevision == SeismicCustomModules.revision()) {
            return false;
        }
        creativeRevision = SeismicCustomModules.revision();
        var parameters = new CreativeModeTab.ItemDisplayParameters(minecraft.level.enabledFeatures(),
            minecraft.player.canUseGameMasterBlocks(), minecraft.level.registryAccess());
        BuiltInRegistries.CREATIVE_MODE_TAB.stream().filter(tab -> tab.getType() == CreativeModeTab.Type.CATEGORY)
            .forEach(tab -> tab.buildContents(parameters));
        BuiltInRegistries.CREATIVE_MODE_TAB.stream().filter(tab -> tab.getType() != CreativeModeTab.Type.CATEGORY)
            .forEach(tab -> tab.buildContents(parameters));
        return true;
    }

    private static final class ModuleModel extends BakedModelWrapper<BakedModel> {
        private final BakedModel smallIconModel;
        private final Map<String, BakedModel> variants = new HashMap<>();
        private int revision = Integer.MIN_VALUE;
        private final ItemOverrides overrides = new ItemOverrides() {
            @Override
            public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
                                      @Nullable LivingEntity entity, int seed) {
                if (revision != SeismicCustomModules.revision()) {
                    variants.clear();
                    revision = SeismicCustomModules.revision();
                }
                SeismicAnomalyType type = SeismicCustomModuleItem.target(stack);
                if (type == null || SeismicCustomModules.definition(type) == null) {
                    return originalModel;
                }
                return variants.computeIfAbsent(type.name(), key -> {
                    var definition = SeismicCustomModules.definition(type);
                    ItemStack icon = new ItemStack(BuiltInRegistries.ITEM.get(definition.icon()));
                    TextureAtlasSprite sprite = Minecraft.getInstance().getItemRenderer().getModel(icon, level, entity, seed).getParticleIcon();
                    BakedModel base = definition.iconSize() == IconSize.SMALL ? smallIconModel : originalModel;
                    return new IconModel(base, sprite);
                });
            }
        };

        private ModuleModel(BakedModel original, BakedModel smallIconModel) {
            super(original);
            this.smallIconModel = smallIconModel;
        }

        @Override
        public ItemOverrides getOverrides() {
            return overrides;
        }
    }

    private static final class IconModel extends BakedModelWrapper<BakedModel> {
        private final Map<Direction, List<BakedQuad>> faces = new HashMap<>();
        private final List<BakedQuad> general;

        private IconModel(BakedModel original, TextureAtlasSprite icon) {
            super(original);
            general = replaceIcon(original.getQuads(null, null, RandomSource.create(42)), icon);
            for (Direction face : Direction.values()) {
                faces.put(face, replaceIcon(original.getQuads(null, face, RandomSource.create(42)), icon));
            }
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
            return side == null ? general : faces.get(side);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                       ModelData data, @Nullable RenderType renderType) {
            return getQuads(state, side, random);
        }

        @Override
        public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
            originalModel.applyTransform(context, pose, leftHand);
            return this;
        }

        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            return List.of(this);
        }

        private static List<BakedQuad> replaceIcon(List<BakedQuad> quads, TextureAtlasSprite icon) {
            List<BakedQuad> result = new ArrayList<>(quads.size());
            for (BakedQuad quad : quads) {
                TextureAtlasSprite source = quad.getSprite();
                if (!source.contents().name().toString().equals("minecraft:item/diamond")) {
                    result.add(quad);
                    continue;
                }
                int[] vertices = quad.getVertices().clone();
                int stride = vertices.length / 4;
                for (int vertex = 0; vertex < 4; vertex++) {
                    int offset = vertex * stride;
                    float u = Float.intBitsToFloat(vertices[offset + 4]);
                    float v = Float.intBitsToFloat(vertices[offset + 5]);
                    float mappedU = icon.getU0() + (u - source.getU0()) / (source.getU1() - source.getU0()) * (icon.getU1() - icon.getU0());
                    float mappedV = icon.getV0() + (v - source.getV0()) / (source.getV1() - source.getV0()) * (icon.getV1() - icon.getV0());
                    vertices[offset + 4] = Float.floatToRawIntBits(mappedU);
                    vertices[offset + 5] = Float.floatToRawIntBits(mappedV);
                }
                result.add(new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), icon, quad.isShade()));
            }
            return List.copyOf(result);
        }
    }
}
