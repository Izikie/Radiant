package net.minecraft.server.network;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.status.INetHandlerStatusServer;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.client.C01PacketPing;
import net.minecraft.network.status.server.S00PacketServerInfo;
import net.minecraft.network.status.server.S01PacketPong;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public class NetHandlerStatusServer implements INetHandlerStatusServer {
    private static final IChatComponent EXIT_MESSAGE = new ChatComponentText("Status request has been handled.");
    private final MinecraftServer server;
    private final NetworkManager networkManager;
    private boolean handled;

    public NetHandlerStatusServer(MinecraftServer serverIn, NetworkManager netManager) {
        this.server = serverIn;
        this.networkManager = netManager;
    }

    public void onDisconnect(IChatComponent reason) {
    }

    public void processServerQuery(C00PacketServerQuery packet) {
        if (this.handled) {
            this.networkManager.closeChannel(EXIT_MESSAGE);
        } else {
            this.handled = true;
            this.networkManager.sendPacket(new S00PacketServerInfo(this.server.getServerStatusResponse()));
        }
    }

    public void processPing(C01PacketPing packet) {
        this.networkManager.sendPacket(new S01PacketPong(packet.getTime()));
        this.networkManager.closeChannel(EXIT_MESSAGE);
    }
}
