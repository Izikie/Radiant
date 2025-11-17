package net.minecraft.network.encoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.NetworkState;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.api.PacketDirection;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.IOException;

public class MessageSerializer extends MessageToByteEncoder<Packet<?>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageSerializer.class);
    private static final Marker SEND_PACKET_MARKER = MarkerFactory.getMarker("PACKET_SENT");

    private final PacketDirection direction;

    public MessageSerializer(PacketDirection direction) {
        this.direction = direction;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet<?> packet, ByteBuf out) throws Exception {
        NetworkState state = ctx.channel()
                .attr(NetworkManager.ATTR_KEY_CONNECTION_STATE)
                .get();

        if (state == null) {
            throw new IOException("Connection state is null, cannot serialize packet");
        }

        Integer packetId = state.getPacketId(this.direction, packet);

        if (packet == null) {
            throw new IOException("Unregistered packet id " + packetId + ", cannot serialize packet");
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(SEND_PACKET_MARKER, "OUT: [{}:{}] {}", state, packetId, packet.getClass().getName());
        }

        PacketBuffer buffer = new PacketBuffer(out);
        buffer.writeVarInt(packetId);

        try {
            packet.write(buffer);
        } catch (Throwable throwable) {
            LOGGER.error("Failed to serialize packet", throwable);
        }
    }

    static {
        SEND_PACKET_MARKER.add(NetworkManager.LOG_MARKER_PACKETS);
    }
}
