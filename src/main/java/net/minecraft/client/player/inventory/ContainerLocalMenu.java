package net.minecraft.client.player.inventory;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.util.chat.IChatComponent;
import net.minecraft.world.ILockableContainer;
import net.minecraft.world.LockCode;

public class ContainerLocalMenu extends InventoryBasic implements ILockableContainer {
    private final String guiID;
    private final Int2IntOpenHashMap field_174895_b = new Int2IntOpenHashMap();

    public ContainerLocalMenu(String id, IChatComponent title, int slotCount) {
        super(title, slotCount);
        this.guiID = id;
    }

    @Override
    public int getField(int id) {
        return this.field_174895_b.getOrDefault(id, 0);
    }

    @Override
    public void setField(int id, int value) {
        this.field_174895_b.put(id, value);
    }

    @Override
    public int getFieldCount() {
        return this.field_174895_b.size();
    }

    @Override
    public boolean isLocked() {
        return false;
    }

    @Override
    public void setLockCode(LockCode code) {
    }

    @Override
    public LockCode getLockCode() {
        return LockCode.EMPTY_CODE;
    }

    @Override
    public String getGuiID() {
        return this.guiID;
    }

    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {
        throw new UnsupportedOperationException();
    }
}
