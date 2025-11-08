package net.minecraft.network;

import net.minecraft.util.chat.IChatComponent;

public interface INetHandler {
    void onDisconnect(IChatComponent reason);
}
