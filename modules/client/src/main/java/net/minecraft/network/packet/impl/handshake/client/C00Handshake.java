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
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.protocolVersion = buf.readVarIntFromBuffer();
        this.ip = buf.readStringFromBuffer(255);
        this.port = buf.readUnsignedShort();
        this.requestedState = NetworkState.getById(buf.readVarIntFromBuffer());
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarIntToBuffer(this.protocolVersion);
        buf.writeString(this.ip);
        buf.writeShort(this.port);
        buf.writeVarIntToBuffer(this.requestedState.getId());
    }

    @Override
    public void processPacket(INetHandlerHandshakeServer handler) {
        handler.processHandshake(this);
    }

    public NetworkState getRequestedState() {
        return this.requestedState;
    }

    public int getProtocolVersion() {
        return this.protocolVersion;
    }
}
