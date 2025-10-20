package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderMinecartMobSpawner;
import net.minecraft.entity.ai.EntityMinecartMobSpawner;
import net.optifine.Log;

public class ModelAdapterMinecartMobSpawner extends ModelAdapterMinecart {
	public ModelAdapterMinecartMobSpawner() {
		super(EntityMinecartMobSpawner.class, "spawner_minecart", 0.5F);
	}

	@Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
		RenderManager rendermanager = Minecraft.get().getRenderManager();
		RenderMinecartMobSpawner renderminecartmobspawner = new RenderMinecartMobSpawner(rendermanager);

        Log.error("Field not found: RenderMinecart.modelMinecart");
        return null;
    }
}
