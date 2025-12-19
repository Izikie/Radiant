package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class PingResponseHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PingResponseHandler.class);
    private final NetworkSystem networkSystem;

    public PingResponseHandler(NetworkSystem networkSystem) {
        this.networkSystem = networkSystem;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object data) {
        ByteBuf byteBuf = (ByteBuf) data;
        byteBuf.markReaderIndex();
        boolean flag = true;

        try {
            if (byteBuf.readUnsignedByte() == 254) {
                InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
                MinecraftServer server = this.networkSystem.getServer();
                int i = byteBuf.readableBytes();

                switch (i) {
                    case 0 -> {
                        LOGGER.debug("Ping: (<1.3.x) from {}:{}", address.getAddress(), address.getPort());
                        String format = String.format("%s\u00a7%d\u00a7%d", server.getMOTD(), server.getCurrentPlayerCount(), server.getMaxPlayers());
                        this.writeAndFlush(ctx, this.getStringBuffer(format));
                    }
                    case 1 -> {
                        if (byteBuf.readUnsignedByte() != 1) {
                            return;
                        }

                        LOGGER.debug("Ping: (1.4-1.5.x) from {}:{}", address.getAddress(), address.getPort());
                        String format = String.format("\u00a71\u0000%d\u0000%s\u0000%s\u0000%d\u0000%d", 127, server.getMinecraftVersion(), server.getMOTD(), server.getCurrentPlayerCount(), server.getMaxPlayers());
                        this.writeAndFlush(ctx, this.getStringBuffer(format));
                    }
                    default -> {
                        boolean flag1 = byteBuf.readUnsignedByte() == 1;
                        flag1 = flag1 & byteBuf.readUnsignedByte() == 250;
                        flag1 = flag1 & "MC|PingHost".equals(new String(byteBuf.readBytes(byteBuf.readShort() * 2).array(), StandardCharsets.UTF_16BE));
                        int j = byteBuf.readUnsignedShort();
                        flag1 = flag1 & byteBuf.readUnsignedByte() >= 73;
                        flag1 = flag1 & 3 + byteBuf.readBytes(byteBuf.readShort() * 2).array().length + 4 == j;
                        flag1 = flag1 & byteBuf.readInt() <= 65535;
                        flag1 = flag1 & byteBuf.readableBytes() == 0;

                        if (!flag1) {
                            return;
                        }

                        LOGGER.debug("Ping: (1.6) from {}:{}", address.getAddress(), address.getPort());
                        String format = String.format("\u00a71\u0000%d\u0000%s\u0000%s\u0000%d\u0000%d", 127, server.getMinecraftVersion(), server.getMOTD(), server.getCurrentPlayerCount(), server.getMaxPlayers());
                        ByteBuf bytebuf1 = this.getStringBuffer(format);

                        try {
                            this.writeAndFlush(ctx, bytebuf1);
                        } finally {
                            bytebuf1.release();
                        }
                    }
                }

                byteBuf.release();
                flag = false;
            }
        } catch (RuntimeException _) {
        } finally {
            if (flag) {
                byteBuf.resetReaderIndex();
                ctx.channel().pipeline().remove("legacy_query");
                ctx.fireChannelRead(data);
            }
        }
    }

    private void writeAndFlush(ChannelHandlerContext ctx, ByteBuf data) {
        ctx.pipeline().firstContext().writeAndFlush(data).addListener(ChannelFutureListener.CLOSE);
    }

    private ByteBuf getStringBuffer(String string) {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(255);
        char[] chars = string.toCharArray();
        byteBuf.writeShort(chars.length);

        for (char character : chars) {
            byteBuf.writeChar(character);
        }

        return byteBuf;
    }
}
