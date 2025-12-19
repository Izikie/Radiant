package net.minecraft.network.packet.impl.status.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.status.INetHandlerStatusClient;
import net.minecraft.util.chat.ChatStyle;
import net.minecraft.util.chat.IChatComponent;
import net.minecraft.util.json.EnumTypeAdapterFactory;

import java.io.IOException;

public class S00PacketServerInfo implements Packet<INetHandlerStatusClient> {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ServerStatusResponse.MinecraftProtocolVersionIdentifier.class, new ServerStatusResponse.MinecraftProtocolVersionIdentifier.Serializer())
            .registerTypeAdapter(ServerStatusResponse.PlayerCountData.class, new ServerStatusResponse.PlayerCountData.Serializer())
            .registerTypeAdapter(ServerStatusResponse.class, new ServerStatusResponse.Serializer())
            .registerTypeHierarchyAdapter(IChatComponent.class, new IChatComponent.Serializer())
            .registerTypeHierarchyAdapter(ChatStyle.class, new ChatStyle.Serializer())
            .registerTypeAdapterFactory(new EnumTypeAdapterFactory())
            .create();
    private ServerStatusResponse response;

    public S00PacketServerInfo() {
    }

    public S00PacketServerInfo(ServerStatusResponse responseIn) {
        this.response = responseIn;
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.response = GSON.fromJson(buf.readString(32767), ServerStatusResponse.class);
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeString(GSON.toJson(this.response));
    }

    @Override
    public void handle(INetHandlerStatusClient handler) {
        handler.handleServerInfo(this);
    }

    public ServerStatusResponse getResponse() {
        return this.response;
    }
}
