package net.minecraft.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.chat.ChatComponentText;
import net.minecraft.util.chat.ChatComponentTranslation;
import net.minecraft.util.chat.IChatComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InventoryBasic implements IInventory {
    private String inventoryTitle;
    private final int slotsCount;
    private final ItemStack[] inventoryContents;
    private List<IInvBasic> changeListeners;
    private boolean hasCustomName;

    public InventoryBasic(String title, boolean customName, int slotCount) {
        this.inventoryTitle = title;
        this.hasCustomName = customName;
        this.slotsCount = slotCount;
        this.inventoryContents = new ItemStack[slotCount];
    }

    public InventoryBasic(IChatComponent title, int slotCount) {
        this(title.getUnformattedText(), true, slotCount);
    }

    public void addInventoryChangeListener(IInvBasic listener) {
        if (this.changeListeners == null) {
            this.changeListeners = new ArrayList<>();
        }

        this.changeListeners.add(listener);
    }

    public void removeInventoryChangeListener(IInvBasic listener) {
        this.changeListeners.remove(listener);
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return index >= 0 && index < this.inventoryContents.length ? this.inventoryContents[index] : null;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        if (this.inventoryContents[index] != null) {
            if (this.inventoryContents[index].stackSize <= count) {
                ItemStack itemstack1 = this.inventoryContents[index];
                this.inventoryContents[index] = null;
                this.markDirty();
                return itemstack1;
            } else {
                ItemStack itemstack = this.inventoryContents[index].splitStack(count);

                if (this.inventoryContents[index].stackSize == 0) {
                    this.inventoryContents[index] = null;
                }

                this.markDirty();
                return itemstack;
            }
        } else {
            return null;
        }
    }

    public ItemStack func_174894_a(ItemStack stack) {
        ItemStack itemstack = stack.copy();

        for (int i = 0; i < this.slotsCount; ++i) {
            ItemStack itemstack1 = this.getStackInSlot(i);

            if (itemstack1 == null) {
                this.setInventorySlotContents(i, itemstack);
                this.markDirty();
                return null;
            }

            if (ItemStack.areItemsEqual(itemstack1, itemstack)) {
                int j = Math.min(this.getInventoryStackLimit(), itemstack1.getMaxStackSize());
                int k = Math.min(itemstack.stackSize, j - itemstack1.stackSize);

                if (k > 0) {
                    itemstack1.stackSize += k;
                    itemstack.stackSize -= k;

                    if (itemstack.stackSize <= 0) {
                        this.markDirty();
                        return null;
                    }
                }
            }
        }

        if (itemstack.stackSize != stack.stackSize) {
            this.markDirty();
        }

        return itemstack;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        if (this.inventoryContents[index] != null) {
            ItemStack itemstack = this.inventoryContents[index];
            this.inventoryContents[index] = null;
            return itemstack;
        } else {
            return null;
        }
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        this.inventoryContents[index] = stack;

        if (stack != null && stack.stackSize > this.getInventoryStackLimit()) {
            stack.stackSize = this.getInventoryStackLimit();
        }

        this.markDirty();
    }

    @Override
    public int getSizeInventory() {
        return this.slotsCount;
    }

    @Override
    public String getName() {
        return this.inventoryTitle;
    }

    @Override
    public boolean hasCustomName() {
        return this.hasCustomName;
    }

    public void setCustomName(String inventoryTitleIn) {
        this.hasCustomName = true;
        this.inventoryTitle = inventoryTitleIn;
    }

    @Override
    public IChatComponent getDisplayName() {
        return this.hasCustomName() ? new ChatComponentText(this.getName()) : new ChatComponentTranslation(this.getName());
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
        if (this.changeListeners != null) {
            for (IInvBasic changeListener : this.changeListeners) {
                changeListener.onInventoryChanged(this);
            }
        }
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory(EntityPlayer player) {
    }

    @Override
    public void closeInventory(EntityPlayer player) {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        Arrays.fill(this.inventoryContents, null);
    }
}
