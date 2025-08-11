package net.optifine.player;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.util.ResourceLocation;
import net.radiant.util.NativeImage;

public class CapeImageBuffer extends ImageBufferDownload {
	private final ResourceLocation resourceLocation;
	private AbstractClientPlayer player;
	private boolean elytraOfCape;

	public CapeImageBuffer(AbstractClientPlayer player, ResourceLocation resourceLocation) {
		this.player = player;
		this.resourceLocation = resourceLocation;
	}

	public NativeImage parseUserSkin(NativeImage imageRaw) {
		NativeImage image = CapeUtils.parseCape(imageRaw);
		this.elytraOfCape = CapeUtils.isElytraCape(imageRaw, image);
		return image;
	}

	public void skinAvailable() {
		if (this.player != null) {
			this.player.setLocationOfCape(this.resourceLocation);
			this.player.setElytraOfCape(this.elytraOfCape);
		}

		this.cleanup();
	}

	public void cleanup() {
		this.player = null;
	}

	public boolean isElytraOfCape() {
		return this.elytraOfCape;
	}
}
