package net.radiant.render;

import net.radiant.render.opengl.OpenGlTextureBackend;
import net.radiant.render.vulkan.VulkanPreviewTextureBackend;
import net.radiant.render.vulkan.VulkanTerrainPreview;

public final class TextureBackends {
    public static final TextureBackend ACTIVE = VulkanTerrainPreview.ENABLED
            ? VulkanPreviewTextureBackend.INSTANCE : new OpenGlTextureBackend();

    private TextureBackends() {
    }
}
