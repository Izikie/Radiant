package net.minecraft.item;

import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockWallSign;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import java.util.List;

public class ItemBanner extends ItemBlock {
    public ItemBanner() {
        super(Blocks.STANDING_BANNER);
        this.maxStackSize = 16;
        this.setCreativeTab(CreativeTabs.TAB_DECORATIONS);
        this.setHasSubtypes(true);
        this.setMaxDamage(0);
    }

    public boolean onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, Direction side, float hitX, float hitY, float hitZ) {
        if (side == Direction.DOWN) {
            return false;
        } else if (!worldIn.getBlockState(pos).getBlock().getMaterial().isSolid()) {
            return false;
        } else {
            pos = pos.offset(side);

            if (!playerIn.canPlayerEdit(pos, side, stack)) {
                return false;
            } else if (!Blocks.STANDING_BANNER.canPlaceBlockAt(worldIn, pos)) {
                return false;
            } else if (worldIn.isRemote) {
                return true;
            } else {
                if (side == Direction.UP) {
                    int i = MathHelper.floor(((playerIn.rotationYaw + 180.0F) * 16.0F / 360.0F) + 0.5D) & 15;
                    worldIn.setBlockState(pos, Blocks.STANDING_BANNER.getDefaultState().withProperty(BlockStandingSign.ROTATION, i), 3);
                } else {
                    worldIn.setBlockState(pos, Blocks.WALL_BANNER.getDefaultState().withProperty(BlockWallSign.FACING, side), 3);
                }

                --stack.stackSize;
                TileEntity tileentity = worldIn.getTileEntity(pos);

                if (tileentity instanceof TileEntityBanner tileEntityBanner) {
                    tileEntityBanner.setItemValues(stack);
                }

                return true;
            }
        }
    }

    public String getItemStackDisplayName(ItemStack stack) {
        String s = "item.banner.";
        DyeColor enumdyecolor = this.getBaseColor(stack);
        s = s + enumdyecolor.getUnlocalizedName() + ".name";
        return StatCollector.translateToLocal(s);
    }

    public void addInformation(ItemStack stack, EntityPlayer playerIn, List<String> tooltip, boolean advanced) {
        NBTTagCompound nbttagcompound = stack.getSubCompound("BlockEntityTag", false);

        if (nbttagcompound != null && nbttagcompound.hasKey("Patterns")) {
            NBTTagList nbttaglist = nbttagcompound.getTagList("Patterns", 10);

            for (int i = 0; i < nbttaglist.tagCount() && i < 6; ++i) {
                NBTTagCompound nbttagcompound1 = nbttaglist.getCompoundTagAt(i);
                DyeColor enumdyecolor = DyeColor.byDyeDamage(nbttagcompound1.getInteger("Color"));
                TileEntityBanner.BannerPattern tileentitybanner$enumbannerpattern = TileEntityBanner.BannerPattern.getPatternByID(nbttagcompound1.getString("Pattern"));

                if (tileentitybanner$enumbannerpattern != null) {
                    tooltip.add(StatCollector.translateToLocal("item.banner." + tileentitybanner$enumbannerpattern.getPatternName() + "." + enumdyecolor.getUnlocalizedName()));
                }
            }
        }
    }

    public int getColorFromItemStack(ItemStack stack, int renderPass) {
        if (renderPass == 0) {
            return 16777215;
        } else {
            DyeColor enumdyecolor = this.getBaseColor(stack);
            return enumdyecolor.getMapColor().colorValue;
        }
    }

    public void getSubItems(Item itemIn, CreativeTabs tab, List<ItemStack> subItems) {
        for (DyeColor enumdyecolor : DyeColor.values()) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            TileEntityBanner.setBaseColorAndPatterns(nbttagcompound, enumdyecolor.getDyeDamage(), null);
            NBTTagCompound nbttagcompound1 = new NBTTagCompound();
            nbttagcompound1.setTag("BlockEntityTag", nbttagcompound);
            ItemStack itemstack = new ItemStack(itemIn, 1, enumdyecolor.getDyeDamage());
            itemstack.setTagCompound(nbttagcompound1);
            subItems.add(itemstack);
        }
    }

    public CreativeTabs getCreativeTab() {
        return CreativeTabs.TAB_DECORATIONS;
    }

    private DyeColor getBaseColor(ItemStack stack) {
        NBTTagCompound nbttagcompound = stack.getSubCompound("BlockEntityTag", false);
        DyeColor enumdyecolor;

        if (nbttagcompound != null && nbttagcompound.hasKey("Base")) {
            enumdyecolor = DyeColor.byDyeDamage(nbttagcompound.getInteger("Base"));
        } else {
            enumdyecolor = DyeColor.byDyeDamage(stack.getMetadata());
        }

        return enumdyecolor;
    }
}
