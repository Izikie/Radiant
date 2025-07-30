package net.minecraft.client.gui.resourcepack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.ResourcePackListEntry;
import net.minecraft.util.Formatting;

import java.util.List;

public abstract class GuiResourcePackList extends GuiListExtended {
    protected final Minecraft mc;
    protected final List<ResourcePackListEntry> packs;

    public GuiResourcePackList(Minecraft mc, int width, int height, List<ResourcePackListEntry> packs) {
        super(mc, width, height, 32, height - 55 + 4, 36);
        this.mc = mc;
        this.packs = packs;
        this.field_148163_i = false;
        this.setHasListHeader(true, (int) (mc.fontRendererObj.FONT_HEIGHT * 1.5F));
    }

    protected void drawListHeader(int width, int height, Tessellator tessellator) {
        String s = Formatting.UNDERLINE + "" + Formatting.BOLD + this.getListHeader();
        this.mc.fontRendererObj.drawString(s, width + this.width / 2 - this.mc.fontRendererObj.getStringWidth(s) / 2, Math.min(this.top + 3, height), 16777215);
    }

    protected abstract String getListHeader();

    public List<ResourcePackListEntry> getList() {
        return this.packs;
    }

    protected int getSize() {
        return this.getList().size();
    }

    public ResourcePackListEntry getListEntry(int index) {
        return this.getList().get(index);
    }

    public int getListWidth() {
        return this.width;
    }

    protected int getScrollBarX() {
        return this.right - 6;
    }
}
