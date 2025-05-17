package net.minecraft.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.stats.AchievementList;

public class SlotCrafting extends Slot {
    private final InventoryCrafting craftMatrix;
    private final EntityPlayer thePlayer;
    private int amountCrafted;

    public SlotCrafting(EntityPlayer player, InventoryCrafting craftingInventory, IInventory p_i45790_3_, int slotIndex, int xPosition, int yPosition) {
        super(p_i45790_3_, slotIndex, xPosition, yPosition);
        this.thePlayer = player;
        this.craftMatrix = craftingInventory;
    }

    public boolean isItemValid(ItemStack stack) {
        return false;
    }

    public ItemStack decrStackSize(int amount) {
        if (this.getHasStack()) {
            this.amountCrafted += Math.min(amount, this.getStack().stackSize);
        }

        return super.decrStackSize(amount);
    }

    protected void onCrafting(ItemStack stack, int amount) {
        this.amountCrafted += amount;
        this.onCrafting(stack);
    }

    protected void onCrafting(ItemStack stack) {
        if (this.amountCrafted > 0) {
            stack.onCrafting(this.thePlayer.worldObj, this.thePlayer, this.amountCrafted);
        }

        this.amountCrafted = 0;

        if (stack.getItem() == Item.getItemFromBlock(Blocks.CRAFTING_TABLE)) {
            this.thePlayer.triggerAchievement(AchievementList.BUILD_WORK_BENCH);
        }

        if (stack.getItem() instanceof ItemPickaxe) {
            this.thePlayer.triggerAchievement(AchievementList.BUILD_PICKAXE);
        }

        if (stack.getItem() == Item.getItemFromBlock(Blocks.FURNACE)) {
            this.thePlayer.triggerAchievement(AchievementList.BUILD_FURNACE);
        }

        if (stack.getItem() instanceof ItemHoe) {
            this.thePlayer.triggerAchievement(AchievementList.BUILD_HOE);
        }

        if (stack.getItem() == Items.BREAD) {
            this.thePlayer.triggerAchievement(AchievementList.MAKE_BREAD);
        }

        if (stack.getItem() == Items.CAKE) {
            this.thePlayer.triggerAchievement(AchievementList.BAKE_CAKE);
        }

        if (stack.getItem() instanceof ItemPickaxe itemPickaxe && itemPickaxe.getToolMaterial() != Item.ToolMaterial.WOOD) {
            this.thePlayer.triggerAchievement(AchievementList.BUILD_BETTER_PICKAXE);
        }

        if (stack.getItem() instanceof ItemSword) {
            this.thePlayer.triggerAchievement(AchievementList.BUILD_SWORD);
        }

        if (stack.getItem() == Item.getItemFromBlock(Blocks.ENCHANTING_TABLE)) {
            this.thePlayer.triggerAchievement(AchievementList.ENCHANTMENTS);
        }

        if (stack.getItem() == Item.getItemFromBlock(Blocks.BOOKSHELF)) {
            this.thePlayer.triggerAchievement(AchievementList.BOOKCASE);
        }

        if (stack.getItem() == Items.GOLDEN_APPLE && stack.getMetadata() == 1) {
            this.thePlayer.triggerAchievement(AchievementList.OVERPOWERED);
        }
    }

    public void onPickupFromSlot(EntityPlayer playerIn, ItemStack stack) {
        this.onCrafting(stack);
        ItemStack[] aitemstack = CraftingManager.getInstance().func_180303_b(this.craftMatrix, playerIn.worldObj);

        for (int i = 0; i < aitemstack.length; ++i) {
            ItemStack itemstack = this.craftMatrix.getStackInSlot(i);
            ItemStack itemstack1 = aitemstack[i];

            if (itemstack != null) {
                this.craftMatrix.decrStackSize(i, 1);
            }

            if (itemstack1 != null) {
                if (this.craftMatrix.getStackInSlot(i) == null) {
                    this.craftMatrix.setInventorySlotContents(i, itemstack1);
                } else if (!this.thePlayer.inventory.addItemStackToInventory(itemstack1)) {
                    this.thePlayer.dropPlayerItemWithRandomChoice(itemstack1, false);
                }
            }
        }
    }
}
