package net.minecraft.client.gui.resourcepack.api;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.gui.resourcepack.GuiScreenResourcePacks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.ResourcePackListEntry;
import net.minecraft.util.chat.Formatting;

import java.util.List;
import java.util.function.Predicate;

public abstract class GuiResourcePackList extends GuiListExtended {
    protected final List<ResourcePackListEntry> packs;

    protected GuiResourcePackList(Minecraft mc, int width, int height, List<ResourcePackListEntry> packs) {
        super(mc, width, height, 32, height - 55 + 4, 36);
        this.packs = packs;
        this.field_148163_i = false;
        this.setHasListHeader(true, (int) (mc.fontRendererObj.FONT_HEIGHT * 1.5F));
    }

    @Override
    protected void drawListHeader(int width, int height, Tessellator tessellator) {
        String s = Formatting.UNDERLINE + "" + Formatting.BOLD + this.getListHeader();
        this.mc.fontRendererObj.drawString(s, width + this.width / 2 - this.mc.fontRendererObj.getStringWidth(s) / 2, Math.min(this.top + 3, height), 16777215);
    }

    protected abstract String getListHeader();

    public List<ResourcePackListEntry> getList() {
        final List<ResourcePackListEntry> packs = Lists.newCopyOnWriteArrayList(this.packs);

        if (this instanceof GuiResourcePackAvailable && !GuiScreenResourcePacks.searchBox.getText().isBlank()) {
            final Predicate<ResourcePackListEntry> filter = new ResourcePackFilter(GuiScreenResourcePacks.searchBox.getText());

            for (ResourcePackListEntry pack : this.packs) {
                if (!filter.test(pack)) {
                    packs.remove(pack);
                }
            }
        }

        return packs;
    }

    @Override
    protected int getSize() {
        return this.getList().size();
    }

    @Override
    public ResourcePackListEntry getListEntry(int index) {
        return this.getList().get(index);
    }

    @Override
    public int getListWidth() {
        return this.width;
    }

    @Override
    protected int getScrollBarX() {
        return this.right - 6;
    }
}
