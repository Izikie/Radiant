package net.minecraft.network.packet.impl.play.server;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;

import java.io.IOException;

public class S1FPacketSetExperience implements Packet<INetHandlerPlayClient> {
    private float field_149401_a;
    private int totalExperience;
    private int level;

    public S1FPacketSetExperience() {
    }

    public S1FPacketSetExperience(float p_i45222_1_, int totalExperienceIn, int levelIn) {
        this.field_149401_a = p_i45222_1_;
        this.totalExperience = totalExperienceIn;
        this.level = levelIn;
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.field_149401_a = buf.readFloat();
        this.level = buf.readVarInt();
        this.totalExperience = buf.readVarInt();
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeFloat(this.field_149401_a);
        buf.writeVarInt(this.level);
        buf.writeVarInt(this.totalExperience);
    }

    @Override
    public void handle(INetHandlerPlayClient handler) {
        handler.handleSetExperience(this);
    }

    public float func_149397_c() {
        return this.field_149401_a;
    }

    public int getTotalExperience() {
        return this.totalExperience;
    }

    public int getLevel() {
        return this.level;
    }
}
