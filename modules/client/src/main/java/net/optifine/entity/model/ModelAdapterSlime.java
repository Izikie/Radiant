package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSlime;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSlime;
import net.minecraft.entity.monster.EntitySlime;

public class ModelAdapterSlime extends ModelAdapter {
    public ModelAdapterSlime() {
        super(EntitySlime.class, "slime", 0.25F);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelSlime(16);
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (!(model instanceof ModelSlime modelslime)) {
            return null;
        } else {
            return modelPart.equals("body") ? modelslime.getSlimeBodies()
                    : (modelPart.equals("left_eye") ? modelslime.getSlimeLeftEye()
                    : (modelPart.equals("right_eye") ? modelslime.getSlimeRightEye()
                    : (modelPart.equals("mouth") ? modelslime.getSlimeMouth()
                    : null)));
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"body", "left_eye", "right_eye", "mouth"};
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        RenderManager rendermanager = Minecraft.get().getRenderManager();
        return new RenderSlime(rendermanager, modelBase, shadowSize);
    }
}
