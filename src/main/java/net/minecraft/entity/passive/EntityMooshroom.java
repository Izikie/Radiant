package net.minecraft.entity.passive;

import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ParticleTypes;
import net.minecraft.world.World;

public class EntityMooshroom extends EntityCow {
    public EntityMooshroom(World worldIn) {
        super(worldIn);
        this.setSize(0.9F, 1.3F);
        this.spawnableBlock = Blocks.MYCELIUM;
    }

    public boolean interact(EntityPlayer player) {
        ItemStack itemstack = player.inventory.getCurrentItem();

        if (itemstack != null && itemstack.getItem() == Items.BOWL && this.getGrowingAge() >= 0) {
            if (itemstack.stackSize == 1) {
                player.inventory.setInventorySlotContents(player.inventory.currentItem, new ItemStack(Items.MUSHROOM_STEW));
                return true;
            }

            if (player.inventory.addItemStackToInventory(new ItemStack(Items.MUSHROOM_STEW)) && !player.capabilities.isCreativeMode) {
                player.inventory.decrStackSize(player.inventory.currentItem, 1);
                return true;
            }
        }

        if (itemstack != null && itemstack.getItem() == Items.SHEARS && this.getGrowingAge() >= 0) {
            this.setDead();
            this.worldObj.spawnParticle(ParticleTypes.EXPLOSION_LARGE, this.posX, this.posY + (this.height / 2.0F), this.posZ, 0.0D, 0.0D, 0.0D);

            if (!this.worldObj.isRemote) {
                EntityCow entitycow = new EntityCow(this.worldObj);
                entitycow.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
                entitycow.setHealth(this.getHealth());
                entitycow.renderYawOffset = this.renderYawOffset;

                if (this.hasCustomName()) {
                    entitycow.setCustomNameTag(this.getCustomNameTag());
                }

                this.worldObj.spawnEntityInWorld(entitycow);

                for (int i = 0; i < 5; ++i) {
                    this.worldObj.spawnEntityInWorld(new EntityItem(this.worldObj, this.posX, this.posY + this.height, this.posZ, new ItemStack(Blocks.RED_MUSHROOM)));
                }

                itemstack.damageItem(1, player);
                this.playSound("mob.sheep.shear", 1.0F, 1.0F);
            }

            return true;
        } else {
            return super.interact(player);
        }
    }

    public EntityMooshroom createChild(EntityAgeable ageable) {
        return new EntityMooshroom(this.worldObj);
    }
}
