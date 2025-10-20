package net.minecraft.network.play.server;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.util.BlockPos;

import java.io.IOException;

public class S36PacketSignEditorOpen implements Packet<INetHandlerPlayClient> {
    private BlockPos signPosition;

    public S36PacketSignEditorOpen() {
    }

    public S36PacketSignEditorOpen(BlockPos signPositionIn) {
        this.signPosition = signPositionIn;
    }

    @Override
    public void processPacket(INetHandlerPlayClient handler) {
        handler.handleSignEditorOpen(this);
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.signPosition = buf.readBlockPos();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeBlockPos(this.signPosition);
    }

    public BlockPos getSignPosition() {
        return this.signPosition;
    }
}
