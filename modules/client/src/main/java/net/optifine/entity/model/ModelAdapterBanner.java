package net.optifine.entity.model;

import net.minecraft.client.model.ModelBanner;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityBannerRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntityBanner;
import net.optifine.Log;

public class ModelAdapterBanner extends ModelAdapter {
    public ModelAdapterBanner() {
        super(TileEntityBanner.class, "banner", 0.0F);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelBanner();
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (model instanceof ModelBanner modelbanner) {
            return modelPart.equals("slate") ? modelbanner.bannerSlate : (modelPart.equals("stand") ? modelbanner.bannerStand : (modelPart.equals("top") ? modelbanner.bannerTop : null));
        } else {
            return null;
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"slate", "stand", "top"};
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        TileEntityRendererDispatcher tileentityrendererdispatcher = TileEntityRendererDispatcher.INSTANCE;
        TileEntitySpecialRenderer<TileEntityBanner> tileentityspecialrenderer = tileentityrendererdispatcher.getSpecialRendererByClass(TileEntityBanner.class);

        if (tileentityspecialrenderer instanceof TileEntityBannerRenderer) {
            if (tileentityspecialrenderer.getEntityClass() == null) {
                tileentityspecialrenderer = new TileEntityBannerRenderer();
                tileentityspecialrenderer.setRendererDispatcher(tileentityrendererdispatcher);
            }

            Log.error("Field not found: TileEntityBannerRenderer.bannerModel");
        }
        return null;
    }
}
