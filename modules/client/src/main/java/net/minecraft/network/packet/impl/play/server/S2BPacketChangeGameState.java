package net.minecraft.network.packet.impl.play.server;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;

import java.io.IOException;

public class S2BPacketChangeGameState implements Packet<INetHandlerPlayClient> {
    public static final String[] MESSAGE_NAMES = new String[]{"tile.bed.notValid"};
    private int state;
    private float field_149141_c;

    public S2BPacketChangeGameState() {
    }

    public S2BPacketChangeGameState(int stateIn, float p_i45194_2_) {
        this.state = stateIn;
        this.field_149141_c = p_i45194_2_;
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.state = buf.readUnsignedByte();
        this.field_149141_c = buf.readFloat();
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeByte(this.state);
        buf.writeFloat(this.field_149141_c);
    }

    @Override
    public void handle(INetHandlerPlayClient handler) {
        handler.handleChangeGameState(this);
    }

    public int getGameState() {
        return this.state;
    }

    public float func_149137_d() {
        return this.field_149141_c;
    }
}
