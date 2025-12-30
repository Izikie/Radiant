package net.optifine.entity.model;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRabbit;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderRabbit;
import net.minecraft.entity.passive.EntityRabbit;

public class ModelAdapterRabbit extends ModelAdapter {
    private static Object2IntOpenHashMap<String> mapPartFields = null;

    public ModelAdapterRabbit() {
        super(EntityRabbit.class, "rabbit", 0.3F);
    }

    private static Object2IntOpenHashMap<String> getMapPartFields() {
        if (mapPartFields == null) {
            mapPartFields = new Object2IntOpenHashMap<>();
            mapPartFields.put("left_foot", 0);
            mapPartFields.put("right_foot", 1);
            mapPartFields.put("left_thigh", 2);
            mapPartFields.put("right_thigh", 3);
            mapPartFields.put("body", 4);
            mapPartFields.put("left_arm", 5);
            mapPartFields.put("right_arm", 6);
            mapPartFields.put("head", 7);
            mapPartFields.put("right_ear", 8);
            mapPartFields.put("left_ear", 9);
            mapPartFields.put("tail", 10);
            mapPartFields.put("nose", 11);
        }
        return mapPartFields;
    }

    @Override
    public ModelBase makeModel() {
        return new ModelRabbit();
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (model instanceof ModelRabbit modelrabbit) {
            Object2IntOpenHashMap<String> map = getMapPartFields();

            if (map.containsKey(modelPart)) {
                int i = map.getInt(modelPart);
                return switch (i) {
                    case 0 -> modelrabbit.getRabbitLeftFoot();
                    case 1 -> modelrabbit.getRabbitRightFoot();
                    case 2 -> modelrabbit.getRabbitLeftThigh();
                    case 3 -> modelrabbit.getRabbitRightThigh();
                    case 4 -> modelrabbit.getRabbitBody();
                    case 5 -> modelrabbit.getRabbitLeftArm();
                    case 6 -> modelrabbit.getRabbitRightArm();
                    case 7 -> modelrabbit.getRabbitHead();
                    case 8 -> modelrabbit.getRabbitRightEar();
                    case 9 -> modelrabbit.getRabbitLeftEar();
                    case 10 -> modelrabbit.getRabbitTail();
                    case 11 -> modelrabbit.getRabbitNose();
                    default -> null;
                };
                //return (ModelRenderer) Reflector.getFieldValue(modelrabbit, Reflector.ModelRabbit_renderers, i);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"left_foot", "right_foot", "left_thigh", "right_thigh", "body", "left_arm", "right_arm", "head", "right_ear", "left_ear", "tail", "nose"};
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        RenderManager rendermanager = Minecraft.get().getRenderManager();
        return new RenderRabbit(rendermanager, modelBase, shadowSize);
    }
}
