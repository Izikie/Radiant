package net.minecraft.client.renderer;

import net.radiant.NativeImage;

public interface IImageBuffer {
    NativeImage parseUserSkin(NativeImage image);

    void skinAvailable();
}
