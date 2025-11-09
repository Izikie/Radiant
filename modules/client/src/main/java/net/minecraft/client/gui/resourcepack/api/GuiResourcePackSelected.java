package net.minecraft.client.gui.resourcepack.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.ResourcePackListEntry;

import java.util.List;

public class GuiResourcePackSelected extends GuiResourcePackList {
    public GuiResourcePackSelected(Minecraft mc, int width, int height, List<ResourcePackListEntry> pack) {
        super(mc, width, height, pack);
    }

    @Override
    protected String getListHeader() {
        return I18n.format("resourcePack.selected.title");
    }
}
