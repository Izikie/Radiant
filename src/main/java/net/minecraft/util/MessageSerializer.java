package net.minecraft.util;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.PacketDirection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.IOException;

public class MessageSerializer extends MessageToByteEncoder<Packet<?>> {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker RECEIVED_PACKET_MARKER = MarkerManager.getMarker("PACKET_SENT").addParents(NetworkManager.LOG_MARKER_PACKETS);
    private final PacketDirection direction;

    public MessageSerializer(PacketDirection direction) {
        this.direction = direction;
    }

    protected void encode(ChannelHandlerContext ctx, Packet<?> packet, ByteBuf out) throws Exception {
        Integer integer = ctx.channel().attr(NetworkManager.ATTR_KEY_CONNECTION_STATE).get().getPacketId(this.direction, packet);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_PACKET_MARKER, "OUT: [{}:{}] {}", new Object[]{ctx.channel().attr(NetworkManager.ATTR_KEY_CONNECTION_STATE).get(), integer, packet.getClass().getName()});
        }

        if (integer == null) {
            throw new IOException("Can't serialize unregistered packet");
        } else {
            PacketBuffer buffer = new PacketBuffer(out);
            buffer.writeVarIntToBuffer(integer);

            try {
                packet.writePacketData(buffer);
            } catch (Throwable throwable) {
                LOGGER.error(throwable);
            }
        }
    }
}
