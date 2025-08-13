package net.minecraft.network.encoder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.packet.PacketBuffer;

import java.util.List;
import java.util.zip.Inflater;

public class NettyCompressionDecoder extends ByteToMessageDecoder {
    private final Inflater inflater;
    private int threshold;

    public NettyCompressionDecoder(int threshold) {
        this.threshold = threshold;
        this.inflater = new Inflater();
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() == 0)
            return;

        PacketBuffer buffer = new PacketBuffer(in);
        int varInt = buffer.readVarIntFromBuffer();

        if (varInt == 0) {
            out.add(buffer.readBytes(buffer.readableBytes()));
        } else {
            if (varInt < this.threshold) {
                throw new DecoderException("Badly compressed packet - size of " + varInt + " is below server threshold of " + this.threshold);
            }

            if (varInt > 2097152) {
                throw new DecoderException("Badly compressed packet - size of " + varInt + " is larger than protocol maximum of " + 2097152);
            }

            byte[] compressedData = new byte[buffer.readableBytes()];
            buffer.readBytes(compressedData);
            this.inflater.setInput(compressedData);

            byte[] decompressedData = new byte[varInt];
            this.inflater.inflate(decompressedData);
            out.add(Unpooled.wrappedBuffer(decompressedData));

            this.inflater.reset();
        }
    }

    public void setCompressionThreshold(int threshold) {
        this.threshold = threshold;
    }
}
