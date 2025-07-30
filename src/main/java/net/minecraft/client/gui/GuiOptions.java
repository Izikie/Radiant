package net.minecraft.client.gui;

import net.minecraft.client.gui.resourcepack.GuiScreenResourcePacks;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.Difficulty;

import java.io.IOException;

public class GuiOptions extends GuiScreen implements GuiYesNoCallback {
    private static final GameSettings.Options[] field_146440_f = new GameSettings.Options[]{GameSettings.Options.FOV};
    private final GuiScreen pauseMenu;
    private final GameSettings gameSettings;
    private GuiButton button;
    private GuiLockIconButton guiLockIconButton;
    protected String title = "Options";

    public GuiOptions(GuiScreen pauseMenu, GameSettings gameSettings) {
        this.pauseMenu = pauseMenu;
        this.gameSettings = gameSettings;
    }

    public void initGui() {
        int i = 0;
        this.title = I18n.format("options.title");

        for (GameSettings.Options gamesettings$options : field_146440_f) {
            if (gamesettings$options.getEnumFloat()) {
                this.buttonList.add(new GuiOptionSlider(1000, this.width / 2 - 155 + i % 2 * 160, this.height / 6 - 12 + 24 * (i >> 1), gamesettings$options));
            } else {
                GuiOptionButton guioptionbutton = new GuiOptionButton(gamesettings$options.returnEnumOrdinal(), this.width / 2 - 155 + i % 2 * 160, this.height / 6 - 12 + 24 * (i >> 1), gamesettings$options, this.gameSettings.getKeyBinding(gamesettings$options));
                this.buttonList.add(guioptionbutton);
            }

            ++i;
        }

        if (this.mc.world != null) {
            Difficulty enumdifficulty = this.mc.world.getDifficulty();
            this.button = new GuiButton(1, this.width / 2 - 155 + i % 2 * 160, this.height / 6 - 12 + 24 * (i >> 1), 150, 20, this.getDifficultyString(enumdifficulty));
            this.buttonList.add(this.button);

            if (this.mc.isSingleplayer() && !this.mc.world.getWorldInfo().isHardcoreModeEnabled()) {
                this.button.setWidth(this.button.getButtonWidth() - 20);
                this.guiLockIconButton = new GuiLockIconButton(2, this.button.xPosition + this.button.getButtonWidth(), this.button.yPosition);
                this.buttonList.add(this.guiLockIconButton);
                this.guiLockIconButton.setLocked(this.mc.world.getWorldInfo().isDifficultyLocked());
                this.guiLockIconButton.enabled = !this.guiLockIconButton.isLocked();
                this.button.enabled = !this.guiLockIconButton.isLocked();
            } else {
                this.button.enabled = false;
            }
        }

        this.buttonList.add(new GuiButton(3, this.width / 2 - 155, this.height / 6 + 48 - 6, 150, 20, I18n.format("options.skinCustomisation")));
        this.buttonList.add(new GuiButton(4, this.width / 2 - 155, this.height / 6 + 72 - 6, 150, 20, I18n.format("options.sounds")));
        this.buttonList.add(new GuiButton(5, this.width / 2 + 5, this.height / 6 + 72 - 6, 150, 20, I18n.format("options.controls")));
        this.buttonList.add(new GuiButton(6, this.width / 2 - 155, this.height / 6 + 96 - 6, 150, 20, I18n.format("options.video")));
        this.buttonList.add(new GuiButton(7, this.width / 2 + 5, this.height / 6 + 96 - 6, 150, 20, I18n.format("options.chat.title")));
        this.buttonList.add(new GuiButton(8, this.width / 2 - 155, this.height / 6 + 120 - 6, 150, 20, I18n.format("options.resourcepack")));
        this.buttonList.add(new GuiButton(9, this.width / 2 + 5, this.height / 6 + 120 - 6, 150, 20, I18n.format("options.language")));
        this.buttonList.add(new GuiButton(10, this.width / 2 - 100, this.height / 6 + 146, I18n.format("gui.done")));
    }

    public String getDifficultyString(Difficulty enumDifficulty) {
        IChatComponent ichatcomponent = new ChatComponentText("");
        ichatcomponent.appendSibling(new ChatComponentTranslation("options.difficulty"));
        ichatcomponent.appendText(": ");
        ichatcomponent.appendSibling(new ChatComponentTranslation(enumDifficulty.getDifficultyResourceKey()));
        return ichatcomponent.getFormattedText();
    }

    public void confirmClicked(boolean result, int id) {
        this.mc.displayGuiScreen(this);

        if (id == 2 && result && this.mc.world != null) {
            this.mc.world.getWorldInfo().setDifficultyLocked(true);
            this.guiLockIconButton.setLocked(true);
            this.guiLockIconButton.enabled = false;
            this.button.enabled = false;
        }
    }

    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.enabled) {
            switch (button.id) {
                case 1 -> {
                    this.mc.world.getWorldInfo().setDifficulty(Difficulty.getDifficultyEnum(this.mc.world.getDifficulty().getDifficultyId() + 1));
                    this.button.displayString = this.getDifficultyString(this.mc.world.getDifficulty());
                }

                case 2 ->
                        this.mc.displayGuiScreen(new GuiYesNo(this, (new ChatComponentTranslation("difficulty.lock.title")).getFormattedText(), (new ChatComponentTranslation("difficulty.lock.question", new ChatComponentTranslation(this.mc.world.getWorldInfo().getDifficulty().getDifficultyResourceKey()))).getFormattedText(), 109));

                case 3 -> {
                    this.mc.gameSettings.saveOptions();
                    this.mc.displayGuiScreen(new GuiCustomizeSkin(this));
                }

                case 4 -> {
                    this.mc.gameSettings.saveOptions();
                    this.mc.displayGuiScreen(new GuiScreenOptionsSounds(this, this.gameSettings));
                }

                case 5 -> {
                    this.mc.gameSettings.saveOptions();
                    this.mc.displayGuiScreen(new GuiControls(this, this.gameSettings));
                }

                case 6 -> {
                    this.mc.gameSettings.saveOptions();
                    this.mc.displayGuiScreen(new GuiVideoSettings(this, this.gameSettings));

                }

                case 7 -> {
                    this.mc.gameSettings.saveOptions();
                    this.mc.displayGuiScreen(new ScreenChatOptions(this, this.gameSettings));
                }

                case 8 -> {
                    this.mc.gameSettings.saveOptions();
                    this.mc.displayGuiScreen(new GuiScreenResourcePacks(this));
                }

                case 9 -> {
                    this.mc.gameSettings.saveOptions();
                    this.mc.displayGuiScreen(new GuiLanguage(this, this.gameSettings, this.mc.getLanguageManager()));
                }

                case 10 -> {
                    this.mc.gameSettings.saveOptions();
                    this.mc.displayGuiScreen(this.pauseMenu);
                }

                case 1000 -> {
                    // Do Nothing
                }

                default -> {
                    GameSettings.Options gamesettings$options = ((GuiOptionButton) button).returnEnumOptions();
                    this.gameSettings.setOptionValue(gamesettings$options, 1);
                    button.displayString = this.gameSettings.getKeyBinding(GameSettings.Options.getEnumOptions(button.id));
                }
            }
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, this.title, this.width / 2, 15, 16777215);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
