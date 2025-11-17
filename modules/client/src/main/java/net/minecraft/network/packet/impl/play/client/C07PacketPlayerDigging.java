package net.minecraft.network.packet.impl.play.client;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;

import java.io.IOException;

public class C07PacketPlayerDigging implements Packet<INetHandlerPlayServer> {
    private BlockPos position;
    private Direction facing;
    private Action status;

    public C07PacketPlayerDigging() {
    }

    public C07PacketPlayerDigging(Action statusIn, BlockPos posIn, Direction facingIn) {
        this.status = statusIn;
        this.position = posIn;
        this.facing = facingIn;
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.status = buf.readEnum(Action.class);
        this.position = buf.readBlockPos();
        this.facing = Direction.getFront(buf.readUnsignedByte());
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeEnum(this.status);
        buf.writeBlockPos(this.position);
        buf.writeByte(this.facing.getIndex());
    }

    @Override
    public void handle(INetHandlerPlayServer handler) {
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
