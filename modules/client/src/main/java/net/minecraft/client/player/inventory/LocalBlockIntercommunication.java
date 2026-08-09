package net.minecraft.client.player.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.chat.IChatComponent;
import net.minecraft.world.IInteractionObject;

public record LocalBlockIntercommunication(String guiID, IChatComponent displayName) implements IInteractionObject {

    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return this.displayName.getUnformattedText();
    }

    @Override
    public boolean hasCustomName() {
        return true;
    }
}
