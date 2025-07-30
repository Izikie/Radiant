package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelDragon;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderDragon;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.boss.EntityDragon;

public class ModelAdapterDragon extends ModelAdapter {
	public ModelAdapterDragon() {
		super(EntityDragon.class, "dragon", 0.5F);
	}

	public ModelBase makeModel() {
		return new ModelDragon(0.0F);
	}

	public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
		if (!(model instanceof ModelDragon modeldragon)) {
			return null;
		} else {
			return modelPart.equals("head") ? modeldragon.getHead()
					: (modelPart.equals("spine") ? modeldragon.getSpine()
					: (modelPart.equals("jaw") ? modeldragon.getJaw()
					: (modelPart.equals("body") ? modeldragon.getBody()
					: (modelPart.equals("rear_leg") ? modeldragon.getRearLeg()
					: (modelPart.equals("front_leg") ? modeldragon.getFrontLeg()
					: (modelPart.equals("rear_leg_tip") ? modeldragon.getRearLegTip()
					: (modelPart.equals("front_leg_tip") ? modeldragon.getFrontLegTip()
					: (modelPart.equals("rear_foot") ? modeldragon.getRearFoot()
					: (modelPart.equals("front_foot") ? modeldragon.getFrontFoot()
					: (modelPart.equals("wing") ? modeldragon.getWing()
					: (modelPart.equals("wing_tip") ? modeldragon.getWingTip()
					: null)))))))))));
		}
	}

	public String[] getModelRendererNames() {
		return new String[]{"head", "spine", "jaw", "body", "rear_leg", "front_leg", "rear_leg_tip", "front_leg_tip", "rear_foot", "front_foot", "wing", "wing_tip"};
	}

	public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
		RenderManager rendermanager = Minecraft.getMinecraft().getRenderManager();
		RenderDragon renderdragon = new RenderDragon(rendermanager);
		renderdragon.mainModel = modelBase;
		renderdragon.shadowSize = shadowSize;
		return renderdragon;
	}
}
