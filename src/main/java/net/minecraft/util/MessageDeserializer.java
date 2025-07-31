package net.minecraft.util;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.PacketDirection;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.IOException;
import java.util.List;

public class MessageDeserializer extends ByteToMessageDecoder {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageDeserializer.class);
    private static final Marker RECEIVED_PACKET_MARKER = MarkerFactory.getMarker("PACKET_RECEIVED");
    private final PacketDirection direction;

    public MessageDeserializer(PacketDirection direction) {
        this.direction = direction;
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf p_decode_2_, List<Object> p_decode_3_) throws Exception {
        if (p_decode_2_.readableBytes() != 0) {
            PacketBuffer buffer = new PacketBuffer(p_decode_2_);
            int i = buffer.readVarIntFromBuffer();
            Packet<?> packet = ctx.channel().attr(NetworkManager.ATTR_KEY_CONNECTION_STATE).get().getPacket(this.direction, i);

            if (packet == null) {
                throw new IOException("Bad packet id " + i);
            } else {
                packet.readPacketData(buffer);

                if (buffer.readableBytes() > 0) {
                    throw new IOException("Packet " + ctx.channel().attr(NetworkManager.ATTR_KEY_CONNECTION_STATE).get().getId() + "/" + i + " (" + packet.getClass().getSimpleName() + ") was larger than I expected, found " + buffer.readableBytes() + " bytes extra whilst reading packet " + i);
                } else {
                    p_decode_3_.add(packet);

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(RECEIVED_PACKET_MARKER, " IN: [{}:{}] {}", new Object[]{ctx.channel().attr(NetworkManager.ATTR_KEY_CONNECTION_STATE).get(), i, packet.getClass().getName()});
                    }
                }
            }
        }
    }

    static {
        RECEIVED_PACKET_MARKER.add(NetworkManager.LOG_MARKER_PACKETS);
    }
}
