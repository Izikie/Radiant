package net.minecraft.network.packet.impl.play.server;

import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;
import net.minecraft.util.chat.IChatComponent;

import java.io.IOException;

public class S2DPacketOpenWindow implements Packet<INetHandlerPlayClient> {
    private int windowId;
    private String inventoryType;
    private IChatComponent windowTitle;
    private int slotCount;
    private int entityId;

    public S2DPacketOpenWindow() {
    }

    public S2DPacketOpenWindow(int incomingWindowId, String incomingWindowTitle, IChatComponent windowTitleIn) {
        this(incomingWindowId, incomingWindowTitle, windowTitleIn, 0);
    }

    public S2DPacketOpenWindow(int windowIdIn, String guiId, IChatComponent windowTitleIn, int slotCountIn) {
        this.windowId = windowIdIn;
        this.inventoryType = guiId;
        this.windowTitle = windowTitleIn;
        this.slotCount = slotCountIn;
    }

    public S2DPacketOpenWindow(int windowIdIn, String guiId, IChatComponent windowTitleIn, int slotCountIn, int incomingEntityId) {
        this(windowIdIn, guiId, windowTitleIn, slotCountIn);
        this.entityId = incomingEntityId;
    }

    @Override
    public void handle(INetHandlerPlayClient handler) {
        handler.handleOpenWindow(this);
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.windowId = buf.readUnsignedByte();
        this.inventoryType = buf.readString(32);
        this.windowTitle = buf.readChatComponent();
        this.slotCount = buf.readUnsignedByte();

        if (this.inventoryType.equals("EntityHorse")) {
            this.entityId = buf.readInt();
        }
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeByte(this.windowId);
        buf.writeString(this.inventoryType);
        buf.writeChatComponent(this.windowTitle);
        buf.writeByte(this.slotCount);

        if (this.inventoryType.equals("EntityHorse")) {
            buf.writeInt(this.entityId);
        }
    }

    public int getWindowId() {
        return this.windowId;
    }

    public String getGuiId() {
        return this.inventoryType;
    }

    public IChatComponent getWindowTitle() {
        return this.windowTitle;
    }

    public int getSlotCount() {
        return this.slotCount;
    }

    public int getEntityId() {
        return this.entityId;
    }

    public boolean hasSlots() {
        return this.slotCount > 0;
    }
}
