package net.minecraft.item;

import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.world.World;

public class ItemMinecart extends Item {
    private static final IBehaviorDispenseItem DISPENSER_MINECART_BEHAVIOR = new BehaviorDefaultDispenseItem() {
        private final BehaviorDefaultDispenseItem behaviourDefaultDispenseItem = new BehaviorDefaultDispenseItem();

        public ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
            Direction enumfacing = BlockDispenser.getFacing(source.getBlockMetadata());
            World world = source.getWorld();
            double d0 = source.getX() + enumfacing.getFrontOffsetX() * 1.125D;
            double d1 = Math.floor(source.getY()) + enumfacing.getFrontOffsetY();
            double d2 = source.getZ() + enumfacing.getFrontOffsetZ() * 1.125D;
            BlockPos blockpos = source.getBlockPos().offset(enumfacing);
            IBlockState iblockstate = world.getBlockState(blockpos);
            BlockRailBase.RailShape railShape = iblockstate.getBlock() instanceof BlockRailBase railBase ? iblockstate.getValue(railBase.getShapeProperty()) : BlockRailBase.RailShape.NORTH_SOUTH;
            double d3;

            if (BlockRailBase.isRailBlock(iblockstate)) {
                if (railShape.isAscending()) {
                    d3 = 0.6D;
                } else {
                    d3 = 0.1D;
                }
            } else {
                if (iblockstate.getBlock().getMaterial() != Material.AIR || !BlockRailBase.isRailBlock(world.getBlockState(blockpos.down()))) {
                    return this.behaviourDefaultDispenseItem.dispense(source, stack);
                }

                IBlockState iblockstate1 = world.getBlockState(blockpos.down());
                BlockRailBase.RailShape railShape1 = iblockstate1.getBlock() instanceof BlockRailBase blockRailBase ? iblockstate1.getValue(blockRailBase.getShapeProperty()) : BlockRailBase.RailShape.NORTH_SOUTH;

                if (enumfacing != Direction.DOWN && railShape1.isAscending()) {
                    d3 = -0.4D;
                } else {
                    d3 = -0.9D;
                }
            }

            EntityMinecart entityminecart = EntityMinecart.getMinecart(world, d0, d1 + d3, d2, ((ItemMinecart) stack.getItem()).minecartType);

            if (stack.hasDisplayName()) {
                entityminecart.setCustomNameTag(stack.getDisplayName());
            }

            world.spawnEntityInWorld(entityminecart);
            stack.splitStack(1);
            return stack;
        }

        protected void playDispenseSound(IBlockSource source) {
            source.getWorld().playAuxSFX(1000, source.getBlockPos(), 0);
        }
    };
    private final EntityMinecart.MinecartType minecartType;

    public ItemMinecart(EntityMinecart.MinecartType type) {
        this.maxStackSize = 1;
        this.minecartType = type;
        this.setCreativeTab(CreativeTabs.TAB_TRANSPORT);
        BlockDispenser.dispenseBehaviorRegistry.putObject(this, DISPENSER_MINECART_BEHAVIOR);
    }

    public boolean onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, Direction side, float hitX, float hitY, float hitZ) {
        IBlockState iblockstate = worldIn.getBlockState(pos);

        if (BlockRailBase.isRailBlock(iblockstate)) {
            if (!worldIn.isRemote) {
                BlockRailBase.RailShape railShape = iblockstate.getBlock() instanceof BlockRailBase blockRailBase ? iblockstate.getValue(blockRailBase.getShapeProperty()) : BlockRailBase.RailShape.NORTH_SOUTH;
                double d0 = 0.0D;

                if (railShape.isAscending()) {
                    d0 = 0.5D;
                }

                EntityMinecart entityminecart = EntityMinecart.getMinecart(worldIn, pos.getX() + 0.5D, pos.getY() + 0.0625D + d0, pos.getZ() + 0.5D, this.minecartType);

                if (stack.hasDisplayName()) {
                    entityminecart.setCustomNameTag(stack.getDisplayName());
                }

                worldIn.spawnEntityInWorld(entityminecart);
            }

            --stack.stackSize;
            return true;
        } else {
            return false;
        }
    }
}
