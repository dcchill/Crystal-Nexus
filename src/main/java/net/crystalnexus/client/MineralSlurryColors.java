package net.crystalnexus.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.crystalnexus.processing.MaterialProcessingCatalog;
import net.crystalnexus.processing.SlurryColorMath;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MineralSlurryColors {
    public static final int FALLBACK = 0xff7f95a3;
    private static final Map<ResourceLocation, Integer> CACHE = new ConcurrentHashMap<>();

    private MineralSlurryColors() {}

    public static int tint(FluidStack slurry) {
        ResourceLocation material = MaterialProcessingCatalog.slurryMaterial(slurry).orElse(null);
        Minecraft minecraft = Minecraft.getInstance();
        if (material == null || minecraft.level == null) return FALLBACK;
        return CACHE.computeIfAbsent(material, ignored -> color(minecraft, material));
    }

    private static int color(Minecraft minecraft, ResourceLocation id) {
        ItemStack mineral = MaterialProcessingCatalog.get(minecraft.level).byId(id)
            .map(material -> material.dust("crystalnexus", 1)).orElse(ItemStack.EMPTY);
        if (mineral.isEmpty()) return FALLBACK;
        try {
            NativeImage image = minecraft.getItemRenderer().getModel(mineral, minecraft.level, null, 0)
                .getParticleIcon().contents().getOriginalImage();
            return SlurryColorMath.averageAbgr(image.getPixelsRGBA(), FALLBACK);
        } catch (RuntimeException ignored) {
            return FALLBACK;
        }
    }

    public static void clear() { CACHE.clear(); }
}
