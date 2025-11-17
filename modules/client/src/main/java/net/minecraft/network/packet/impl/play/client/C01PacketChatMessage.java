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
    public void read(PacketBuffer buf) throws IOException {
        this.message = buf.readString(100);
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeString(this.message);
    }

    @Override
    public void handle(INetHandlerPlayServer handler) {
        handler.processChatMessage(this);
    }

    public String getMessage() {
        return this.message;
    }
}
