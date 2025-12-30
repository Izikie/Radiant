package net.optifine.entity.model;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelOcelot;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderOcelot;
import net.minecraft.entity.passive.EntityOcelot;

public class ModelAdapterOcelot extends ModelAdapter {
    private static Object2IntOpenHashMap<String> mapPartFields = null;

    public ModelAdapterOcelot() {
        super(EntityOcelot.class, "ocelot", 0.4F);
    }

    private static Object2IntOpenHashMap<String> getMapPartFields() {
        if (mapPartFields == null) {
            mapPartFields = new Object2IntOpenHashMap<>();
            mapPartFields.put("back_left_leg", 0);
            mapPartFields.put("back_right_leg", 1);
            mapPartFields.put("front_left_leg", 2);
            mapPartFields.put("front_right_leg", 3);
            mapPartFields.put("tail", 4);
            mapPartFields.put("tail2", 5);
            mapPartFields.put("head", 6);
            mapPartFields.put("body", 7);
        }
        return mapPartFields;
    }

    @Override
    public ModelBase makeModel() {
        return new ModelOcelot();
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (model instanceof ModelOcelot modelocelot) {
            Object2IntOpenHashMap<String> map = getMapPartFields();

            if (map.containsKey(modelPart)) {
                int i = map.getInt(modelPart);
                return switch (i) {
                    case 0 -> modelocelot.getOcelotBackLeftLeg();
                    case 1 -> modelocelot.getOcelotBackRightLeg();
                    case 2 -> modelocelot.getOcelotFrontLeftLeg();
                    case 3 -> modelocelot.getOcelotFrontRightLeg();
                    case 4 -> modelocelot.getOcelotTail();
                    case 5 -> modelocelot.getOcelotTail2();
                    case 6 -> modelocelot.getOcelotHead();
                    case 7 -> modelocelot.getOcelotBody();
                    default -> null;
                };
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"back_left_leg", "back_right_leg", "front_left_leg", "front_right_leg", "tail", "tail2", "head", "body"};
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        RenderManager rendermanager = Minecraft.get().getRenderManager();
        return new RenderOcelot(rendermanager, modelBase, shadowSize);
    }
}
