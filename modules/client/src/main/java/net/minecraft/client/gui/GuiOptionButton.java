package net.minecraft.client.gui;

import net.minecraft.client.settings.GameSettings;

public class GuiOptionButton extends GuiButton {
    private final GameSettings.Options enumOptions;

    public GuiOptionButton(int id, int x, int y, String label) {
        this(id, x, y, null, label);
    }

    public GuiOptionButton(int id, int x, int y, int width, int height, String label) {
        super(id, x, y, width, height, label);
        this.enumOptions = null;
    }

    public GuiOptionButton(int id, int x, int y, GameSettings.Options options, String label) {
        super(id, x, y, 150, 20, label);
        this.enumOptions = options;
    }

    public GameSettings.Options returnEnumOptions() {
        return this.enumOptions;
    }
}
