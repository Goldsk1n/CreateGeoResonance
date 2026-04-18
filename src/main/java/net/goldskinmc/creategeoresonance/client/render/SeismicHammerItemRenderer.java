package net.goldskinmc.creategeoresonance.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class SeismicHammerItemRenderer extends CustomRenderedItemModelRenderer {
    private static final float MAX_PISTON_TRAVEL = 3.0F / 16.0F;
    // Hand-space forward for this model setup: negative Y.
    private static final float PISTON_AXIS_X = 0.0F;
    private static final float PISTON_AXIS_Y = -1.0F;
    private static final float PISTON_AXIS_Z = 0.0F;
    private static final PartialModel BODY = PartialModel.of(ResourceLocation.fromNamespaceAndPath(
        CreateGeoResonanceMod.MODID, "item/seismic_hammer_body"));
    private static final PartialModel PISTON = PartialModel.of(ResourceLocation.fromNamespaceAndPath(
        CreateGeoResonanceMod.MODID, "item/seismic_hammer_piston"));

    @Override
    protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ItemDisplayContext transformType,
                          PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        float travel = pistonTravel(stack, transformType);

        renderer.render(BODY.get(), light);

        ms.pushPose();
        ms.translate(PISTON_AXIS_X * travel, PISTON_AXIS_Y * travel, PISTON_AXIS_Z * travel);
        renderer.render(PISTON.get(), light);
        ms.popPose();
    }

    private static float pistonTravel(ItemStack stack, ItemDisplayContext transformType) {
        if (!isHandContext(transformType)) {
            return 0.0F;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return 0.0F;
        }
        float cooldown = minecraft.player.getCooldowns().getCooldownPercent(stack.getItem(), minecraft.getFrameTime());
        cooldown = Mth.clamp(cooldown, 0.0F, 1.0F);
        float eased = cooldown * cooldown * (3.0F - 2.0F * cooldown);
        return MAX_PISTON_TRAVEL * eased;
    }

    private static boolean isHandContext(ItemDisplayContext transformType) {
        return transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            || transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
            || transformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
            || transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }
}
