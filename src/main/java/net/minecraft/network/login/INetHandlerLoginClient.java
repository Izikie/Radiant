package net.minecraft.network.login;

import net.minecraft.network.INetHandler;
import net.minecraft.network.login.server.S00PacketDisconnect;
import net.minecraft.network.login.server.S01PacketEncryptionRequest;
import net.minecraft.network.login.server.S02PacketLoginSuccess;
import net.minecraft.network.login.server.S03PacketEnableCompression;

public interface INetHandlerLoginClient extends INetHandler {
    void handleEncryptionRequest(S01PacketEncryptionRequest packet);

    void handleLoginSuccess(S02PacketLoginSuccess packet);

    void handleDisconnect(S00PacketDisconnect packet);

    void handleEnableCompression(S03PacketEnableCompression packet);
}
