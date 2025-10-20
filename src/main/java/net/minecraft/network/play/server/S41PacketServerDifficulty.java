package net.minecraft.network.play.server;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.world.Difficulty;

import java.io.IOException;

public class S41PacketServerDifficulty implements Packet<INetHandlerPlayClient> {
    private Difficulty difficulty;
    private boolean difficultyLocked;

    public S41PacketServerDifficulty() {
    }

    public S41PacketServerDifficulty(Difficulty difficultyIn, boolean lockedIn) {
        this.difficulty = difficultyIn;
        this.difficultyLocked = lockedIn;
    }

    @Override
    public void processPacket(INetHandlerPlayClient handler) {
        handler.handleServerDifficulty(this);
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.difficulty = Difficulty.getDifficultyEnum(buf.readUnsignedByte());
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeByte(this.difficulty.getDifficultyId());
    }

    public boolean isDifficultyLocked() {
        return this.difficultyLocked;
    }

    public Difficulty getDifficulty() {
        return this.difficulty;
    }
}
