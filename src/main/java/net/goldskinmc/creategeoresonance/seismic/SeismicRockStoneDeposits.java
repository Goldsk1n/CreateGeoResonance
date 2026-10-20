package net.goldskinmc.creategeoresonance.seismic;

import net.neoforged.fml.ModList;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SeismicRockStoneDeposits {
    private static final Map<SeismicAnomalyType, TagKey<Block>> TAGS = createTags();

    private SeismicRockStoneDeposits() {
    }

    private static Map<SeismicAnomalyType, TagKey<Block>> createTags() {
        Map<SeismicAnomalyType, TagKey<Block>> tags = new HashMap<>();
        for (SeismicAnomalyType type : SeismicAnomalyType.values()) {
            if (type.isRockStoneDeposit()) {
                String mineral = type.mineralType().name().toLowerCase(Locale.ROOT);
                tags.put(type, BlockTags.create(ResourceLocation.fromNamespaceAndPath(
                    CreateGeoResonanceMod.MODID, "deposits/rock_stone/" + mineral)));
            }
        }
        return Map.copyOf(tags);
    }

    public static Map<Block, SeismicAnomalyType> detectableBlocks(Set<SeismicAnomalyType> detectableTypes) {
        // Resolve tags once per scan, not per sampled block; rebuilding also respects datapack reloads.
        Map<Block, SeismicAnomalyType> targets = new HashMap<>();
        TAGS.forEach((type, tag) -> {
            if (!detectableTypes.contains(type) && !detectableTypes.contains(type.mineralType())) {
                return;
            }
            BuiltInRegistries.BLOCK.getTag(tag).ifPresent(holders -> holders.forEach(holder -> {
                SeismicAnomalyType previous = targets.put(holder.value(), type);
                if (previous != null && previous != type) {
                    throw new IllegalStateException("Conflicting Rock & Stone deposit tags for "
                        + BuiltInRegistries.BLOCK.getKey(holder.value()));
                }
            }));
        });
        return Map.copyOf(targets);
    }

    public static boolean matches(BlockState state, SeismicAnomalyType type) {
        TagKey<Block> tag = TAGS.get(type);
        return tag != null && state.is(tag);
    }

    public static boolean hasDepositBlock(String mineral) {
        return ModList.get().isLoaded("create_rns") && BuiltInRegistries.BLOCK.containsKey(
            ResourceLocation.fromNamespaceAndPath("create_rns", mineral + "_deposit_block"));
    }

    public static boolean hasCraftingMaterial(String mineral) {
        if (!hasDepositBlock(mineral) || !BuiltInRegistries.ITEM.containsKey(
                ResourceLocation.fromNamespaceAndPath("create_rns", "resonant_amethyst"))) {
            return false;
        }
        TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
            CreateGeoResonanceMod.MODID, "rock_stone_materials/" + mineral));
        return BuiltInRegistries.ITEM.getTag(tag).map(holders -> holders.iterator().hasNext()).orElse(false);
    }
}
