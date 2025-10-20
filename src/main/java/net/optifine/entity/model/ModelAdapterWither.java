package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelWither;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderWither;
import net.minecraft.entity.boss.EntityWither;
import net.optifine.Config;

public class ModelAdapterWither extends ModelAdapter {
	public ModelAdapterWither() {
		super(EntityWither.class, "wither", 0.5F);
	}

	@Override
    public ModelBase makeModel() {
		return new ModelWither(0.0F);
	}

	@Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
		if (model instanceof ModelWither modelwither) {
			String s = "body";

			if (modelPart.startsWith(s)) {
				ModelRenderer[] amodelrenderer1 = modelwither.getBodyParts();

				if (amodelrenderer1 == null) {
					return null;
				} else {
					String s3 = modelPart.substring(s.length());
					int j = Config.parseInt(s3, -1);
					--j;
					return j >= 0 && j < amodelrenderer1.length ? amodelrenderer1[j] : null;
				}
			} else {
				String s1 = "head";

				if (modelPart.startsWith(s1)) {
					ModelRenderer[] amodelrenderer = modelwither.getHeads();

					if (amodelrenderer == null) {
						return null;
					} else {
						String s2 = modelPart.substring(s1.length());
						int i = Config.parseInt(s2, -1);
						--i;
						return i >= 0 && i < amodelrenderer.length ? amodelrenderer[i] : null;
					}
				} else {
					return null;
				}
			}
		} else {
			return null;
		}
	}

	@Override
    public String[] getModelRendererNames() {
		return new String[]{"body1", "body2", "body3", "head1", "head2", "head3"};
	}

	@Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
		RenderManager rendermanager = Minecraft.get().getRenderManager();
		RenderWither renderwither = new RenderWither(rendermanager);
		renderwither.mainModel = modelBase;
		renderwither.shadowSize = shadowSize;
		return renderwither;
	}
}
