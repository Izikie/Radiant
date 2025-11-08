package net.optifine.entity.model;

import net.minecraft.client.model.ModelRenderer;
import net.optifine.entity.model.anim.ModelUpdater;

public record CustomModelRenderer(String modelPart, boolean attach, ModelRenderer modelRenderer,
                                  ModelUpdater modelUpdater) {
}
