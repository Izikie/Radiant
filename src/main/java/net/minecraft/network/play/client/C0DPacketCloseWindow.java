package net.minecraft.network.play.client;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayServer;

import java.io.IOException;

public class C0DPacketCloseWindow implements Packet<INetHandlerPlayServer> {
    private int windowId;

    public C0DPacketCloseWindow() {
    }

    public C0DPacketCloseWindow(int windowId) {
        this.windowId = windowId;
    }

    @Override
    public void processPacket(INetHandlerPlayServer handler) {
        handler.processCloseWindow(this);
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.windowId = buf.readByte();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeByte(this.windowId);
    }
}
