package net.minecraft.network.login;

import net.minecraft.network.INetHandler;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.login.client.C01PacketEncryptionResponse;

public interface INetHandlerLoginServer extends INetHandler {
    void processLoginStart(C00PacketLoginStart packet);

    void processEncryptionResponse(C01PacketEncryptionResponse packet);
}
