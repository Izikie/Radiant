package net.minecraft.network.packet.impl.play.server;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;
import net.minecraft.world.Difficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import java.io.IOException;

public class S07PacketRespawn implements Packet<INetHandlerPlayClient> {
    private int dimensionID;
    private Difficulty difficulty;
    private WorldSettings.GameType gameType;
    private WorldType worldType;

    public S07PacketRespawn() {
    }

    public S07PacketRespawn(int dimensionIDIn, Difficulty difficultyIn, WorldType worldTypeIn, WorldSettings.GameType gameTypeIn) {
        this.dimensionID = dimensionIDIn;
        this.difficulty = difficultyIn;
        this.gameType = gameTypeIn;
        this.worldType = worldTypeIn;
    }

    @Override
    public void handle(INetHandlerPlayClient handler) {
        handler.handleRespawn(this);
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.dimensionID = buf.readInt();
        this.difficulty = Difficulty.getDifficultyEnum(buf.readUnsignedByte());
        this.gameType = WorldSettings.GameType.getByID(buf.readUnsignedByte());
        this.worldType = WorldType.parseWorldType(buf.readString(16));

        if (this.worldType == null) {
            this.worldType = WorldType.DEFAULT;
        }
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeInt(this.dimensionID);
        buf.writeByte(this.difficulty.getDifficultyId());
        buf.writeByte(this.gameType.getID());
        buf.writeString(this.worldType.getWorldTypeName());
    }

    public int getDimensionID() {
        return this.dimensionID;
    }

    public Difficulty getDifficulty() {
        return this.difficulty;
    }

    public WorldSettings.GameType getGameType() {
        return this.gameType;
    }

    public WorldType getWorldType() {
        return this.worldType;
    }
}
