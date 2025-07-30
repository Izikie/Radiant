package net.minecraft.client.gui.resourcepack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.ResourcePackListEntry;

import java.util.List;

public class GuiResourcePackAvailable extends GuiResourcePackList {
    public GuiResourcePackAvailable(Minecraft mc, int width, int height, List<ResourcePackListEntry> packs) {
        super(mc, width, height, packs);
    }

    protected String getListHeader() {
        return I18n.format("resourcePack.available.title");
    }
}
