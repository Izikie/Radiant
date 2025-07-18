package net.minecraft.block;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBanner;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class BlockCauldron extends Block {
    public static final PropertyInteger LEVEL = PropertyInteger.create("level", 0, 3);

    public BlockCauldron() {
        super(Material.IRON, MapColor.STONE_COLOR);
        this.setDefaultState(this.blockState.getBaseState().withProperty(LEVEL, 0));
    }

    public void addCollisionBoxesToList(World worldIn, BlockPos pos, IBlockState state, AxisAlignedBB mask, List<AxisAlignedBB> list, Entity collidingEntity) {
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.3125F, 1.0F);
        super.addCollisionBoxesToList(worldIn, pos, state, mask, list, collidingEntity);
        float f = 0.125F;
        this.setBlockBounds(0.0F, 0.0F, 0.0F, f, 1.0F, 1.0F);
        super.addCollisionBoxesToList(worldIn, pos, state, mask, list, collidingEntity);
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, f);
        super.addCollisionBoxesToList(worldIn, pos, state, mask, list, collidingEntity);
        this.setBlockBounds(1.0F - f, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        super.addCollisionBoxesToList(worldIn, pos, state, mask, list, collidingEntity);
        this.setBlockBounds(0.0F, 0.0F, 1.0F - f, 1.0F, 1.0F, 1.0F);
        super.addCollisionBoxesToList(worldIn, pos, state, mask, list, collidingEntity);
        this.setBlockBoundsForItemRender();
    }

    public void setBlockBoundsForItemRender() {
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }

    public boolean isOpaqueCube() {
        return false;
    }

    public boolean isFullCube() {
        return false;
    }

    public void onEntityCollidedWithBlock(World worldIn, BlockPos pos, IBlockState state, Entity entityIn) {
        int i = state.getValue(LEVEL);
        float f = pos.getY() + (6.0F + (3 * i)) / 16.0F;

        if (!worldIn.isRemote && entityIn.isBurning() && i > 0 && entityIn.getEntityBoundingBox().minY <= f) {
            entityIn.extinguish();
            this.setWaterLevel(worldIn, pos, state, i - 1);
        }
    }

    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, Direction side, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) {
            return true;
        } else {
            ItemStack itemstack = playerIn.inventory.getCurrentItem();

            if (itemstack == null) {
                return true;
            } else {
                int i = state.getValue(LEVEL);
                Item item = itemstack.getItem();

                if (item == Items.WATER_BUCKET) {
                    if (i < 3) {
                        if (!playerIn.capabilities.isCreativeMode) {
                            playerIn.inventory.setInventorySlotContents(playerIn.inventory.currentItem, new ItemStack(Items.BUCKET));
                        }

                        playerIn.triggerAchievement(StatList.field_181725_I);
                        this.setWaterLevel(worldIn, pos, state, 3);
                    }

                    return true;
                } else if (item == Items.GLASS_BOTTLE) {
                    if (i > 0) {
                        if (!playerIn.capabilities.isCreativeMode) {
                            ItemStack itemstack2 = new ItemStack(Items.POTION, 1, 0);

                            if (!playerIn.inventory.addItemStackToInventory(itemstack2)) {
                                worldIn.spawnEntityInWorld(new EntityItem(worldIn, pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D, itemstack2));
                            } else if (playerIn instanceof EntityPlayerMP entityPlayerMP) {
                                entityPlayerMP.sendContainerToPlayer(playerIn.inventoryContainer);
                            }

                            playerIn.triggerAchievement(StatList.field_181726_J);
                            --itemstack.stackSize;

                            if (itemstack.stackSize <= 0) {
                                playerIn.inventory.setInventorySlotContents(playerIn.inventory.currentItem, null);
                            }
                        }

                        this.setWaterLevel(worldIn, pos, state, i - 1);
                    }

                    return true;
                } else {
                    if (i > 0 && item instanceof ItemArmor itemarmor) {

                        if (itemarmor.getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER && itemarmor.hasColor(itemstack)) {
                            itemarmor.removeColor(itemstack);
                            this.setWaterLevel(worldIn, pos, state, i - 1);
                            playerIn.triggerAchievement(StatList.field_181727_K);
                            return true;
                        }
                    }

                    if (i > 0 && item instanceof ItemBanner && TileEntityBanner.getPatterns(itemstack) > 0) {
                        ItemStack itemstack1 = itemstack.copy();
                        itemstack1.stackSize = 1;
                        TileEntityBanner.removeBannerData(itemstack1);

                        if (itemstack.stackSize <= 1 && !playerIn.capabilities.isCreativeMode) {
                            playerIn.inventory.setInventorySlotContents(playerIn.inventory.currentItem, itemstack1);
                        } else {
                            if (!playerIn.inventory.addItemStackToInventory(itemstack1)) {
                                worldIn.spawnEntityInWorld(new EntityItem(worldIn, pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D, itemstack1));
                            } else if (playerIn instanceof EntityPlayerMP entityPlayerMP) {
                                entityPlayerMP.sendContainerToPlayer(playerIn.inventoryContainer);
                            }

                            playerIn.triggerAchievement(StatList.field_181728_L);

                            if (!playerIn.capabilities.isCreativeMode) {
                                --itemstack.stackSize;
                            }
                        }

                        if (!playerIn.capabilities.isCreativeMode) {
                            this.setWaterLevel(worldIn, pos, state, i - 1);
                        }

                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }
    }

    public void setWaterLevel(World worldIn, BlockPos pos, IBlockState state, int level) {
        worldIn.setBlockState(pos, state.withProperty(LEVEL, MathHelper.clamp(level, 0, 3)), 2);
        worldIn.updateComparatorOutputLevel(pos, this);
    }

    public void fillWithRain(World worldIn, BlockPos pos) {
        if (worldIn.rand.nextInt(20) == 1) {
            IBlockState iblockstate = worldIn.getBlockState(pos);

            if (iblockstate.getValue(LEVEL) < 3) {
                worldIn.setBlockState(pos, iblockstate.cycleProperty(LEVEL), 2);
            }
        }
    }

    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.CAULDRON;
    }

    public Item getItem(World worldIn, BlockPos pos) {
        return Items.CAULDRON;
    }

    public boolean hasComparatorInputOverride() {
        return true;
    }

    public int getComparatorInputOverride(World worldIn, BlockPos pos) {
        return worldIn.getBlockState(pos).getValue(LEVEL);
    }

    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(LEVEL, meta);
    }

    public int getMetaFromState(IBlockState state) {
        return state.getValue(LEVEL);
    }

    protected BlockState createBlockState() {
        return new BlockState(this, LEVEL);
    }
}
