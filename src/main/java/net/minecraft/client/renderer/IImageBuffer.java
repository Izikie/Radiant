package net.minecraft.client.renderer;

import net.radiant.util.NativeImage;

public interface IImageBuffer {
    NativeImage parseUserSkin(NativeImage image);

    void skinAvailable();
}
