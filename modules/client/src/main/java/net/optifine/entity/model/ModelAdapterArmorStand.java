package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelArmorStand;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityArmorStand;
import net.optifine.Config;

public class ModelAdapterArmorStand extends ModelAdapterBiped {
    public ModelAdapterArmorStand() {
        super(EntityArmorStand.class, "armor_stand", 0.0F);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelArmorStand();
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (!(model instanceof ModelArmorStand modelarmorstand)) {
            return null;
        } else {
            return modelPart.equals("right") ? modelarmorstand.standRightSide : (modelPart.equals("left") ? modelarmorstand.standLeftSide : (modelPart.equals("waist") ? modelarmorstand.standWaist : (modelPart.equals("base") ? modelarmorstand.standBase : super.getModelRenderer(modelarmorstand, modelPart))));
        }
    }

    @Override
    public String[] getModelRendererNames() {
        String[] astring = super.getModelRendererNames();
        astring = (String[]) Config.addObjectsToArray(astring, new String[]{"right", "left", "waist", "base"});
        return astring;
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        RenderManager rendermanager = Minecraft.get().getRenderManager();
        ArmorStandRenderer armorstandrenderer = new ArmorStandRenderer(rendermanager);
        armorstandrenderer.mainModel = modelBase;
        armorstandrenderer.shadowSize = shadowSize;
        return armorstandrenderer;
    }
}
