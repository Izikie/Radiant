package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBoat;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderBoat;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityBoat;
import net.optifine.Log;

public class ModelAdapterBoat extends ModelAdapter {
    public ModelAdapterBoat() {
        super(EntityBoat.class, "boat", 0.5F);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelBoat();
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (model instanceof ModelBoat modelboat) {
            return modelPart.equals("bottom") ? modelboat.boatSides[0] : (modelPart.equals("back") ? modelboat.boatSides[1] : (modelPart.equals("front") ? modelboat.boatSides[2] : (modelPart.equals("right") ? modelboat.boatSides[3] : (modelPart.equals("left") ? modelboat.boatSides[4] : null))));
        } else {
            return null;
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"bottom", "back", "front", "right", "left"};
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        RenderManager rendermanager = Minecraft.get().getRenderManager();
        RenderBoat renderboat = new RenderBoat(rendermanager);

        Log.error("Field not found: RenderBoat.modelBoat");
        return null;
    }
}
