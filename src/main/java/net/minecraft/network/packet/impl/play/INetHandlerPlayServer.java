package net.minecraft.network.packet.impl.play;

import net.minecraft.network.INetHandler;
import net.minecraft.network.packet.impl.play.client.*;

public interface INetHandlerPlayServer extends INetHandler {
    void handleAnimation(C0APacketAnimation packet);

    void processChatMessage(C01PacketChatMessage packet);

    void processTabComplete(C14PacketTabComplete packet);

    void processClientStatus(C16PacketClientStatus packet);

    void processClientSettings(C15PacketClientSettings packet);

    void processConfirmTransaction(C0FPacketConfirmTransaction packet);

    void processEnchantItem(C11PacketEnchantItem packet);

    void processClickWindow(C0EPacketClickWindow packet);

    void processCloseWindow(C0DPacketCloseWindow packet);

    void processCustomPayload(C17PacketCustomPayload packet);

    void processUseEntity(C02PacketUseEntity packet);

    void processKeepAlive(C00PacketKeepAlive packet);

    void processPlayer(C03PacketPlayer packet);

    void processPlayerAbilities(C13PacketPlayerAbilities packet);

    void processPlayerDigging(C07PacketPlayerDigging packet);

    void processEntityAction(C0BPacketEntityAction packet);

    void processInput(C0CPacketInput packet);

    void processHeldItemChange(C09PacketHeldItemChange packet);

    void processCreativeInventoryAction(C10PacketCreativeInventoryAction packet);

    void processUpdateSign(C12PacketUpdateSign packet);

    void processPlayerBlockPlacement(C08PacketPlayerBlockPlacement packet);

    void handleSpectate(C18PacketSpectate packet);

    void handleResourcePackStatus(C19PacketResourcePackStatus packet);
}
