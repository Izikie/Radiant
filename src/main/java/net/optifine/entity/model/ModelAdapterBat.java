package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBat;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderBat;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.passive.EntityBat;

public class ModelAdapterBat extends ModelAdapter {
	public ModelAdapterBat() {
		super(EntityBat.class, "bat", 0.25F);
	}

	@Override
    public ModelBase makeModel() {
		return new ModelBat();
	}

	@Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
		if (model instanceof ModelBat modelbat) {
			return modelPart.equals("head") ? modelbat.getBatHead()
					: (modelPart.equals("body") ? modelbat.getBatBody()
					: (modelPart.equals("right_wing") ? modelbat.getBatRightWing()
					: (modelPart.equals("left_wing") ? modelbat.getBatLeftWing()
					: (modelPart.equals("outer_right_wing") ? modelbat.getBatOuterRightWing()
					: (modelPart.equals("outer_left_wing") ? modelbat.getBatOuterLeftWing()
					: null)))));
		} else {
			return null;
		}
	}

	@Override
    public String[] getModelRendererNames() {
		return new String[]{"head", "body", "right_wing", "left_wing", "outer_right_wing", "outer_left_wing"};
	}

	@Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
		RenderManager rendermanager = Minecraft.get().getRenderManager();
		RenderBat renderbat = new RenderBat(rendermanager);
		renderbat.mainModel = modelBase;
		renderbat.shadowSize = shadowSize;
		return renderbat;
	}
}
