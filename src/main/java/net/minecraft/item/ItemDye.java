package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.world.ParticleTypes;
import net.minecraft.world.World;

import java.util.List;

public class ItemDye extends Item {
    public static final int[] DYE_COLORS = new int[]{1973019, 11743532, 3887386, 5320730, 2437522, 8073150, 2651799, 11250603, 4408131, 14188952, 4312372, 14602026, 6719955, 12801229, 15435844, 15790320};

    public ItemDye() {
        this.setHasSubtypes(true);
        this.setMaxDamage(0);
        this.setCreativeTab(CreativeTabs.TAB_MATERIALS);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        int i = stack.getMetadata();
        return super.getUnlocalizedName() + "." + DyeColor.byDyeDamage(i).getUnlocalizedName();
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, Direction side, float hitX, float hitY, float hitZ) {
        if (!playerIn.canPlayerEdit(pos.offset(side), side, stack)) {
            return false;
        } else {
            DyeColor enumdyecolor = DyeColor.byDyeDamage(stack.getMetadata());

            if (enumdyecolor == DyeColor.WHITE) {
                if (applyBonemeal(stack, worldIn, pos)) {
                    if (!worldIn.isRemote) {
                        worldIn.playAuxSFX(2005, pos, 0);
                    }

                    return true;
                }
            } else if (enumdyecolor == DyeColor.BROWN) {
                IBlockState iblockstate = worldIn.getBlockState(pos);
                Block block = iblockstate.getBlock();

                if (block == Blocks.LOG && iblockstate.getValue(BlockPlanks.VARIANT) == BlockPlanks.WoodType.JUNGLE) {
                    if (side == Direction.DOWN) {
                        return false;
                    }

                    if (side == Direction.UP) {
                        return false;
                    }

                    pos = pos.offset(side);

                    if (worldIn.isAirBlock(pos)) {
                        IBlockState iblockstate1 = Blocks.COCOA.onBlockPlaced(worldIn, pos, side, hitX, hitY, hitZ, 0, playerIn);
                        worldIn.setBlockState(pos, iblockstate1, 2);

                        if (!playerIn.capabilities.isCreativeMode) {
                            --stack.stackSize;
                        }
                    }

                    return true;
                }
            }

            return false;
        }
    }

    public static boolean applyBonemeal(ItemStack stack, World worldIn, BlockPos target) {
        IBlockState iblockstate = worldIn.getBlockState(target);

        if (iblockstate.getBlock() instanceof IGrowable igrowable) {

            if (igrowable.canGrow(worldIn, target, iblockstate, worldIn.isRemote)) {
                if (!worldIn.isRemote) {
                    if (igrowable.canUseBonemeal(worldIn, worldIn.rand, target, iblockstate)) {
                        igrowable.grow(worldIn, worldIn.rand, target, iblockstate);
                    }

                    --stack.stackSize;
                }

                return true;
            }
        }

        return false;
    }

    public static void spawnBonemealParticles(World worldIn, BlockPos pos, int amount) {
        if (amount == 0) {
            amount = 15;
        }

        Block block = worldIn.getBlockState(pos).getBlock();

        if (block.getMaterial() != Material.AIR) {
            block.setBlockBoundsBasedOnState(worldIn, pos);

            for (int i = 0; i < amount; ++i) {
                double d0 = ITEM_RAND.nextGaussian() * 0.02D;
                double d1 = ITEM_RAND.nextGaussian() * 0.02D;
                double d2 = ITEM_RAND.nextGaussian() * 0.02D;
                worldIn.spawnParticle(ParticleTypes.VILLAGER_HAPPY, (pos.getX() + ITEM_RAND.nextFloat()), pos.getY() + ITEM_RAND.nextFloat() * block.getBlockBoundsMaxY(), (pos.getZ() + ITEM_RAND.nextFloat()), d0, d1, d2);
            }
        }
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer playerIn, EntityLivingBase target) {
        if (target instanceof EntitySheep entitysheep) {
            DyeColor enumdyecolor = DyeColor.byDyeDamage(stack.getMetadata());

            if (!entitysheep.getSheared() && entitysheep.getFleeceColor() != enumdyecolor) {
                entitysheep.setFleeceColor(enumdyecolor);
                --stack.stackSize;
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void getSubItems(Item itemIn, CreativeTabs tab, List<ItemStack> subItems) {
        for (int i = 0; i < 16; ++i) {
            subItems.add(new ItemStack(itemIn, 1, i));
        }
    }
}
