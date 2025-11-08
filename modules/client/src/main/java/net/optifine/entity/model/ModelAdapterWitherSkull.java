package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSkeletonHead;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.RenderWitherSkull;
import net.minecraft.entity.projectile.EntityWitherSkull;
import net.optifine.Log;

public class ModelAdapterWitherSkull extends ModelAdapter {
	public ModelAdapterWitherSkull() {
		super(EntityWitherSkull.class, "wither_skull", 0.0F);
	}

	@Override
    public ModelBase makeModel() {
		return new ModelSkeletonHead();
	}

	@Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
		if (model instanceof ModelSkeletonHead modelskeletonhead) {
			return modelPart.equals("head") ? modelskeletonhead.skeletonHead : null;
		} else {
			return null;
		}
	}

	@Override
    public String[] getModelRendererNames() {
		return new String[]{"head"};
	}

	@Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
		RenderManager rendermanager = Minecraft.get().getRenderManager();
		RenderWitherSkull renderwitherskull = new RenderWitherSkull(rendermanager);

        Log.error("Field not found: RenderWitherSkull_model");
        return null;
    }
}
