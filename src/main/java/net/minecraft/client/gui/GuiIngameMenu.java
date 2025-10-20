package net.minecraft.client.gui;

import net.minecraft.client.gui.achievement.GuiAchievements;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraft.client.resources.I18n;
import net.radiant.GuiInGameMultiplayer;

import java.io.IOException;

public class GuiIngameMenu extends GuiScreen {

    @Override
    public void initGui() {
        this.buttonList.clear();
        int offset = -16;

        this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height / 4 + 24 + offset, I18n.format("menu.returnToGame")));
        this.buttonList.add(new GuiButton(1, this.width / 2 - 100, this.height / 4 + 48 + offset, I18n.format("menu.multiplayer")));

        this.buttonList.add(new GuiButton(2, this.width / 2 - 100, this.height / 4 + 96 + offset, 98, 20, I18n.format("menu.options")));
        GuiButton shareBtn = new GuiButton(3, this.width / 2 + 2, this.height / 4 + 96 + offset, 98, 20, I18n.format("menu.shareToLan"));
        shareBtn.enabled = this.mc.isSingleplayer() && !this.mc.getIntegratedServer().getPublic();
        this.buttonList.add(shareBtn);

        String quitLabel = this.mc.isIntegratedServerRunning() ? I18n.format("menu.returnToMenu") : I18n.format("menu.disconnect");
        this.buttonList.add(new GuiButton(4, this.width / 2 - 100, this.height / 4 + 120 + offset, quitLabel));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0 -> {
                this.mc.displayGuiScreen(null);
                this.mc.setIngameFocus();
            }
            case 1 -> this.mc.displayGuiScreen(new GuiInGameMultiplayer());
            case 2 -> this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
            case 3 -> this.mc.displayGuiScreen(new GuiShareToLan(this));
            case 4 -> {
                boolean flag = this.mc.isIntegratedServerRunning();
                button.enabled = false;
                this.mc.world.sendQuittingDisconnectingPacket();
                this.mc.loadWorld(null);

                if (flag) {
                    this.mc.displayGuiScreen(new GuiMainMenu());
                } else {
                    this.mc.displayGuiScreen(new GuiMultiplayer(new GuiMainMenu()));
                }
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, I18n.format("menu.game"), this.width / 2, 40, 16777215);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
