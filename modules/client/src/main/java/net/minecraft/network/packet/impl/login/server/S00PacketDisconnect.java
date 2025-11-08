package net.minecraft.network.packet.impl.login.server;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.login.INetHandlerLoginClient;
import net.minecraft.util.chat.IChatComponent;

import java.io.IOException;

public class S00PacketDisconnect implements Packet<INetHandlerLoginClient> {
    private IChatComponent reason;

    public S00PacketDisconnect() {
    }

    public S00PacketDisconnect(IChatComponent reasonIn) {
        this.reason = reasonIn;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.reason = buf.readChatComponent();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeChatComponent(this.reason);
    }

    @Override
    public void processPacket(INetHandlerLoginClient handler) {
        handler.handleDisconnect(this);
    }

    public IChatComponent getReason() {
        return this.reason;
    }
}
