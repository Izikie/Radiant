package net.minecraft.network.packet.impl.play.client;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayServer;
import net.minecraft.util.BlockPos;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class C14PacketTabComplete implements Packet<INetHandlerPlayServer> {
    private String message;
    private BlockPos targetBlock;

    public C14PacketTabComplete() {
    }

    public C14PacketTabComplete(String msg) {
        this(msg, null);
    }

    public C14PacketTabComplete(String msg, BlockPos target) {
        this.message = msg;
        this.targetBlock = target;
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.message = buf.readString(32767);
        boolean flag = buf.readBoolean();

        if (flag) {
            this.targetBlock = buf.readBlockPos();
        }
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeString(StringUtils.substring(this.message, 0, 32767));
        boolean flag = this.targetBlock != null;
        buf.writeBoolean(flag);

        if (flag) {
            buf.writeBlockPos(this.targetBlock);
        }
    }

    @Override
    public void handle(INetHandlerPlayServer handler) {
        handler.processTabComplete(this);
    }

    public String getMessage() {
        return this.message;
    }

    public BlockPos getTargetBlock() {
        return this.targetBlock;
    }
}
