package net.minecraft.network.play.client;

import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;

import java.io.IOException;

public class C07PacketPlayerDigging implements Packet<INetHandlerPlayServer> {
    private BlockPos position;
    private Direction facing;
    private Action status;

    public C07PacketPlayerDigging() {}

    public C07PacketPlayerDigging(Action statusIn, BlockPos posIn, Direction facingIn) {
        this.status = statusIn;
        this.position = posIn;
        this.facing = facingIn;
    }

    public void readPacketData(PacketBuffer buf) throws IOException {
        this.status = buf.readEnumValue(Action.class);
        this.position = buf.readBlockPos();
        this.facing = Direction.getFront(buf.readUnsignedByte());
    }

    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeEnumValue(this.status);
        buf.writeBlockPos(this.position);
        buf.writeByte(this.facing.getIndex());
    }

    public void processPacket(INetHandlerPlayServer handler) {
        handler.processPlayerDigging(this);
    }

    public BlockPos getPosition() {
        return this.position;
    }

    public Direction getFacing() {
        return this.facing;
    }

    public Action getStatus() {
        return this.status;
    }

    public enum Action {
        START_DESTROY_BLOCK,
        ABORT_DESTROY_BLOCK,
        STOP_DESTROY_BLOCK,
        DROP_ALL_ITEMS,
        DROP_ITEM,
        RELEASE_USE_ITEM
    }
}
