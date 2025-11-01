package net.minecraft.client.network;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.packet.impl.handshake.INetHandlerHandshakeServer;
import net.minecraft.network.packet.impl.handshake.client.C00Handshake;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.NetHandlerLoginServer;
import net.minecraft.util.chat.IChatComponent;

public class NetHandlerHandshakeMemory implements INetHandlerHandshakeServer {
    private final MinecraftServer mcServer;
    private final NetworkManager networkManager;

    public NetHandlerHandshakeMemory(MinecraftServer mcServerIn, NetworkManager networkManagerIn) {
        this.mcServer = mcServerIn;
        this.networkManager = networkManagerIn;
    }

    @Override
    public void processHandshake(C00Handshake packet) {
        this.networkManager.setConnectionState(packet.getRequestedState());
        this.networkManager.setNetHandler(new NetHandlerLoginServer(this.mcServer, this.networkManager));
    }

    @Override
    public void onDisconnect(IChatComponent reason) {
    }
}
