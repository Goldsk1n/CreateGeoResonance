package net.goldskinmc.creategeoresonance.client;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceItems;
import net.goldskinmc.creategeoresonance.seismic.SeismicAnomalyType;
import net.goldskinmc.creategeoresonance.seismic.SeismicCustomModuleItem;
import net.goldskinmc.creategeoresonance.seismic.SeismicCustomModules;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@JeiPlugin
public final class GeoResonanceJeiPlugin implements IModPlugin {
    @Nullable
    private IJeiRuntime runtime;
    private int revision = Integer.MIN_VALUE;

    public GeoResonanceJeiPlugin() {
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("creategeoresonance", "custom_modules");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(GeoResonanceItems.CUSTOM_MODULE.get(),
            new mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<net.minecraft.world.item.ItemStack>() {
                @Override
                public Object getSubtypeData(net.minecraft.world.item.ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
                    SeismicAnomalyType type = SeismicCustomModuleItem.target(stack);
                    return type == null ? null : type.name();
                }

                @Override
                public String getLegacyStringSubtypeInfo(net.minecraft.world.item.ItemStack stack,
                                                          mezz.jei.api.ingredients.subtypes.UidContext context) {
                    SeismicAnomalyType type = SeismicCustomModuleItem.target(stack);
                    return type == null ? "" : type.name();
                }
            });
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        registration.addExtraIngredients(VanillaTypes.ITEM_STACK, SeismicCustomModules.definitions().stream()
            .map(definition -> SeismicCustomModuleItem.create(definition.id())).toList());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        this.runtime = runtime;
        revision = Integer.MIN_VALUE;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    private void onClientTick(ClientTickEvent.Post event) {
        IJeiRuntime active = runtime;
        if (active == null || revision == SeismicCustomModules.revision()) {
            return;
        }
        revision = SeismicCustomModules.revision();
        var ingredients = active.getIngredientManager();
        var previous = ingredients.getAllIngredients(VanillaTypes.ITEM_STACK).stream()
            .filter(stack -> stack.getItem() instanceof SeismicCustomModuleItem).toList();
        if (!previous.isEmpty()) {
            ingredients.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, previous);
        }
        var current = SeismicCustomModules.definitions().stream()
            .map(definition -> SeismicCustomModuleItem.create(definition.id())).toList();
        if (!current.isEmpty()) {
            ingredients.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, current);
        }
    }
}
