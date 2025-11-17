package net.minecraft.network.encoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.network.packet.api.PacketBuffer;

import java.io.IOException;

public class MessagePrepender extends MessageToByteEncoder<ByteBuf> {

    private static final int MAX_VARINT_SIZE = 3;

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf payload, ByteBuf out) throws Exception {
        int size = payload.readableBytes();
        int varIntSize = PacketBuffer.getVarIntSize(size);

        if (varIntSize > MAX_VARINT_SIZE) {
            throw new IOException(
                    "Packet payload of " + size + " bytes exceeds maximum allowed size of " + MAX_VARINT_SIZE
            );
        }

        PacketBuffer buffer = new PacketBuffer(out);
        buffer.ensureWritable(varIntSize + size);
        buffer.writeVarInt(size);
        buffer.writeBytes(payload, payload.readerIndex(), size);
    }
}
