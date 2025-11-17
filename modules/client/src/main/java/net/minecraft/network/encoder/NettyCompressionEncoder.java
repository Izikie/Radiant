package net.minecraft.network.encoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.network.packet.api.PacketBuffer;

import java.util.zip.Deflater;

public class NettyCompressionEncoder extends MessageToByteEncoder<ByteBuf> {
    private final byte[] buffer = new byte[8192];
    private final Deflater deflater;
    private int threshold;

    public NettyCompressionEncoder(int threshold) {
        this.threshold = threshold;
        this.deflater = new Deflater();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) {
        int length = in.readableBytes();
        PacketBuffer buffer = new PacketBuffer(out);

        if (length < this.threshold) {
            buffer.writeVarInt(0);
            buffer.writeBytes(in);
        } else {
            byte[] decompressedData = new byte[length];
            in.readBytes(decompressedData);
            buffer.writeVarInt(decompressedData.length);
            this.deflater.setInput(decompressedData, 0, length);
            this.deflater.finish();

            while (!this.deflater.finished()) {
                int compressed = this.deflater.deflate(this.buffer);
                buffer.writeBytes(this.buffer, 0, compressed);
            }

            this.deflater.reset();
        }
    }

    public void setCompressionThreshold(int threshold) {
        this.threshold = threshold;
    }
}
