package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.*;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeColorHelper;

import java.util.Random;

public abstract class BlockLiquid extends Block {
    public static final PropertyInteger LEVEL = PropertyInteger.create("level", 0, 15);

    protected BlockLiquid(Material materialIn) {
        super(materialIn);
        this.setDefaultState(this.blockState.getBaseState().withProperty(LEVEL, 0));
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        this.setTickRandomly(true);
    }

    public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
        return this.blockMaterial != Material.LAVA;
    }

    public int colorMultiplier(IBlockAccess worldIn, BlockPos pos, int renderPass) {
        return this.blockMaterial == Material.WATER ? BiomeColorHelper.getWaterColorAtPos(worldIn, pos) : 16777215;
    }

    public static float getLiquidHeightPercent(int meta) {
        if (meta >= 8) {
            meta = 0;
        }

        return (meta + 1) / 9.0F;
    }

    protected int getLevel(IBlockAccess worldIn, BlockPos pos) {
        return worldIn.getBlockState(pos).getBlock().getMaterial() == this.blockMaterial ? worldIn.getBlockState(pos).getValue(LEVEL) : -1;
    }

    protected int getEffectiveFlowDecay(IBlockAccess worldIn, BlockPos pos) {
        int i = this.getLevel(worldIn, pos);
        return i >= 8 ? 0 : i;
    }

    public boolean isFullCube() {
        return false;
    }

    public boolean isOpaqueCube() {
        return false;
    }

    public boolean canCollideCheck(IBlockState state, boolean hitIfLiquid) {
        return hitIfLiquid && state.getValue(LEVEL) == 0;
    }

    public boolean isBlockSolid(IBlockAccess worldIn, BlockPos pos, Direction side) {
        Material material = worldIn.getBlockState(pos).getBlock().getMaterial();
        return material == this.blockMaterial ? false : (side == Direction.UP ? true : (material == Material.ICE ? false : super.isBlockSolid(worldIn, pos, side)));
    }

    public boolean shouldSideBeRendered(IBlockAccess worldIn, BlockPos pos, Direction side) {
        return worldIn.getBlockState(pos).getBlock().getMaterial() == this.blockMaterial ? false : (side == Direction.UP ? true : super.shouldSideBeRendered(worldIn, pos, side));
    }

    public boolean shouldRenderSides(IBlockAccess blockAccess, BlockPos pos) {
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                IBlockState iblockstate = blockAccess.getBlockState(pos.add(i, 0, j));
                Block block = iblockstate.getBlock();
                Material material = block.getMaterial();

                if (material != this.blockMaterial && !block.isFullBlock()) {
                    return true;
                }
            }
        }

        return false;
    }

    public AxisAlignedBB getCollisionBoundingBox(World worldIn, BlockPos pos, IBlockState state) {
        return null;
    }

    public int getRenderType() {
        return 1;
    }

    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return null;
    }

    public int quantityDropped(Random random) {
        return 0;
    }

    protected Vec3 getFlowVector(IBlockAccess worldIn, BlockPos pos) {
        Vec3 vec3 = new Vec3(0.0D, 0.0D, 0.0D);
        int i = this.getEffectiveFlowDecay(worldIn, pos);

        for (Direction enumfacing : Direction.Plane.HORIZONTAL) {
            BlockPos blockpos = pos.offset(enumfacing);
            int j = this.getEffectiveFlowDecay(worldIn, blockpos);

            if (j < 0) {
                if (!worldIn.getBlockState(blockpos).getBlock().getMaterial().blocksMovement()) {
                    j = this.getEffectiveFlowDecay(worldIn, blockpos.down());

                    if (j >= 0) {
                        int k = j - (i - 8);
                        vec3 = vec3.addVector(((blockpos.getX() - pos.getX()) * k), ((blockpos.getY() - pos.getY()) * k), ((blockpos.getZ() - pos.getZ()) * k));
                    }
                }
            } else if (j >= 0) {
                int l = j - i;
                vec3 = vec3.addVector(((blockpos.getX() - pos.getX()) * l), ((blockpos.getY() - pos.getY()) * l), ((blockpos.getZ() - pos.getZ()) * l));
            }
        }

        if (worldIn.getBlockState(pos).getValue(LEVEL) >= 8) {
            for (Direction enumfacing1 : Direction.Plane.HORIZONTAL) {
                BlockPos blockpos1 = pos.offset(enumfacing1);

                if (this.isBlockSolid(worldIn, blockpos1, enumfacing1) || this.isBlockSolid(worldIn, blockpos1.up(), enumfacing1)) {
                    vec3 = vec3.normalize().addVector(0.0D, -6.0D, 0.0D);
                    break;
                }
            }
        }

        return vec3.normalize();
    }

    public Vec3 modifyAcceleration(World worldIn, BlockPos pos, Entity entityIn, Vec3 motion) {
        return motion.add(this.getFlowVector(worldIn, pos));
    }

    public int tickRate(World worldIn) {
        return this.blockMaterial == Material.WATER ? 5 : (this.blockMaterial == Material.LAVA ? (worldIn.provider.getHasNoSky() ? 10 : 30) : 0);
    }

    public int getMixedBrightnessForBlock(IBlockAccess worldIn, BlockPos pos) {
        int i = worldIn.getCombinedLight(pos, 0);
        int j = worldIn.getCombinedLight(pos.up(), 0);
        int k = i & 255;
        int l = j & 255;
        int i1 = i >> 16 & 255;
        int j1 = j >> 16 & 255;
        return (k > l ? k : l) | (i1 > j1 ? i1 : j1) << 16;
    }

    public RenderLayer getBlockLayer() {
        return this.blockMaterial == Material.WATER ? RenderLayer.TRANSLUCENT : RenderLayer.SOLID;
    }

    public void randomDisplayTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        double d0 = pos.getX();
        double d1 = pos.getY();
        double d2 = pos.getZ();

        if (this.blockMaterial == Material.WATER) {
            int i = state.getValue(LEVEL);

            if (i > 0 && i < 8) {
                if (rand.nextInt(64) == 0) {
                    worldIn.playSound(d0 + 0.5D, d1 + 0.5D, d2 + 0.5D, "liquid.water", rand.nextFloat() * 0.25F + 0.75F, rand.nextFloat() + 0.5F, false);
                }
            } else if (rand.nextInt(10) == 0) {
                worldIn.spawnParticle(ParticleTypes.SUSPENDED, d0 + rand.nextFloat(), d1 + rand.nextFloat(), d2 + rand.nextFloat(), 0.0D, 0.0D, 0.0D);
            }
        }

        if (this.blockMaterial == Material.LAVA && worldIn.getBlockState(pos.up()).getBlock().getMaterial() == Material.AIR && !worldIn.getBlockState(pos.up()).getBlock().isOpaqueCube()) {
            if (rand.nextInt(100) == 0) {
                double d8 = d0 + rand.nextFloat();
                double d4 = d1 + this.maxY;
                double d6 = d2 + rand.nextFloat();
                worldIn.spawnParticle(ParticleTypes.LAVA, d8, d4, d6, 0.0D, 0.0D, 0.0D);
                worldIn.playSound(d8, d4, d6, "liquid.lavapop", 0.2F + rand.nextFloat() * 0.2F, 0.9F + rand.nextFloat() * 0.15F, false);
            }

            if (rand.nextInt(200) == 0) {
                worldIn.playSound(d0, d1, d2, "liquid.lava", 0.2F + rand.nextFloat() * 0.2F, 0.9F + rand.nextFloat() * 0.15F, false);
            }
        }

        if (rand.nextInt(10) == 0 && World.doesBlockHaveSolidTopSurface(worldIn, pos.down())) {
            Material material = worldIn.getBlockState(pos.down(2)).getBlock().getMaterial();

            if (!material.blocksMovement() && !material.isLiquid()) {
                double d3 = d0 + rand.nextFloat();
                double d5 = d1 - 1.05D;
                double d7 = d2 + rand.nextFloat();

                if (this.blockMaterial == Material.WATER) {
                    worldIn.spawnParticle(ParticleTypes.DRIP_WATER, d3, d5, d7, 0.0D, 0.0D, 0.0D);
                } else {
                    worldIn.spawnParticle(ParticleTypes.DRIP_LAVA, d3, d5, d7, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    public static double getFlowDirection(IBlockAccess worldIn, BlockPos pos, Material materialIn) {
        Vec3 vec3 = getFlowingBlock(materialIn).getFlowVector(worldIn, pos);
        return vec3.xCoord == 0.0D && vec3.zCoord == 0.0D ? -1000.0D : MathHelper.atan2(vec3.zCoord, vec3.xCoord) - (Math.PI / 2.0D);
    }

    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        this.checkForMixing(worldIn, pos, state);
    }

    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock) {
        this.checkForMixing(worldIn, pos, state);
    }

    public boolean checkForMixing(World worldIn, BlockPos pos, IBlockState state) {
        if (this.blockMaterial == Material.LAVA) {
            boolean flag = false;

            for (Direction enumfacing : Direction.values()) {
                if (enumfacing != Direction.DOWN && worldIn.getBlockState(pos.offset(enumfacing)).getBlock().getMaterial() == Material.WATER) {
                    flag = true;
                    break;
                }
            }

            if (flag) {
                Integer integer = state.getValue(LEVEL);

                if (integer == 0) {
                    worldIn.setBlockState(pos, Blocks.OBSIDIAN.getDefaultState());
                    this.triggerMixEffects(worldIn, pos);
                    return true;
                }

                if (integer <= 4) {
                    worldIn.setBlockState(pos, Blocks.COBBLESTONE.getDefaultState());
                    this.triggerMixEffects(worldIn, pos);
                    return true;
                }
            }
        }

        return false;
    }

    protected void triggerMixEffects(World worldIn, BlockPos pos) {
        double d0 = pos.getX();
        double d1 = pos.getY();
        double d2 = pos.getZ();
        worldIn.playSoundEffect(d0 + 0.5D, d1 + 0.5D, d2 + 0.5D, "random.fizz", 0.5F, 2.6F + (worldIn.rand.nextFloat() - worldIn.rand.nextFloat()) * 0.8F);

        for (int i = 0; i < 8; ++i) {
            worldIn.spawnParticle(ParticleTypes.SMOKE_LARGE, d0 + Math.random(), d1 + 1.2D, d2 + Math.random(), 0.0D, 0.0D, 0.0D);
        }
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

    public static BlockDynamicLiquid getFlowingBlock(Material materialIn) {
        if (materialIn == Material.WATER) {
            return Blocks.FLOWING_WATER;
        } else if (materialIn == Material.LAVA) {
            return Blocks.FLOWING_LAVA;
        } else {
            throw new IllegalArgumentException("Invalid material");
        }
    }

    public static BlockStaticLiquid getStaticBlock(Material materialIn) {
        if (materialIn == Material.WATER) {
            return Blocks.WATER;
        } else if (materialIn == Material.LAVA) {
            return Blocks.LAVA;
        } else {
            throw new IllegalArgumentException("Invalid material");
        }
    }
}
