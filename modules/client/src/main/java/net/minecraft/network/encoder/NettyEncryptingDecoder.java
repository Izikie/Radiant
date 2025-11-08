package net.minecraft.network.encoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import javax.crypto.Cipher;
import java.util.List;

public class NettyEncryptingDecoder extends MessageToMessageDecoder<ByteBuf> {
    private final NettyEncryptionTranslator decryptionCodec;

    public NettyEncryptingDecoder(Cipher cipher) {
        this.decryptionCodec = new NettyEncryptionTranslator(cipher);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        out.add(this.decryptionCodec.decipher(ctx, in));
    }
}
