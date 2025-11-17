package net.minecraft.network.packet.impl.play.server;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;
import net.minecraft.util.chat.IChatComponent;

import java.io.IOException;

public class S02PacketChat implements Packet<INetHandlerPlayClient> {
    private IChatComponent chatComponent;
    private byte type;

    public S02PacketChat() {
    }

    public S02PacketChat(IChatComponent component) {
        this(component, (byte) 1);
    }

    public S02PacketChat(IChatComponent message, byte typeIn) {
        this.chatComponent = message;
        this.type = typeIn;
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.chatComponent = buf.readChatComponent();
        this.type = buf.readByte();
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeChatComponent(this.chatComponent);
        buf.writeByte(this.type);
    }

    @Override
    public void handle(INetHandlerPlayClient handler) {
        handler.handleChat(this);
    }

    public IChatComponent getChatComponent() {
        return this.chatComponent;
    }

    public boolean isChat() {
        return this.type == 1 || this.type == 2;
    }

    public byte getType() {
        return this.type;
    }
}
