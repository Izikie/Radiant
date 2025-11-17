package net.minecraft.network.packet.impl.handshake.client;

import net.minecraft.network.NetworkState;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.handshake.INetHandlerHandshakeServer;

import java.io.IOException;

public class C00Handshake implements Packet<INetHandlerHandshakeServer> {
    private int protocolVersion;
    private String ip;
    private int port;
    private NetworkState requestedState;

    public C00Handshake() {
    }

    public C00Handshake(int version, String ip, int port, NetworkState requestedState) {
        this.protocolVersion = version;
        this.ip = ip;
        this.port = port;
        this.requestedState = requestedState;
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.protocolVersion = buf.readVarInt();
        this.ip = buf.readString(255);
        this.port = buf.readUnsignedShort();
        this.requestedState = NetworkState.getById(buf.readVarInt());
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeVarInt(this.protocolVersion);
        buf.writeString(this.ip);
        buf.writeShort(this.port);
        buf.writeVarInt(this.requestedState.getId());
    }

    @Override
    public void handle(INetHandlerHandshakeServer handler) {
        handler.processHandshake(this);
    }

    public NetworkState getRequestedState() {
        return this.requestedState;
    }

    public int getProtocolVersion() {
        return this.protocolVersion;
    }
}
