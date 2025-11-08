package net.minecraft.network.encoder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import net.minecraft.network.packet.api.PacketBuffer;

import java.util.List;

public class MessageSplitter extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        msg.markReaderIndex();
        byte[] abyte = new byte[3];

        for (int i = 0; i < abyte.length; ++i) {
            if (!msg.isReadable()) {
                msg.resetReaderIndex();
                return;
            }

            abyte[i] = msg.readByte();

            if (abyte[i] >= 0) {
                PacketBuffer packetbuffer = new PacketBuffer(Unpooled.wrappedBuffer(abyte));

                try {
                    int j = packetbuffer.readVarIntFromBuffer();

                    if (msg.readableBytes() >= j) {
                        out.add(msg.readBytes(j));
                        return;
                    }

                    msg.resetReaderIndex();
                } finally {
                    packetbuffer.release();
                }

                return;
            }
        }

        throw new CorruptedFrameException("length wider than 21-bit");
    }
}
