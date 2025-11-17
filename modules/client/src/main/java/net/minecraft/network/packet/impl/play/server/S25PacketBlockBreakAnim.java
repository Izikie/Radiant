package net.minecraft.network.packet.impl.play.server;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;
import net.minecraft.util.BlockPos;

import java.io.IOException;

public class S25PacketBlockBreakAnim implements Packet<INetHandlerPlayClient> {
    private int breakerId;
    private BlockPos position;
    private int progress;

    public S25PacketBlockBreakAnim() {
    }

    public S25PacketBlockBreakAnim(int breakerId, BlockPos pos, int progress) {
        this.breakerId = breakerId;
        this.position = pos;
        this.progress = progress;
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.breakerId = buf.readVarInt();
        this.position = buf.readBlockPos();
        this.progress = buf.readUnsignedByte();
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeVarInt(this.breakerId);
        buf.writeBlockPos(this.position);
        buf.writeByte(this.progress);
    }

    @Override
    public void handle(INetHandlerPlayClient handler) {
        handler.handleBlockBreakAnim(this);
    }

    public int getBreakerId() {
        return this.breakerId;
    }

    public BlockPos getPosition() {
        return this.position;
    }

    public int getProgress() {
        return this.progress;
    }
}
