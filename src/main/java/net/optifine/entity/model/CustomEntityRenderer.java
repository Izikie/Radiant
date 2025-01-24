package net.optifine.entity.model;

import net.minecraft.util.ResourceLocation;

public record CustomEntityRenderer(String name, String basePath, ResourceLocation textureLocation,
                                   CustomModelRenderer[] customModelRenderers, float shadowSize) {
}
