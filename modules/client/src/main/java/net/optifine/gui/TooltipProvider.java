package net.optifine.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.joml.Vector4i;

public interface TooltipProvider {
    Vector4i getTooltipBounds(GuiScreen var1, int var2, int var3);

    String[] getTooltipLines(GuiButton var1, int var2);

    boolean isRenderBorder();
}
