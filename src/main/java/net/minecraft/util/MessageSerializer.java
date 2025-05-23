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

public class MessageSerializer extends MessageToByteEncoder<Packet> {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker RECEIVED_PACKET_MARKER = MarkerManager.getMarker("PACKET_SENT").addParents(NetworkManager.LOG_MARKER_PACKETS);
    private final PacketDirection direction;

    public MessageSerializer(PacketDirection direction) {
        this.direction = direction;
    }

    protected void encode(ChannelHandlerContext p_encode_1_, Packet p_encode_2_, ByteBuf p_encode_3_) throws Exception {
        Integer integer = p_encode_1_.channel().attr(NetworkManager.ATTR_KEY_CONNECTION_STATE).get().getPacketId(this.direction, p_encode_2_);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_PACKET_MARKER, "OUT: [{}:{}] {}", new Object[]{p_encode_1_.channel().attr(NetworkManager.ATTR_KEY_CONNECTION_STATE).get(), integer, p_encode_2_.getClass().getName()});
        }

        if (integer == null) {
            throw new IOException("Can't serialize unregistered packet");
        } else {
            PacketBuffer packetbuffer = new PacketBuffer(p_encode_3_);
            packetbuffer.writeVarIntToBuffer(integer);

            try {
                p_encode_2_.writePacketData(packetbuffer);
            } catch (Throwable throwable) {
                LOGGER.error(throwable);
            }
        }
    }
}
