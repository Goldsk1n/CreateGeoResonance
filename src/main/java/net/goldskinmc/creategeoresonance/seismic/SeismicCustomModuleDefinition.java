package net.goldskinmc.creategeoresonance.seismic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import java.util.Set;

public record SeismicCustomModuleDefinition(ResourceLocation id, String name, String signalName,
                                             ResourceLocation icon, TagKey<Block> blockTag, int color, IconSize iconSize) {
    private static final Set<String> FIELDS = Set.of("name", "signal_name", "icon", "block_tag", "color", "icon_size");

    public enum IconSize {
        STANDARD("standard"),
        SMALL("small");

        private final String serializedName;

        IconSize(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }
    }

    public static SeismicCustomModuleDefinition parse(ResourceLocation id, JsonObject json) {
        for (String field : json.keySet()) {
            if (!FIELDS.contains(field)) {
                throw new JsonParseException(id + ": unknown module field '" + field + "'");
            }
        }
        if (id.toString().length() > 160) {
            throw new JsonParseException("Module ID is too long: " + id);
        }
        String name = text(json, "name");
        String signal = text(json, "signal_name");
        ResourceLocation icon = resource(json, "icon");
        ResourceLocation tag = resource(json, "block_tag");
        if (!BuiltInRegistries.ITEM.containsKey(icon) || BuiltInRegistries.ITEM.get(icon) == Items.AIR
            || icon.toString().equals("creategeoresonance:custom_module")) {
            throw new JsonParseException(id + ": icon must be an existing, non-custom item: " + icon);
        }
        String hex = GsonHelper.getAsString(json, "color");
        if (!hex.matches("#[0-9a-fA-F]{6}")) {
            throw new JsonParseException(id + ": color must be #RRGGBB");
        }
        IconSize iconSize = switch (GsonHelper.getAsString(json, "icon_size", "standard")) {
            case "standard" -> IconSize.STANDARD;
            case "small" -> IconSize.SMALL;
            default -> throw new JsonParseException(id + ": icon_size must be 'standard' or 'small'");
        };
        return new SeismicCustomModuleDefinition(id, name, signal, icon, BlockTags.create(tag),
            Integer.parseInt(hex.substring(1), 16), iconSize);
    }

    private static String text(JsonObject json, String field) {
        String value = GsonHelper.getAsString(json, field);
        if (value.isBlank() || value.length() > 80 || value.chars().anyMatch(Character::isISOControl)) {
            throw new JsonParseException(field + " must contain 1-80 characters");
        }
        return value;
    }

    private static ResourceLocation resource(JsonObject json, String field) {
        String value = GsonHelper.getAsString(json, field);
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null || value.indexOf(':') < 0) {
            throw new JsonParseException(field + " must be a namespaced ID: " + value);
        }
        return id;
    }

    public Component displayName() {
        return Component.translatableWithFallback(translationPrefix() + ".name", name);
    }

    public Component signalDisplayName() {
        return Component.translatableWithFallback(translationPrefix() + ".signal", signalName);
    }

    private String translationPrefix() {
        return "module." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("signal_name", signalName);
        json.addProperty("icon", icon.toString());
        json.addProperty("icon_size", iconSize.serializedName());
        json.addProperty("block_tag", blockTag.location().toString());
        json.addProperty("color", String.format(java.util.Locale.ROOT, "#%06X", color));
        return json;
    }
}
