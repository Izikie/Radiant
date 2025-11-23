package net.minecraft.network.encoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
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
import java.util.List;

public class MessageDeserializer extends ByteToMessageDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageDeserializer.class);
    private static final Marker RECEIVED_PACKET_MARKER = MarkerFactory.getMarker("PACKET_RECEIVED");

    private final PacketDirection direction;

    public MessageDeserializer(PacketDirection direction) {
        this.direction = direction;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf payload, List<Object> out) throws Exception {
        if (!payload.isReadable()) {
            return;
        }

        PacketBuffer buffer = new PacketBuffer(payload);
        int packetId = buffer.readVarInt();

        NetworkState state = ctx.channel()
                .attr(NetworkManager.ATTR_KEY_CONNECTION_STATE)
                .get();

        if (state == null) {
            throw new IOException("Connection state is null, cannot deserialize packet");
        }

        Packet<?> packet = state.getPacket(this.direction, packetId);

        if (packet == null) {
            throw new IOException("Unregistered packet id " + packetId + ", cannot deserialize packet");
        }

        packet.read(buffer);

        if (buffer.isReadable()) {
            throw new IOException(
                    "Packet " + state.getId() + "/" + packetId + " (" + packet.getClass().getSimpleName() +
                            ") has " + buffer.readableBytes() + " unexpected extra bytes"
            );
        }

        out.add(packet);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_PACKET_MARKER, " IN: [{}:{}] {}", state, packetId, packet.getClass().getName());
        }
    }

    static {
        RECEIVED_PACKET_MARKER.add(NetworkManager.LOG_MARKER_PACKETS);
    }
}
