package net.minecraft.network;

import net.minecraft.util.chat.IChatComponent;

public interface INetHandler {
    default boolean acceptsQueuedPackets() {
        return true;
    }

    void onDisconnect(IChatComponent reason);
}
