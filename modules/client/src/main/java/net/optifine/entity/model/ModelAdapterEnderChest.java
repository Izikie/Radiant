package net.optifine.entity.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelChest;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityEnderChestRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.optifine.Log;

public class ModelAdapterEnderChest extends ModelAdapter {
	public ModelAdapterEnderChest() {
		super(TileEntityEnderChest.class, "ender_chest", 0.0F);
	}

	@Override
    public ModelBase makeModel() {
		return new ModelChest();
	}

	@Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
		if (model instanceof ModelChest modelchest) {
			return modelPart.equals("lid") ? modelchest.chestLid : (modelPart.equals("base") ? modelchest.chestBelow : (modelPart.equals("knob") ? modelchest.chestKnob : null));
		} else {
			return null;
		}
	}

	@Override
    public String[] getModelRendererNames() {
		return new String[]{"lid", "base", "knob"};
	}

	@Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
		TileEntityRendererDispatcher tileentityrendererdispatcher = TileEntityRendererDispatcher.INSTANCE;
		TileEntitySpecialRenderer<TileEntityEnderChest> tileentityspecialrenderer = tileentityrendererdispatcher.getSpecialRendererByClass(TileEntityEnderChest.class);

        if (tileentityspecialrenderer instanceof TileEntityEnderChestRenderer) {
            if (tileentityspecialrenderer.getEntityClass() == null) {
                tileentityspecialrenderer = new TileEntityEnderChestRenderer();
                tileentityspecialrenderer.setRendererDispatcher(tileentityrendererdispatcher);
            }

            Log.error("Field not found: TileEntityEnderChestRenderer.modelChest");
        }
        return null;
    }
}
