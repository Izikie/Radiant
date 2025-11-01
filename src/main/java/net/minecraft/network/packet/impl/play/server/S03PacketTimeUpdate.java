package net.minecraft.network.packet.impl.play.server;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;

import java.io.IOException;

public class S03PacketTimeUpdate implements Packet<INetHandlerPlayClient> {
    private long totalWorldTime;
    private long worldTime;

    public S03PacketTimeUpdate() {
    }

    public S03PacketTimeUpdate(long totalWorldTimeIn, long totalTimeIn, boolean doDayLightCycle) {
        this.totalWorldTime = totalWorldTimeIn;
        this.worldTime = totalTimeIn;

        if (!doDayLightCycle) {
            this.worldTime = -this.worldTime;

            if (this.worldTime == 0L) {
                this.worldTime = -1L;
            }
        }
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.totalWorldTime = buf.readLong();
        this.worldTime = buf.readLong();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeLong(this.totalWorldTime);
        buf.writeLong(this.worldTime);
    }

    @Override
    public void processPacket(INetHandlerPlayClient handler) {
        handler.handleTimeUpdate(this);
    }

    public long getTotalWorldTime() {
        return this.totalWorldTime;
    }

    public long getWorldTime() {
        return this.worldTime;
    }
}
