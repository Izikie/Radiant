package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelLeashKnot;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderLeashKnot;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLeashKnot;
import net.optifine.Log;

public class ModelAdapterLeadKnot extends ModelAdapter {
	public ModelAdapterLeadKnot() {
		super(EntityLeashKnot.class, "lead_knot", 0.0F);
	}

	@Override
    public ModelBase makeModel() {
		return new ModelLeashKnot();
	}

	@Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
		if (model instanceof ModelLeashKnot modelleashknot) {
			return modelPart.equals("knot") ? modelleashknot.field_110723_a : null;
		} else {
			return null;
		}
	}

	@Override
    public String[] getModelRendererNames() {
		return new String[]{"knot"};
	}

	@Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
		RenderManager rendermanager = Minecraft.get().getRenderManager();
		RenderLeashKnot renderleashknot = new RenderLeashKnot(rendermanager);

        Log.error("Field not found: RenderLeashKnot.leashKnotModel");
        return null;
    }
}
