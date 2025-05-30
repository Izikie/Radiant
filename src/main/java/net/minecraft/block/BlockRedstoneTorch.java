package net.minecraft.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.ParticleTypes;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.*;

public class BlockRedstoneTorch extends BlockTorch {
    private static final Map<World, List<Toggle>> toggles = new HashMap<>();
    private final boolean isOn;

    private boolean isBurnedOut(World worldIn, BlockPos pos, boolean turnOff) {
        if (!toggles.containsKey(worldIn)) {
            toggles.put(worldIn, new ArrayList<>());
        }

        List<Toggle> list = toggles.get(worldIn);

        if (turnOff) {
            list.add(new Toggle(pos, worldIn.getTotalWorldTime()));
        }

        int i = 0;

        for (Toggle toggle : list) {

            if (toggle.pos.equals(pos)) {
                ++i;

                if (i >= 8) {
                    return true;
                }
            }
        }

        return false;
    }

    protected BlockRedstoneTorch(boolean isOn) {
        this.isOn = isOn;
        this.setTickRandomly(true);
        this.setCreativeTab(null);
    }

    public int tickRate(World worldIn) {
        return 2;
    }

    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        if (this.isOn) {
            for (Direction enumfacing : Direction.values()) {
                worldIn.notifyNeighborsOfStateChange(pos.offset(enumfacing), this);
            }
        }
    }

    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        if (this.isOn) {
            for (Direction enumfacing : Direction.values()) {
                worldIn.notifyNeighborsOfStateChange(pos.offset(enumfacing), this);
            }
        }
    }

    public int getWeakPower(IBlockAccess worldIn, BlockPos pos, IBlockState state, Direction side) {
        return this.isOn && state.getValue(FACING) != side ? 15 : 0;
    }

    private boolean shouldBeOff(World worldIn, BlockPos pos, IBlockState state) {
        Direction enumfacing = state.getValue(FACING).getOpposite();
        return worldIn.isSidePowered(pos.offset(enumfacing), enumfacing);
    }

    public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random random) {
    }

    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        boolean flag = this.shouldBeOff(worldIn, pos, state);
        List<Toggle> list = toggles.get(worldIn);

        while (list != null && !list.isEmpty() && worldIn.getTotalWorldTime() - list.getFirst().time > 60L) {
            list.removeFirst();
        }

        if (this.isOn) {
            if (flag) {
                worldIn.setBlockState(pos, Blocks.UNLIT_REDSTONE_TORCH.getDefaultState().withProperty(FACING, state.getValue(FACING)), 3);

                if (this.isBurnedOut(worldIn, pos, true)) {
                    worldIn.playSoundEffect((pos.getX() + 0.5F), (pos.getY() + 0.5F), (pos.getZ() + 0.5F), "random.fizz", 0.5F, 2.6F + (worldIn.rand.nextFloat() - worldIn.rand.nextFloat()) * 0.8F);

                    for (int i = 0; i < 5; ++i) {
                        double d0 = pos.getX() + rand.nextDouble() * 0.6D + 0.2D;
                        double d1 = pos.getY() + rand.nextDouble() * 0.6D + 0.2D;
                        double d2 = pos.getZ() + rand.nextDouble() * 0.6D + 0.2D;
                        worldIn.spawnParticle(ParticleTypes.SMOKE_NORMAL, d0, d1, d2, 0.0D, 0.0D, 0.0D);
                    }

                    worldIn.scheduleUpdate(pos, worldIn.getBlockState(pos).getBlock(), 160);
                }
            }
        } else if (!flag && !this.isBurnedOut(worldIn, pos, false)) {
            worldIn.setBlockState(pos, Blocks.REDSTONE_TORCH.getDefaultState().withProperty(FACING, state.getValue(FACING)), 3);
        }
    }

    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock) {
        if (!this.onNeighborChangeInternal(worldIn, pos, state)) {
            if (this.isOn == this.shouldBeOff(worldIn, pos, state)) {
                worldIn.scheduleUpdate(pos, this, this.tickRate(worldIn));
            }
        }
    }

    public int getStrongPower(IBlockAccess worldIn, BlockPos pos, IBlockState state, Direction side) {
        return side == Direction.DOWN ? this.getWeakPower(worldIn, pos, state, side) : 0;
    }

    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(Blocks.REDSTONE_TORCH);
    }

    public boolean canProvidePower() {
        return true;
    }

    public void randomDisplayTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (this.isOn) {
            double d0 = pos.getX() + 0.5D + (rand.nextDouble() - 0.5D) * 0.2D;
            double d1 = pos.getY() + 0.7D + (rand.nextDouble() - 0.5D) * 0.2D;
            double d2 = pos.getZ() + 0.5D + (rand.nextDouble() - 0.5D) * 0.2D;
            Direction enumfacing = state.getValue(FACING);

            if (enumfacing.getAxis().isHorizontal()) {
                Direction enumfacing1 = enumfacing.getOpposite();
                double d3 = 0.27D;
                d0 += 0.27D * enumfacing1.getFrontOffsetX();
                d1 += 0.22D;
                d2 += 0.27D * enumfacing1.getFrontOffsetZ();
            }

            worldIn.spawnParticle(ParticleTypes.REDSTONE, d0, d1, d2, 0.0D, 0.0D, 0.0D);
        }
    }

    public Item getItem(World worldIn, BlockPos pos) {
        return Item.getItemFromBlock(Blocks.REDSTONE_TORCH);
    }

    public boolean isAssociatedBlock(Block other) {
        return other == Blocks.UNLIT_REDSTONE_TORCH || other == Blocks.REDSTONE_TORCH;
    }

    record Toggle(BlockPos pos, long time) {
    }
}
