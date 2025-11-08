package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelEnderCrystal;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.RenderEnderCrystal;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.optifine.Log;

public class ModelAdapterEnderCrystal extends ModelAdapter {
	public ModelAdapterEnderCrystal() {
		this("end_crystal");
	}

	protected ModelAdapterEnderCrystal(String name) {
		super(EntityEnderCrystal.class, name, 0.5F);
	}

	@Override
    public ModelBase makeModel() {
		return new ModelEnderCrystal(0.0F, true);
	}

	@Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
		if (model instanceof ModelEnderCrystal modelendercrystal) {
			return modelPart.equals("cube") ? modelendercrystal.getCube()
					: (modelPart.equals("glass") ? modelendercrystal.getGlass()
					: (modelPart.equals("base") ? modelendercrystal.getBase()
					: null));
		} else {
			return null;
		}
	}

	@Override
    public String[] getModelRendererNames() {
		return new String[]{"cube", "glass", "base"};
	}

	@Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
		RenderManager rendermanager = Minecraft.get().getRenderManager();
		Render<EntityEnderCrystal> render = rendermanager.getEntityRenderMap().get(EntityEnderCrystal.class);

		if (!(render instanceof RenderEnderCrystal renderendercrystal)) {
			Log.error("Not an instance of RenderEnderCrystal: " + render);
			return null;
		} else {

            Log.error("Field not found: RenderEnderCrystal.modelEnderCrystal");
            return null;
        }
	}
}
