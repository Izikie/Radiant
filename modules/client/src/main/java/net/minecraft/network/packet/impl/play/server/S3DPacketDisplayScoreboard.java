package net.minecraft.network.packet.impl.play.server;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;
import net.minecraft.scoreboard.ScoreObjective;

import java.io.IOException;

public class S3DPacketDisplayScoreboard implements Packet<INetHandlerPlayClient> {
    private int position;
    private String scoreName;

    public S3DPacketDisplayScoreboard() {
    }

    public S3DPacketDisplayScoreboard(int positionIn, ScoreObjective scoreIn) {
        this.position = positionIn;

        if (scoreIn == null) {
            this.scoreName = "";
        } else {
            this.scoreName = scoreIn.getName();
        }
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.position = buf.readByte();
        this.scoreName = buf.readString(16);
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeByte(this.position);
        buf.writeString(this.scoreName);
    }

    @Override
    public void handle(INetHandlerPlayClient handler) {
        handler.handleDisplayScoreboard(this);
    }

    public int func_149371_c() {
        return this.position;
    }

    public String func_149370_d() {
        return this.scoreName;
    }
}
