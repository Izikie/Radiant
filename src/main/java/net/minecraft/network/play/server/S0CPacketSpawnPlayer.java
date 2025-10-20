package net.minecraft.network.play.server;

import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class S0CPacketSpawnPlayer implements Packet<INetHandlerPlayClient> {
    private int entityId;
    private UUID uuid;
    private int x;
    private int y;
    private int z;
    private byte yaw;
    private byte pitch;
    private int currentItem;
    private DataWatcher watcher;
    private List<DataWatcher.WatchableObject> metadata;

    public S0CPacketSpawnPlayer() {
    }

    public S0CPacketSpawnPlayer(EntityPlayer player) {
        this.entityId = player.getEntityId();
        this.uuid = player.getGameProfile().getId();
        this.x = MathHelper.floor(player.posX * 32.0D);
        this.y = MathHelper.floor(player.posY * 32.0D);
        this.z = MathHelper.floor(player.posZ * 32.0D);
        this.yaw = (byte) ((int) (player.rotationYaw * 256.0F / 360.0F));
        this.pitch = (byte) ((int) (player.rotationPitch * 256.0F / 360.0F));
        ItemStack itemstack = player.inventory.getCurrentItem();
        this.currentItem = itemstack == null ? 0 : Item.getIdFromItem(itemstack.getItem());
        this.watcher = player.getDataWatcher();
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.entityId = buf.readVarIntFromBuffer();
        this.uuid = buf.readUuid();
        this.x = buf.readInt();
        this.y = buf.readInt();
        this.z = buf.readInt();
        this.yaw = buf.readByte();
        this.pitch = buf.readByte();
        this.currentItem = buf.readShort();
        this.metadata = DataWatcher.readWatchedListFromPacketBuffer(buf);
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarIntToBuffer(this.entityId);
        buf.writeUuid(this.uuid);
        buf.writeInt(this.x);
        buf.writeInt(this.y);
        buf.writeInt(this.z);
        buf.writeByte(this.yaw);
        buf.writeByte(this.pitch);
        buf.writeShort(this.currentItem);
        this.watcher.writeTo(buf);
    }

    @Override
    public void processPacket(INetHandlerPlayClient handler) {
        handler.handleSpawnPlayer(this);
    }

    public List<DataWatcher.WatchableObject> getMetaData() {
        if (this.metadata == null) {
            this.metadata = this.watcher.getAllWatched();
        }

        return this.metadata;
    }

    public int getEntityID() {
        return this.entityId;
    }

    public UUID getPlayer() {
        return this.uuid;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public byte getYaw() {
        return this.yaw;
    }

    public byte getPitch() {
        return this.pitch;
    }

    public int getCurrentItemID() {
        return this.currentItem;
    }
}
