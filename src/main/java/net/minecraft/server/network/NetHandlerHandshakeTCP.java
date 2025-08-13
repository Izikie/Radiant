package net.minecraft.server.network;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.NetworkState;
import net.minecraft.network.handshake.INetHandlerHandshakeServer;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.server.S00PacketDisconnect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.chat.ChatComponentText;
import net.minecraft.util.chat.IChatComponent;

public class NetHandlerHandshakeTCP implements INetHandlerHandshakeServer {
    private final MinecraftServer server;
    private final NetworkManager networkManager;

    public NetHandlerHandshakeTCP(MinecraftServer serverIn, NetworkManager netManager) {
        this.server = serverIn;
        this.networkManager = netManager;
    }

    public void processHandshake(C00Handshake packet) {
        switch (packet.getRequestedState()) {
            case LOGIN:
                this.networkManager.setConnectionState(NetworkState.LOGIN);

                if (packet.getProtocolVersion() > 47) {
                    ChatComponentText chatcomponenttext = new ChatComponentText("Outdated server! I'm still on 1.8.9");
                    this.networkManager.sendPacket(new S00PacketDisconnect(chatcomponenttext));
                    this.networkManager.closeChannel(chatcomponenttext);
                } else if (packet.getProtocolVersion() < 47) {
                    ChatComponentText chatcomponenttext1 = new ChatComponentText("Outdated client! Please use 1.8.9");
                    this.networkManager.sendPacket(new S00PacketDisconnect(chatcomponenttext1));
                    this.networkManager.closeChannel(chatcomponenttext1);
                } else {
                    this.networkManager.setNetHandler(new NetHandlerLoginServer(this.server, this.networkManager));
                }

                break;

            case STATUS:
                this.networkManager.setConnectionState(NetworkState.STATUS);
                this.networkManager.setNetHandler(new NetHandlerStatusServer(this.server, this.networkManager));
                break;

            default:
                throw new UnsupportedOperationException("Invalid intention " + packet.getRequestedState());
        }
    }

    public void onDisconnect(IChatComponent reason) {
    }
}
