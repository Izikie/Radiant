package net.minecraft.item;

import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.*;
import net.minecraft.util.input.MovingObjectPosition;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.List;

public class ItemMonsterPlacer extends Item {
    public ItemMonsterPlacer() {
        this.setHasSubtypes(true);
        this.setCreativeTab(CreativeTabs.TAB_MISC);
    }

    public String getItemStackDisplayName(ItemStack stack) {
        String s = (StatCollector.translateToLocal(this.getUnlocalizedName() + ".name")).trim();
        String s1 = EntityList.getStringFromID(stack.getMetadata());

        if (s1 != null) {
            s = s + " " + StatCollector.translateToLocal("entity." + s1 + ".name");
        }

        return s;
    }

    public int getColorFromItemStack(ItemStack stack, int renderPass) {
        EntityList.EntityEggInfo entitylist$entityegginfo = EntityList.entityEggs.get(stack.getMetadata());
        return entitylist$entityegginfo != null ? (renderPass == 0 ? entitylist$entityegginfo.primaryColor : entitylist$entityegginfo.secondaryColor) : 16777215;
    }

    public boolean onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, Direction side, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) {
            return true;
        } else if (!playerIn.canPlayerEdit(pos.offset(side), side, stack)) {
            return false;
        } else {
            IBlockState iblockstate = worldIn.getBlockState(pos);

            if (iblockstate.getBlock() == Blocks.MOB_SPAWNER) {
                TileEntity tileentity = worldIn.getTileEntity(pos);

                if (tileentity instanceof TileEntityMobSpawner tileEntityMobSpawner) {
                    MobSpawnerBaseLogic mobspawnerbaselogic = tileEntityMobSpawner.getSpawnerBaseLogic();
                    mobspawnerbaselogic.setEntityName(EntityList.getStringFromID(stack.getMetadata()));
                    tileentity.markDirty();
                    worldIn.markBlockForUpdate(pos);

                    if (!playerIn.capabilities.isCreativeMode) {
                        --stack.stackSize;
                    }

                    return true;
                }
            }

            pos = pos.offset(side);
            double d0 = 0.0D;

            if (side == Direction.UP && iblockstate instanceof BlockFence) {
                d0 = 0.5D;
            }

            Entity entity = spawnCreature(worldIn, stack.getMetadata(), pos.getX() + 0.5D, pos.getY() + d0, pos.getZ() + 0.5D);

            if (entity != null) {
                if (entity instanceof EntityLivingBase && stack.hasDisplayName()) {
                    entity.setCustomNameTag(stack.getDisplayName());
                }

                if (!playerIn.capabilities.isCreativeMode) {
                    --stack.stackSize;
                }
            }

            return true;
        }
    }

    public ItemStack onItemRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn) {
        if (!worldIn.isRemote) {
            MovingObjectPosition movingobjectposition = this.getMovingObjectPositionFromPlayer(worldIn, playerIn, true);

            if (movingobjectposition != null) {
                if (movingobjectposition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    BlockPos blockpos = movingobjectposition.getBlockPos();

                    if (!worldIn.isBlockModifiable(playerIn, blockpos)) {
                        return itemStackIn;
                    }

                    if (!playerIn.canPlayerEdit(blockpos, movingobjectposition.sideHit, itemStackIn)) {
                        return itemStackIn;
                    }

                    if (worldIn.getBlockState(blockpos).getBlock() instanceof BlockLiquid) {
                        Entity entity = spawnCreature(worldIn, itemStackIn.getMetadata(), blockpos.getX() + 0.5D, blockpos.getY() + 0.5D, blockpos.getZ() + 0.5D);

                        if (entity != null) {
                            if (entity instanceof EntityLivingBase && itemStackIn.hasDisplayName()) {
                                entity.setCustomNameTag(itemStackIn.getDisplayName());
                            }

                            if (!playerIn.capabilities.isCreativeMode) {
                                --itemStackIn.stackSize;
                            }

                            playerIn.triggerAchievement(StatList.OBJECT_USE_STATS[Item.getIdFromItem(this)]);
                        }
                    }
                }

            }
        }
        return itemStackIn;
    }

    public static Entity spawnCreature(World worldIn, int entityID, double x, double y, double z) {
        if (!EntityList.entityEggs.containsKey(entityID)) {
            return null;
        } else {
            Entity entity = null;

            for (int i = 0; i < 1; ++i) {
                entity = EntityList.createEntityByID(entityID, worldIn);

                if (entity instanceof EntityLivingBase) {
                    EntityLiving entityliving = (EntityLiving) entity;
                    entity.setLocationAndAngles(x, y, z, MathHelper.wrapAngle(worldIn.rand.nextFloat() * 360.0F), 0.0F);
                    entityliving.rotationYawHead = entityliving.rotationYaw;
                    entityliving.renderYawOffset = entityliving.rotationYaw;
                    entityliving.onInitialSpawn(worldIn.getDifficultyForLocation(new BlockPos(entityliving)), null);
                    worldIn.spawnEntityInWorld(entity);
                    entityliving.playLivingSound();
                }
            }

            return entity;
        }
    }

    public void getSubItems(Item itemIn, CreativeTabs tab, List<ItemStack> subItems) {
        for (EntityList.EntityEggInfo entitylist$entityegginfo : EntityList.entityEggs.values()) {
            subItems.add(new ItemStack(itemIn, 1, entitylist$entityegginfo.spawnedID));
        }
    }
}
