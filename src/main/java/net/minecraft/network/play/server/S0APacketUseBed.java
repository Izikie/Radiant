package net.minecraft.network.play.server;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.io.IOException;

public class S0APacketUseBed implements Packet<INetHandlerPlayClient> {
    private int playerID;
    private BlockPos pos;

    public S0APacketUseBed() {
    }

    public S0APacketUseBed(EntityPlayer player, BlockPos bedPosIn) {
        this.playerID = player.getEntityId();
        this.pos = bedPosIn;
    }

    public void readPacketData(PacketBuffer buf) throws IOException {
        this.playerID = buf.readVarIntFromBuffer();
        this.pos = buf.readBlockPos();
    }

    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarIntToBuffer(this.playerID);
        buf.writeBlockPos(this.pos);
    }

    public void processPacket(INetHandlerPlayClient handler) {
        handler.handleUseBed(this);
    }

    public EntityPlayer getPlayer(World worldIn) {
        return (EntityPlayer) worldIn.getEntityByID(this.playerID);
    }

    public BlockPos getPosition() {
        return this.pos;
    }
}
