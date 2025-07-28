package net.optifine.entity.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSign;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySignRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntitySign;
import net.optifine.Log;
import net.optifine.reflect.Reflector;

public class ModelAdapterSign extends ModelAdapter {
	public ModelAdapterSign() {
		super(TileEntitySign.class, "sign", 0.0F);
	}

	public ModelBase makeModel() {
		return new ModelSign();
	}

	public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
		if (model instanceof ModelSign modelsign) {
			return modelPart.equals("board") ? modelsign.signBoard : (modelPart.equals("stick") ? modelsign.signStick : null);
		} else {
			return null;
		}
	}

	public String[] getModelRendererNames() {
		return new String[]{"board", "stick"};
	}

	public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
		TileEntityRendererDispatcher tileentityrendererdispatcher = TileEntityRendererDispatcher.INSTANCE;
		TileEntitySpecialRenderer tileentityspecialrenderer = tileentityrendererdispatcher.getSpecialRendererByClass(TileEntitySign.class);

		if (!(tileentityspecialrenderer instanceof TileEntitySignRenderer)) {
			return null;
		} else {
			if (tileentityspecialrenderer.getEntityClass() == null) {
				tileentityspecialrenderer = new TileEntitySignRenderer();
				tileentityspecialrenderer.setRendererDispatcher(tileentityrendererdispatcher);
			}

            Log.error("Field not found: TileEntitySignRenderer.model");
            return null;
        }
	}
}
