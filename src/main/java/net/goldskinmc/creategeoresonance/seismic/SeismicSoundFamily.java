package net.goldskinmc.creategeoresonance.seismic;

import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public enum SeismicSoundFamily {
    METAL("metal"),
    CRYSTAL("crystal"),
    STONE("stone"),
    LIQUID("liquid"),
    SPECIAL("special");

    private final String serializedName;

    SeismicSoundFamily(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public SoundEvent sound() {
        return switch (this) {
            case METAL -> SoundEvents.NOTE_BLOCK_BELL.value();
            case CRYSTAL -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case STONE -> SoundEvents.CALCITE_HIT;
            case LIQUID -> SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT;
            case SPECIAL -> SoundEvents.SCULK_CLICKING;
        };
    }

    public static SeismicSoundFamily parse(ResourceLocation id, String value) {
        for (SeismicSoundFamily family : values()) {
            if (family.serializedName.equals(value)) {
                return family;
            }
        }
        throw new JsonParseException(id + ": sound_family must be 'metal', 'crystal', 'stone', 'liquid' or 'special'");
    }
}
