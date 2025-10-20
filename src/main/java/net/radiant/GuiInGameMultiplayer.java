package net.radiant;

import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.multiplayer.ServerData;

public class GuiInGameMultiplayer extends GuiMultiplayer {
    public GuiInGameMultiplayer() {
        super(null);
    }

    @Override
    public void connectToServer(ServerData server) {
        disconnect();
        super.connectToServer(server);
    }

    private void disconnect() {
        mc.world.sendQuittingDisconnectingPacket();
        mc.player.sendEndCombat();
        mc.loadWorld(null);
        mc.displayGuiScreen(null);
    }
}
