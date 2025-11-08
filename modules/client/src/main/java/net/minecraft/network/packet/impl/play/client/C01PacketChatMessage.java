package net.minecraft.network.packet.impl.play.client;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayServer;

import java.io.IOException;

public class C01PacketChatMessage implements Packet<INetHandlerPlayServer> {
    private String message;

    public C01PacketChatMessage() {
    }

    public C01PacketChatMessage(String messageIn) {
        if (messageIn.length() > 100) {
            messageIn = messageIn.substring(0, 100);
        }

        this.message = messageIn;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.message = buf.readStringFromBuffer(100);
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeString(this.message);
    }

    @Override
    public void processPacket(INetHandlerPlayServer handler) {
        handler.processChatMessage(this);
    }

    public String getMessage() {
        return this.message;
    }
}
