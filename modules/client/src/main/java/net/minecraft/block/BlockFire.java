package net.minecraft.block;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.ParticleTypes;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderEnd;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;

public class BlockFire extends Block {
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 15);
    public static final PropertyBool FLIP = PropertyBool.create("flip");
    public static final PropertyBool ALT = PropertyBool.create("alt");
    public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool EAST = PropertyBool.create("east");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool WEST = PropertyBool.create("west");
    public static final PropertyInteger UPPER = PropertyInteger.create("upper", 0, 2);
    private final Map<Block, Integer> encouragements = new IdentityHashMap<>();
    private final Map<Block, Integer> flammabilities = new IdentityHashMap<>();

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();

        if (!World.doesBlockHaveSolidTopSurface(worldIn, pos.down()) && !Blocks.FIRE.canCatchFire(worldIn, pos.down())) {
            boolean flag = (i + j + k & 1) == 1;
            boolean flag1 = (i / 2 + j / 2 + k / 2 & 1) == 1;
            int l = 0;

            if (this.canCatchFire(worldIn, pos.up())) {
                l = flag ? 1 : 2;
            }

            return state.withProperty(NORTH, this.canCatchFire(worldIn, pos.north())).withProperty(EAST, this.canCatchFire(worldIn, pos.east())).withProperty(SOUTH, this.canCatchFire(worldIn, pos.south())).withProperty(WEST, this.canCatchFire(worldIn, pos.west())).withProperty(UPPER, l).withProperty(FLIP, flag1).withProperty(ALT, flag);
        } else {
            return this.getDefaultState();
        }
    }

    protected BlockFire() {
        super(Material.FIRE);
        this.setDefaultState(this.blockState.getBaseState().withProperty(AGE, 0).withProperty(FLIP, Boolean.FALSE).withProperty(ALT, Boolean.FALSE).withProperty(NORTH, Boolean.FALSE).withProperty(EAST, Boolean.FALSE).withProperty(SOUTH, Boolean.FALSE).withProperty(WEST, Boolean.FALSE).withProperty(UPPER, 0));
        this.setTickRandomly(true);
    }

    public static void init() {
        Blocks.FIRE.setFireInfo(Blocks.PLANKS, 5, 20);
        Blocks.FIRE.setFireInfo(Blocks.DOUBLE_WOODEN_SLAB, 5, 20);
        Blocks.FIRE.setFireInfo(Blocks.WOODEN_SLAB, 5, 20);
        Blocks.FIRE.setFireInfo(Blocks.OAK_FENCE_GATE, 5, 20);
        Blocks.FIRE.setFireInfo(Blocks.SPRUCE_FENCE_GATE, 5, 20);
        Blocks.FIRE.setFireInfo(Blocks.BIRCH_FENCE_GATE, 5, 20);
        Blocks.FIRE.setFireInfo(Blocks.JUNGLE_FENCE_GATE, 5, 20);
        Blocks.FIRE.setFireInfo(Blocks.DARK_OAK_FENCE_GATE, 5, 20);
        Blocks.FIRE.setFireInfo(Blocks.ACACIA_FENCE_GATE, 5, 20);
        Blocks.FIRE.setFireInfo(Blocks.OAK_FENCE, 5, 20);
        Blocks.FIRE.setFireInfo(Blocks.SPRUCE_FENCE, 5, 20);
        Blocks.FIRE.setFireInfo(Blocks.BIRCH_FENCE, 5, 20);
        Blocks.FIRE.setFireInfo(Blocks.JUNGLE_FENCE, 5, 20);
        Blocks.FIRE.setFireInfo(Blocks.DARK_OAK_FENCE, 5, 20);
        Blocks.FIRE.setFireInfo(Blocks.ACACIA_FENCE, 5, 20);
        Blocks.FIRE.setFireInfo(Blocks.OAK_STAIRS, 5, 20);
        Blocks.FIRE.setFireInfo(Blocks.BIRCH_STAIRS, 5, 20);
        Blocks.FIRE.setFireInfo(Blocks.SPRUCE_STAIRS, 5, 20);
        Blocks.FIRE.setFireInfo(Blocks.JUNGLE_STAIRS, 5, 20);
        Blocks.FIRE.setFireInfo(Blocks.LOG, 5, 5);
        Blocks.FIRE.setFireInfo(Blocks.LOG_2, 5, 5);
        Blocks.FIRE.setFireInfo(Blocks.LEAVES, 30, 60);
        Blocks.FIRE.setFireInfo(Blocks.LEAVES_2, 30, 60);
        Blocks.FIRE.setFireInfo(Blocks.BOOKSHELF, 30, 20);
        Blocks.FIRE.setFireInfo(Blocks.TNT, 15, 100);
        Blocks.FIRE.setFireInfo(Blocks.TALL_GRASS, 60, 100);
        Blocks.FIRE.setFireInfo(Blocks.DOUBLE_PLANT, 60, 100);
        Blocks.FIRE.setFireInfo(Blocks.YELLOW_FLOWER, 60, 100);
        Blocks.FIRE.setFireInfo(Blocks.RED_FLOWER, 60, 100);
        Blocks.FIRE.setFireInfo(Blocks.DEAD_BUSH, 60, 100);
        Blocks.FIRE.setFireInfo(Blocks.WOOL, 30, 60);
        Blocks.FIRE.setFireInfo(Blocks.VINE, 15, 100);
        Blocks.FIRE.setFireInfo(Blocks.COAL_BLOCK, 5, 5);
        Blocks.FIRE.setFireInfo(Blocks.HAY_BLOCK, 60, 20);
        Blocks.FIRE.setFireInfo(Blocks.CARPET, 60, 20);
    }

    public void setFireInfo(Block blockIn, int encouragement, int flammability) {
        this.encouragements.put(blockIn, encouragement);
        this.flammabilities.put(blockIn, flammability);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(World worldIn, BlockPos pos, IBlockState state) {
        return null;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean isFullCube() {
        return false;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public int tickRate(World worldIn) {
        return 30;
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (worldIn.getGameRules().getBoolean("doFireTick")) {
            if (!this.canPlaceBlockAt(worldIn, pos)) {
                worldIn.setBlockToAir(pos);
            }

            Block block = worldIn.getBlockState(pos.down()).getBlock();
            boolean flag = block == Blocks.NETHERRACK;

            if (worldIn.provider instanceof WorldProviderEnd && block == Blocks.BEDROCK) {
                flag = true;
            }

            if (!flag && worldIn.isRaining() && this.canDie(worldIn, pos)) {
                worldIn.setBlockToAir(pos);
            } else {
                int i = state.getValue(AGE);

                if (i < 15) {
                    state = state.withProperty(AGE, i + rand.nextInt(3) / 2);
                    worldIn.setBlockState(pos, state, 4);
                }

                worldIn.scheduleUpdate(pos, this, this.tickRate(worldIn) + rand.nextInt(10));

                if (!flag) {
                    if (!this.canNeighborCatchFire(worldIn, pos)) {
                        if (!World.doesBlockHaveSolidTopSurface(worldIn, pos.down()) || i > 3) {
                            worldIn.setBlockToAir(pos);
                        }

                        return;
                    }

                    if (!this.canCatchFire(worldIn, pos.down()) && i == 15 && rand.nextInt(4) == 0) {
                        worldIn.setBlockToAir(pos);
                        return;
                    }
                }

                boolean flag1 = worldIn.isBlockinHighHumidity(pos);
                int j = 0;

                if (flag1) {
                    j = -50;
                }

                this.catchOnFire(worldIn, pos.east(), 300 + j, rand, i);
                this.catchOnFire(worldIn, pos.west(), 300 + j, rand, i);
                this.catchOnFire(worldIn, pos.down(), 250 + j, rand, i);
                this.catchOnFire(worldIn, pos.up(), 250 + j, rand, i);
                this.catchOnFire(worldIn, pos.north(), 300 + j, rand, i);
                this.catchOnFire(worldIn, pos.south(), 300 + j, rand, i);

                for (int k = -1; k <= 1; ++k) {
                    for (int l = -1; l <= 1; ++l) {
                        for (int i1 = -1; i1 <= 4; ++i1) {
                            if (k != 0 || i1 != 0 || l != 0) {
                                int j1 = 100;

                                if (i1 > 1) {
                                    j1 += (i1 - 1) * 100;
                                }

                                BlockPos blockpos = pos.add(k, i1, l);
                                int k1 = this.getNeighborEncouragement(worldIn, blockpos);

                                if (k1 > 0) {
                                    int l1 = (k1 + 40 + worldIn.getDifficulty().getDifficultyId() * 7) / (i + 30);

                                    if (flag1) {
                                        l1 /= 2;
                                    }

                                    if (l1 > 0 && rand.nextInt(j1) <= l1 && (!worldIn.isRaining() || !this.canDie(worldIn, blockpos))) {
                                        int i2 = i + rand.nextInt(5) / 4;

                                        if (i2 > 15) {
                                            i2 = 15;
                                        }

                                        worldIn.setBlockState(blockpos, state.withProperty(AGE, i2), 3);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected boolean canDie(World worldIn, BlockPos pos) {
        return worldIn.isRainingAt(pos) || worldIn.isRainingAt(pos.west()) || worldIn.isRainingAt(pos.east()) || worldIn.isRainingAt(pos.north()) || worldIn.isRainingAt(pos.south());
    }

    @Override
    public boolean requiresUpdates() {
        return false;
    }

    private int getFlammability(Block blockIn) {
        Integer integer = this.flammabilities.get(blockIn);
        return integer == null ? 0 : integer;
    }

    private int getEncouragement(Block blockIn) {
        Integer integer = this.encouragements.get(blockIn);
        return integer == null ? 0 : integer;
    }

    private void catchOnFire(World worldIn, BlockPos pos, int chance, Random random, int age) {
        int i = this.getFlammability(worldIn.getBlockState(pos).getBlock());

        if (random.nextInt(chance) < i) {
            IBlockState iblockstate = worldIn.getBlockState(pos);

            if (random.nextInt(age + 10) < 5 && !worldIn.isRainingAt(pos)) {
                int j = age + random.nextInt(5) / 4;

                if (j > 15) {
                    j = 15;
                }

                worldIn.setBlockState(pos, this.getDefaultState().withProperty(AGE, j), 3);
            } else {
                worldIn.setBlockToAir(pos);
            }

            if (iblockstate.getBlock() == Blocks.TNT) {
                Blocks.TNT.onBlockDestroyedByPlayer(worldIn, pos, iblockstate.withProperty(BlockTNT.EXPLODE, Boolean.TRUE));
            }
        }
    }

    private boolean canNeighborCatchFire(World worldIn, BlockPos pos) {
        for (Direction enumfacing : Direction.values()) {
            if (this.canCatchFire(worldIn, pos.offset(enumfacing))) {
                return true;
            }
        }

        return false;
    }

    private int getNeighborEncouragement(World worldIn, BlockPos pos) {
        if (!worldIn.isAirBlock(pos)) {
            return 0;
        } else {
            int i = 0;

            for (Direction enumfacing : Direction.values()) {
                i = Math.max(this.getEncouragement(worldIn.getBlockState(pos.offset(enumfacing)).getBlock()), i);
            }

            return i;
        }
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    public boolean canCatchFire(IBlockAccess worldIn, BlockPos pos) {
        return this.getEncouragement(worldIn.getBlockState(pos).getBlock()) > 0;
    }

    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        return World.doesBlockHaveSolidTopSurface(worldIn, pos.down()) || this.canNeighborCatchFire(worldIn, pos);
    }

    @Override
    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock) {
        if (!World.doesBlockHaveSolidTopSurface(worldIn, pos.down()) && !this.canNeighborCatchFire(worldIn, pos)) {
            worldIn.setBlockToAir(pos);
        }
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        if (worldIn.provider.getDimensionId() > 0 || !Blocks.PORTAL.func_176548_d(worldIn, pos)) {
            if (!World.doesBlockHaveSolidTopSurface(worldIn, pos.down()) && !this.canNeighborCatchFire(worldIn, pos)) {
                worldIn.setBlockToAir(pos);
            } else {
                worldIn.scheduleUpdate(pos, this, this.tickRate(worldIn) + worldIn.rand.nextInt(10));
            }
        }
    }

    @Override
    public void randomDisplayTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (rand.nextInt(24) == 0) {
            worldIn.playSound((pos.getX() + 0.5F), (pos.getY() + 0.5F), (pos.getZ() + 0.5F), "fire.fire", 1.0F + rand.nextFloat(), rand.nextFloat() * 0.7F + 0.3F, false);
        }

        if (!World.doesBlockHaveSolidTopSurface(worldIn, pos.down()) && !Blocks.FIRE.canCatchFire(worldIn, pos.down())) {
            if (Blocks.FIRE.canCatchFire(worldIn, pos.west())) {
                for (int j = 0; j < 2; ++j) {
                    double d3 = pos.getX() + rand.nextDouble() * 0.10000000149011612D;
                    double d8 = pos.getY() + rand.nextDouble();
                    double d13 = pos.getZ() + rand.nextDouble();
                    worldIn.spawnParticle(ParticleTypes.SMOKE_LARGE, d3, d8, d13, 0.0D, 0.0D, 0.0D);
                }
            }

            if (Blocks.FIRE.canCatchFire(worldIn, pos.east())) {
                for (int k = 0; k < 2; ++k) {
                    double d4 = (pos.getX() + 1) - rand.nextDouble() * 0.10000000149011612D;
                    double d9 = pos.getY() + rand.nextDouble();
                    double d14 = pos.getZ() + rand.nextDouble();
                    worldIn.spawnParticle(ParticleTypes.SMOKE_LARGE, d4, d9, d14, 0.0D, 0.0D, 0.0D);
                }
            }

            if (Blocks.FIRE.canCatchFire(worldIn, pos.north())) {
                for (int l = 0; l < 2; ++l) {
                    double d5 = pos.getX() + rand.nextDouble();
                    double d10 = pos.getY() + rand.nextDouble();
                    double d15 = pos.getZ() + rand.nextDouble() * 0.10000000149011612D;
                    worldIn.spawnParticle(ParticleTypes.SMOKE_LARGE, d5, d10, d15, 0.0D, 0.0D, 0.0D);
                }
            }

            if (Blocks.FIRE.canCatchFire(worldIn, pos.south())) {
                for (int i1 = 0; i1 < 2; ++i1) {
                    double d6 = pos.getX() + rand.nextDouble();
                    double d11 = pos.getY() + rand.nextDouble();
                    double d16 = (pos.getZ() + 1) - rand.nextDouble() * 0.10000000149011612D;
                    worldIn.spawnParticle(ParticleTypes.SMOKE_LARGE, d6, d11, d16, 0.0D, 0.0D, 0.0D);
                }
            }

            if (Blocks.FIRE.canCatchFire(worldIn, pos.up())) {
                for (int j1 = 0; j1 < 2; ++j1) {
                    double d7 = pos.getX() + rand.nextDouble();
                    double d12 = (pos.getY() + 1) - rand.nextDouble() * 0.10000000149011612D;
                    double d17 = pos.getZ() + rand.nextDouble();
                    worldIn.spawnParticle(ParticleTypes.SMOKE_LARGE, d7, d12, d17, 0.0D, 0.0D, 0.0D);
                }
            }
        } else {
            for (int i = 0; i < 3; ++i) {
                double d0 = pos.getX() + rand.nextDouble();
                double d1 = pos.getY() + rand.nextDouble() * 0.5D + 0.5D;
                double d2 = pos.getZ() + rand.nextDouble();
                worldIn.spawnParticle(ParticleTypes.SMOKE_LARGE, d0, d1, d2, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    public MapColor getMapColor(IBlockState state) {
        return MapColor.TNT_COLOR;
    }

    @Override
    public RenderLayer getBlockLayer() {
        return RenderLayer.CUTOUT;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(AGE, meta);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AGE);
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, AGE, NORTH, EAST, SOUTH, WEST, UPPER, FLIP, ALT);
    }
}
