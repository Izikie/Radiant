package net.minecraft.world.gen.feature;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

public class WorldGenSwamp extends WorldGenAbstractTree {
    private static final IBlockState field_181648_a = Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.WoodType.OAK);
    private static final IBlockState field_181649_b = Blocks.LEAVES.getDefaultState().withProperty(BlockOldLeaf.VARIANT, BlockPlanks.WoodType.OAK).withProperty(BlockOldLeaf.CHECK_DECAY, Boolean.FALSE);

    public WorldGenSwamp() {
        super(false);
    }

    public boolean generate(World worldIn, Random rand, BlockPos position) {
        int i;

        for (i = rand.nextInt(4) + 5; worldIn.getBlockState(position.down()).getBlock().getMaterial() == Material.WATER; position = position.down()) {
        }

        boolean flag = true;

        if (position.getY() >= 1 && position.getY() + i + 1 <= 256) {
            for (int j = position.getY(); j <= position.getY() + 1 + i; ++j) {
                int k = 1;

                if (j == position.getY()) {
                    k = 0;
                }

                if (j >= position.getY() + 1 + i - 2) {
                    k = 3;
                }

                BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

                for (int l = position.getX() - k; l <= position.getX() + k && flag; ++l) {
                    for (int i1 = position.getZ() - k; i1 <= position.getZ() + k && flag; ++i1) {
                        if (j >= 0 && j < 256) {
                            Block block = worldIn.getBlockState(blockpos$mutableblockpos.set(l, j, i1)).getBlock();

                            if (block.getMaterial() != Material.AIR && block.getMaterial() != Material.LEAVES) {
                                if (block != Blocks.WATER && block != Blocks.FLOWING_WATER) {
                                    flag = false;
                                } else if (j > position.getY()) {
                                    flag = false;
                                }
                            }
                        } else {
                            flag = false;
                        }
                    }
                }
            }

            if (!flag) {
                return false;
            } else {
                Block block1 = worldIn.getBlockState(position.down()).getBlock();

                if ((block1 == Blocks.GRASS || block1 == Blocks.DIRT) && position.getY() < 256 - i - 1) {
                    this.func_175921_a(worldIn, position.down());

                    for (int l1 = position.getY() - 3 + i; l1 <= position.getY() + i; ++l1) {
                        int k2 = l1 - (position.getY() + i);
                        int i3 = 2 - k2 / 2;

                        for (int k3 = position.getX() - i3; k3 <= position.getX() + i3; ++k3) {
                            int l3 = k3 - position.getX();

                            for (int j1 = position.getZ() - i3; j1 <= position.getZ() + i3; ++j1) {
                                int k1 = j1 - position.getZ();

                                if (Math.abs(l3) != i3 || Math.abs(k1) != i3 || rand.nextInt(2) != 0 && k2 != 0) {
                                    BlockPos blockpos = new BlockPos(k3, l1, j1);

                                    if (!worldIn.getBlockState(blockpos).getBlock().isFullBlock()) {
                                        this.setBlockAndNotifyAdequately(worldIn, blockpos, field_181649_b);
                                    }
                                }
                            }
                        }
                    }

                    for (int i2 = 0; i2 < i; ++i2) {
                        Block block2 = worldIn.getBlockState(position.up(i2)).getBlock();

                        if (block2.getMaterial() == Material.AIR || block2.getMaterial() == Material.LEAVES || block2 == Blocks.FLOWING_WATER || block2 == Blocks.WATER) {
                            this.setBlockAndNotifyAdequately(worldIn, position.up(i2), field_181648_a);
                        }
                    }

                    for (int j2 = position.getY() - 3 + i; j2 <= position.getY() + i; ++j2) {
                        int l2 = j2 - (position.getY() + i);
                        int j3 = 2 - l2 / 2;
                        BlockPos.MutableBlockPos blockpos$mutableblockpos1 = new BlockPos.MutableBlockPos();

                        for (int i4 = position.getX() - j3; i4 <= position.getX() + j3; ++i4) {
                            for (int j4 = position.getZ() - j3; j4 <= position.getZ() + j3; ++j4) {
                                blockpos$mutableblockpos1.set(i4, j2, j4);

                                if (worldIn.getBlockState(blockpos$mutableblockpos1).getBlock().getMaterial() == Material.LEAVES) {
                                    BlockPos blockpos3 = blockpos$mutableblockpos1.west();
                                    BlockPos blockpos4 = blockpos$mutableblockpos1.east();
                                    BlockPos blockpos1 = blockpos$mutableblockpos1.north();
                                    BlockPos blockpos2 = blockpos$mutableblockpos1.south();

                                    if (rand.nextInt(4) == 0 && worldIn.getBlockState(blockpos3).getBlock().getMaterial() == Material.AIR) {
                                        this.func_181647_a(worldIn, blockpos3, BlockVine.EAST);
                                    }

                                    if (rand.nextInt(4) == 0 && worldIn.getBlockState(blockpos4).getBlock().getMaterial() == Material.AIR) {
                                        this.func_181647_a(worldIn, blockpos4, BlockVine.WEST);
                                    }

                                    if (rand.nextInt(4) == 0 && worldIn.getBlockState(blockpos1).getBlock().getMaterial() == Material.AIR) {
                                        this.func_181647_a(worldIn, blockpos1, BlockVine.SOUTH);
                                    }

                                    if (rand.nextInt(4) == 0 && worldIn.getBlockState(blockpos2).getBlock().getMaterial() == Material.AIR) {
                                        this.func_181647_a(worldIn, blockpos2, BlockVine.NORTH);
                                    }
                                }
                            }
                        }
                    }

                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    private void func_181647_a(World p_181647_1_, BlockPos p_181647_2_, PropertyBool p_181647_3_) {
        IBlockState iblockstate = Blocks.VINE.getDefaultState().withProperty(p_181647_3_, Boolean.TRUE);
        this.setBlockAndNotifyAdequately(p_181647_1_, p_181647_2_, iblockstate);
        int i = 4;

        for (p_181647_2_ = p_181647_2_.down(); p_181647_1_.getBlockState(p_181647_2_).getBlock().getMaterial() == Material.AIR && i > 0; --i) {
            this.setBlockAndNotifyAdequately(p_181647_1_, p_181647_2_, iblockstate);
            p_181647_2_ = p_181647_2_.down();
        }
    }
}
