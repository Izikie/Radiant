package net.minecraft.network.packet.impl.play.server;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;

import java.io.IOException;
import java.util.Objects;

public class S29PacketSoundEffect implements Packet<INetHandlerPlayClient> {
    private String soundName;
    private int posX;
    private int posY = Integer.MAX_VALUE;
    private int posZ;
    private float soundVolume;
    private int soundPitch;

    public S29PacketSoundEffect() {
    }

    public S29PacketSoundEffect(String soundNameIn, double soundX, double soundY, double soundZ, float volume, float pitch) {
        Objects.requireNonNull(soundNameIn, "name");
        this.soundName = soundNameIn;
        this.posX = (int) (soundX * 8.0D);
        this.posY = (int) (soundY * 8.0D);
        this.posZ = (int) (soundZ * 8.0D);
        this.soundVolume = volume;
        this.soundPitch = (int) (pitch * 63.0F);
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.soundName = buf.readStringFromBuffer(256);
        this.posX = buf.readInt();
        this.posY = buf.readInt();
        this.posZ = buf.readInt();
        this.soundVolume = buf.readFloat();
        this.soundPitch = buf.readUnsignedByte();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeString(this.soundName);
        buf.writeInt(this.posX);
        buf.writeInt(this.posY);
        buf.writeInt(this.posZ);
        buf.writeFloat(this.soundVolume);
        buf.writeByte(this.soundPitch);
    }

    public String getSoundName() {
        return this.soundName;
    }

    public double getX() {
        return (this.posX / 8.0F);
    }

    public double getY() {
        return (this.posY / 8.0F);
    }

    public double getZ() {
        return (this.posZ / 8.0F);
    }

    public float getVolume() {
        return this.soundVolume;
    }

    public float getPitch() {
        return this.soundPitch / 63.0F;
    }

    @Override
    public void processPacket(INetHandlerPlayClient handler) {
        handler.handleSoundEffect(this);
    }
}
