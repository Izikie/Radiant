package net.minecraft.util;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.network.PacketBuffer;

public class MessageSerializer2 extends MessageToByteEncoder<ByteBuf> {
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        int i = in.readableBytes();
        int j = PacketBuffer.getVarIntSize(i);

        if (j > 3) {
            throw new IllegalArgumentException("unable to fit " + i + " into " + 3);
        } else {
            PacketBuffer buffer = new PacketBuffer(out);
            buffer.ensureWritable(j + i);
            buffer.writeVarIntToBuffer(i);
            buffer.writeBytes(in, in.readerIndex(), i);
        }
    }
}
