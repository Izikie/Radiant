package net.minecraft.network.play.server;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.util.BlockPos;

import java.io.IOException;

public class S05PacketSpawnPosition implements Packet<INetHandlerPlayClient> {
    private BlockPos spawnBlockPos;

    public S05PacketSpawnPosition() {
    }

    public S05PacketSpawnPosition(BlockPos spawnBlockPosIn) {
        this.spawnBlockPos = spawnBlockPosIn;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.spawnBlockPos = buf.readBlockPos();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeBlockPos(this.spawnBlockPos);
    }

    @Override
    public void processPacket(INetHandlerPlayClient handler) {
        handler.handleSpawnPosition(this);
    }

    public BlockPos getSpawnPos() {
        return this.spawnBlockPos;
    }
}
