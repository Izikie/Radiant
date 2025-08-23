package net.optifine.player;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.optifine.Config;
import net.optifine.Log;

import java.util.Map;

public class PlayerItemsLayer implements LayerRenderer {
	private final RenderPlayer renderPlayer;

	public PlayerItemsLayer(RenderPlayer renderPlayer) {
		this.renderPlayer = renderPlayer;
	}

	public static void register(Map<String, RenderPlayer> renderPlayerMap) {
		boolean flag = false;

		for (Object object : renderPlayerMap.keySet()) {
			Object object1 = renderPlayerMap.get(object);

			if (object1 instanceof RenderPlayer renderplayer) {
				renderplayer.addLayer(new PlayerItemsLayer(renderplayer));
				flag = true;
			}
		}

		if (!flag) {
			Log.error("PlayerItemsLayer not registered");
		}
	}

	public void doRenderLayer(EntityLivingBase entityLiving, float limbSwing, float limbSwingAmount, float partialTicks, float ticksExisted, float headYaw, float rotationPitch, float scale) {
		this.renderEquippedItems(entityLiving, scale, partialTicks);
	}

	protected void renderEquippedItems(EntityLivingBase entityLiving, float scale, float partialTicks) {
		if (Config.isShowCapes()) {
			if (!entityLiving.isInvisible()) {
				if (entityLiving instanceof AbstractClientPlayer abstractclientplayer) {
					GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
					GlStateManager.disableRescaleNormal();
					GlStateManager.enableCull();
					ModelBiped modelbiped = this.renderPlayer.getMainModel();
					PlayerConfigurations.renderPlayerItems(modelbiped, abstractclientplayer, scale, partialTicks);
					GlStateManager.disableCull();
				}
			}
		}
	}

	public boolean shouldCombineTextures() {
		return false;
	}
}
