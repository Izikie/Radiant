package net.minecraft.network.encoder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.packet.api.PacketBuffer;

import java.util.List;
import java.util.zip.Inflater;

public class NettyCompressionDecoder extends ByteToMessageDecoder {
    private static final int MAX_SIZE = 2097152;
    private final Inflater inflater;
    private int threshold;

    public NettyCompressionDecoder(int threshold) {
        this.threshold = threshold;
        this.inflater = new Inflater();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (!in.isReadable())
            return;

        PacketBuffer buffer = new PacketBuffer(in);
        int varInt = buffer.readVarIntFromBuffer();

        if (varInt == 0) {
            out.add(buffer.readBytes(buffer.readableBytes()));
            return;
        }

        if (varInt < threshold) {
            throw new DecoderException("Badly compressed packet - size of " + varInt + " is below server threshold of " + threshold);
        }

        if (varInt > MAX_SIZE) {
            throw new DecoderException("Badly compressed packet - size of " + varInt + " is larger than protocol maximum of " + MAX_SIZE);
        }

        byte[] compressedData = new byte[buffer.readableBytes()];
        buffer.readBytes(compressedData);
        this.inflater.setInput(compressedData);

        byte[] decompressedData = new byte[varInt];
        this.inflater.inflate(decompressedData);
        out.add(Unpooled.wrappedBuffer(decompressedData));

        this.inflater.reset();
    }

    public void setCompressionThreshold(int threshold) {
        this.threshold = threshold;
    }
}
