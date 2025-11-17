package net.minecraft.network.packet.impl.play.server;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;

import java.io.IOException;

public class S31PacketWindowProperty implements Packet<INetHandlerPlayClient> {
    private int windowId;
    private int varIndex;
    private int varValue;

    public S31PacketWindowProperty() {
    }

    public S31PacketWindowProperty(int windowIdIn, int varIndexIn, int varValueIn) {
        this.windowId = windowIdIn;
        this.varIndex = varIndexIn;
        this.varValue = varValueIn;
    }

    @Override
    public void handle(INetHandlerPlayClient handler) {
        handler.handleWindowProperty(this);
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.windowId = buf.readUnsignedByte();
        this.varIndex = buf.readShort();
        this.varValue = buf.readShort();
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeByte(this.windowId);
        buf.writeShort(this.varIndex);
        buf.writeShort(this.varValue);
    }

    public int getWindowId() {
        return this.windowId;
    }

    public int getVarIndex() {
        return this.varIndex;
    }

    public int getVarValue() {
        return this.varValue;
    }
}
