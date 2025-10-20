package net.optifine.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.optifine.shaders.config.ShaderOptions;
import net.optifine.shaders.gui.GuiButtonEnumShaderOption;
import org.joml.Vector4i;

public class TooltipProviderEnumShaderOptions implements TooltipProvider {
	@Override
    public Vector4i getTooltipBounds(GuiScreen guiScreen, int x, int y) {
		int i = guiScreen.width - 450;
		int j = 35;

		if (i < 10) {
			i = 10;
		}

		if (y <= j + 94) {
			j += 100;
		}

		int k = i + 150 + 150;
		int l = j + 84 + 10;
		return new Vector4i(i, j, k - i, l - j);
	}

	@Override
    public boolean isRenderBorder() {
		return true;
	}

	@Override
    public String[] getTooltipLines(GuiButton btn, int width) {
        if (!(btn instanceof GuiButtonEnumShaderOption guibuttonenumshaderoption)) {
            return null;
        } else {
            ShaderOptions enumshaderoption = guibuttonenumshaderoption.getEnumShaderOption();
            return this.getTooltipLines(enumshaderoption);
        }
	}

	private String[] getTooltipLines(ShaderOptions option) {
		return TooltipProviderOptions.getTooltipLines(option.getResourceKey());
	}
}
