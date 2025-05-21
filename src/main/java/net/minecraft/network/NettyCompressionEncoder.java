package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.zip.Deflater;

public class NettyCompressionEncoder extends MessageToByteEncoder<ByteBuf> {
    private final byte[] buffer = new byte[8192];
    private final Deflater deflater;
    private int threshold;

    public NettyCompressionEncoder(int threshold) {
        this.threshold = threshold;
        this.deflater = new Deflater();
    }

    protected void encode(ChannelHandlerContext context, ByteBuf in, ByteBuf out) throws Exception {
        int length = in.readableBytes();
        PacketBuffer pBuffer = new PacketBuffer(out);

        if (length < this.threshold) {
            pBuffer.writeVarIntToBuffer(0);
            pBuffer.writeBytes(in);
        } else {
            byte[] decompressedData = new byte[length];
            in.readBytes(decompressedData);
            pBuffer.writeVarIntToBuffer(decompressedData.length);
            this.deflater.setInput(decompressedData, 0, length);
            this.deflater.finish();

            while (!this.deflater.finished()) {
                int compressed = this.deflater.deflate(this.buffer);
                pBuffer.writeBytes(this.buffer, 0, compressed);
            }

            this.deflater.reset();
        }
    }

    public void setCompressionThreshold(int threshold) {
        this.threshold = threshold;
    }
}
