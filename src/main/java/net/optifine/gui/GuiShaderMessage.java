package net.optifine.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.optifine.shaders.gui.GuiShaders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiShaderMessage extends GuiScreen {
	private final GuiScreen screen;
	private final String main;
	private final String secondary;
	private final List<String> lines = new ArrayList<>();
	private final int actionIndex;

	public GuiShaderMessage(GuiScreen screen, String main, String secondary, int actionIndex) {
		this.screen = screen;
		this.main = main;
		this.secondary = secondary;
		this.actionIndex = actionIndex;
	}

	@Override
    public void initGui() {
		buttonList.add(new GuiOptionButton(0, width / 2 - 74, height / 6 + 96, I18n.format("gui.back")));
		lines.clear();
		lines.addAll(fontRendererObj.listFormattedStringToWidth(secondary, width - 50));

		buttonList.add(new GuiOptionButton(1, width / 2 - 74, height / 6 + 116 + 4, I18n.format("options.off")));
	}

	@Override
    protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 0) {
			mc.displayGuiScreen(screen);
		} else if (button.id == 1) {
			switch (actionIndex) {
				case 0 -> {
					mc.gameSettings.ofAaLevel = 0;
					mc.gameSettings.saveOptions();
				}
				case 1 -> {
					mc.gameSettings.ofAfLevel = 1;
					mc.gameSettings.saveOptions();
				}
				case 2 -> {
					mc.gameSettings.ofFastRender = false;
					mc.gameSettings.saveOptions();
				}
			}

			mc.displayGuiScreen(new GuiShaders(screen));
		}
	}

	@Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRendererObj, main, width / 2, 70, 16777215);
		int i = 90;

		for (String line : lines) {
			drawCenteredString(fontRendererObj, line, width / 2, i, 16777215);
			i += fontRendererObj.FONT_HEIGHT;
		}

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

}
