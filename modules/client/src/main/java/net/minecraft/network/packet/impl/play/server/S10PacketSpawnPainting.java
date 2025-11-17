package net.minecraft.network.packet.impl.play.server;

import net.minecraft.entity.item.EntityPainting;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;

import java.io.IOException;

public class S10PacketSpawnPainting implements Packet<INetHandlerPlayClient> {
    private int entityID;
    private BlockPos position;
    private Direction facing;
    private String title;

    public S10PacketSpawnPainting() {
    }

    public S10PacketSpawnPainting(EntityPainting painting) {
        this.entityID = painting.getEntityId();
        this.position = painting.getHangingPosition();
        this.facing = painting.facingDirection;
        this.title = painting.art.title;
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.entityID = buf.readVarInt();
        this.title = buf.readString(EntityPainting.PaintingType.field_180001_A);
        this.position = buf.readBlockPos();
        this.facing = Direction.getHorizontal(buf.readUnsignedByte());
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeVarInt(this.entityID);
        buf.writeString(this.title);
        buf.writeBlockPos(this.position);
        buf.writeByte(this.facing.getHorizontalIndex());
    }

    @Override
    public void handle(INetHandlerPlayClient handler) {
        handler.handleSpawnPainting(this);
    }

    public int getEntityID() {
        return this.entityID;
    }

    public BlockPos getPosition() {
        return this.position;
    }

    public Direction getFacing() {
        return this.facing;
    }

    public String getTitle() {
        return this.title;
    }
}
