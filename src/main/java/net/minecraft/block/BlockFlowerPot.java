package net.minecraft.block;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.RenderLayer;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.StatCollector;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockFlowerPot extends BlockContainer {
    public static final PropertyInteger LEGACY_DATA = PropertyInteger.create("legacy_data", 0, 15);
    public static final PropertyEnum<FlowerType> CONTENTS = PropertyEnum.create("contents", FlowerType.class);

    public BlockFlowerPot() {
        super(Material.CIRCUITS);
        this.setDefaultState(this.blockState.getBaseState().withProperty(CONTENTS, FlowerType.EMPTY).withProperty(LEGACY_DATA, 0));
        this.setBlockBoundsForItemRender();
    }

    public String getLocalizedName() {
        return StatCollector.translateToLocal("item.flowerPot.name");
    }

    public void setBlockBoundsForItemRender() {
        float f = 0.375F;
        float f1 = f / 2.0F;
        this.setBlockBounds(0.5F - f1, 0.0F, 0.5F - f1, 0.5F + f1, f, 0.5F + f1);
    }

    public boolean isOpaqueCube() {
        return false;
    }

    public int getRenderType() {
        return 3;
    }

    public boolean isFullCube() {
        return false;
    }

    public int colorMultiplier(IBlockAccess worldIn, BlockPos pos, int renderPass) {
        TileEntity tileentity = worldIn.getTileEntity(pos);

        if (tileentity instanceof TileEntityFlowerPot tileEntityFlowerPot) {
            Item item = tileEntityFlowerPot.getFlowerPotItem();

            if (item instanceof ItemBlock) {
                return Block.getBlockFromItem(item).colorMultiplier(worldIn, pos, renderPass);
            }
        }

        return 16777215;
    }

    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, Direction side, float hitX, float hitY, float hitZ) {
        ItemStack itemstack = playerIn.inventory.getCurrentItem();

        if (itemstack != null && itemstack.getItem() instanceof ItemBlock) {
            TileEntityFlowerPot tileentityflowerpot = this.getTileEntity(worldIn, pos);

            if (tileentityflowerpot == null) {
                return false;
            } else if (tileentityflowerpot.getFlowerPotItem() != null) {
                return false;
            } else {
                Block block = Block.getBlockFromItem(itemstack.getItem());

                if (!this.canNotContain(block, itemstack.getMetadata())) {
                    return false;
                } else {
                    tileentityflowerpot.setFlowerPotData(itemstack.getItem(), itemstack.getMetadata());
                    tileentityflowerpot.markDirty();
                    worldIn.markBlockForUpdate(pos);
                    playerIn.triggerAchievement(StatList.field_181736_T);

                    if (!playerIn.capabilities.isCreativeMode && --itemstack.stackSize <= 0) {
                        playerIn.inventory.setInventorySlotContents(playerIn.inventory.currentItem, null);
                    }

                    return true;
                }
            }
        } else {
            return false;
        }
    }

    private boolean canNotContain(Block blockIn, int meta) {
        return blockIn != Blocks.YELLOW_FLOWER && blockIn != Blocks.RED_FLOWER && blockIn != Blocks.CACTUS && blockIn != Blocks.BROWN_MUSHROOM && blockIn != Blocks.RED_MUSHROOM && blockIn != Blocks.SAPLING && blockIn != Blocks.DEAD_BUSH ? blockIn == Blocks.TALL_GRASS && meta == BlockTallGrass.EnumType.FERN.getMeta() : true;
    }

    public Item getItem(World worldIn, BlockPos pos) {
        TileEntityFlowerPot tileentityflowerpot = this.getTileEntity(worldIn, pos);
        return tileentityflowerpot != null && tileentityflowerpot.getFlowerPotItem() != null ? tileentityflowerpot.getFlowerPotItem() : Items.FLOWER_POT;
    }

    public int getDamageValue(World worldIn, BlockPos pos) {
        TileEntityFlowerPot tileentityflowerpot = this.getTileEntity(worldIn, pos);
        return tileentityflowerpot != null && tileentityflowerpot.getFlowerPotItem() != null ? tileentityflowerpot.getFlowerPotData() : 0;
    }

    public boolean isFlowerPot() {
        return true;
    }

    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        return super.canPlaceBlockAt(worldIn, pos) && World.doesBlockHaveSolidTopSurface(worldIn, pos.down());
    }

    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock) {
        if (!World.doesBlockHaveSolidTopSurface(worldIn, pos.down())) {
            this.dropBlockAsItem(worldIn, pos, state, 0);
            worldIn.setBlockToAir(pos);
        }
    }

    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntityFlowerPot tileentityflowerpot = this.getTileEntity(worldIn, pos);

        if (tileentityflowerpot != null && tileentityflowerpot.getFlowerPotItem() != null) {
            spawnAsEntity(worldIn, pos, new ItemStack(tileentityflowerpot.getFlowerPotItem(), 1, tileentityflowerpot.getFlowerPotData()));
        }

        super.breakBlock(worldIn, pos, state);
    }

    public void onBlockHarvested(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player) {
        super.onBlockHarvested(worldIn, pos, state, player);

        if (player.capabilities.isCreativeMode) {
            TileEntityFlowerPot tileentityflowerpot = this.getTileEntity(worldIn, pos);

            if (tileentityflowerpot != null) {
                tileentityflowerpot.setFlowerPotData(null, 0);
            }
        }
    }

    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.FLOWER_POT;
    }

    private TileEntityFlowerPot getTileEntity(World worldIn, BlockPos pos) {
        TileEntity tileentity = worldIn.getTileEntity(pos);
        return tileentity instanceof TileEntityFlowerPot tileEntityFlowerPot ? tileEntityFlowerPot : null;
    }

    public TileEntity createNewTileEntity(World worldIn, int meta) {
        Block block = null;
        int i = 0;

        switch (meta) {
            case 1:
                block = Blocks.RED_FLOWER;
                i = BlockFlower.FlowerType.POPPY.getMeta();
                break;

            case 2:
                block = Blocks.YELLOW_FLOWER;
                break;

            case 3:
                block = Blocks.SAPLING;
                i = BlockPlanks.WoodType.OAK.getMetadata();
                break;

            case 4:
                block = Blocks.SAPLING;
                i = BlockPlanks.WoodType.SPRUCE.getMetadata();
                break;

            case 5:
                block = Blocks.SAPLING;
                i = BlockPlanks.WoodType.BIRCH.getMetadata();
                break;

            case 6:
                block = Blocks.SAPLING;
                i = BlockPlanks.WoodType.JUNGLE.getMetadata();
                break;

            case 7:
                block = Blocks.RED_MUSHROOM;
                break;

            case 8:
                block = Blocks.BROWN_MUSHROOM;
                break;

            case 9:
                block = Blocks.CACTUS;
                break;

            case 10:
                block = Blocks.DEAD_BUSH;
                break;

            case 11:
                block = Blocks.TALL_GRASS;
                i = BlockTallGrass.EnumType.FERN.getMeta();
                break;

            case 12:
                block = Blocks.SAPLING;
                i = BlockPlanks.WoodType.ACACIA.getMetadata();
                break;

            case 13:
                block = Blocks.SAPLING;
                i = BlockPlanks.WoodType.DARK_OAK.getMetadata();
        }

        return new TileEntityFlowerPot(Item.getItemFromBlock(block), i);
    }

    protected BlockState createBlockState() {
        return new BlockState(this, CONTENTS, LEGACY_DATA);
    }

    public int getMetaFromState(IBlockState state) {
        return state.getValue(LEGACY_DATA);
    }

    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        FlowerType blockflowerpot$enumflowertype = FlowerType.EMPTY;
        TileEntity tileentity = worldIn.getTileEntity(pos);

        if (tileentity instanceof TileEntityFlowerPot tileentityflowerpot) {
            Item item = tileentityflowerpot.getFlowerPotItem();

            if (item instanceof ItemBlock) {
                int i = tileentityflowerpot.getFlowerPotData();
                Block block = Block.getBlockFromItem(item);

                if (block == Blocks.SAPLING) {
                    blockflowerpot$enumflowertype = switch (BlockPlanks.WoodType.byMetadata(i)) {
                        case OAK -> FlowerType.OAK_SAPLING;
                        case SPRUCE -> FlowerType.SPRUCE_SAPLING;
                        case BIRCH -> FlowerType.BIRCH_SAPLING;
                        case JUNGLE -> FlowerType.JUNGLE_SAPLING;
                        case ACACIA -> FlowerType.ACACIA_SAPLING;
                        case DARK_OAK -> FlowerType.DARK_OAK_SAPLING;
                        default -> FlowerType.EMPTY;
                    };
                } else if (block == Blocks.TALL_GRASS) {
                    blockflowerpot$enumflowertype = switch (i) {
                        case 0 -> FlowerType.DEAD_BUSH;
                        case 2 -> FlowerType.FERN;
                        default -> FlowerType.EMPTY;
                    };
                } else if (block == Blocks.YELLOW_FLOWER) {
                    blockflowerpot$enumflowertype = FlowerType.DANDELION;
                } else if (block == Blocks.RED_FLOWER) {
                    blockflowerpot$enumflowertype = switch (BlockFlower.FlowerType.getType(BlockFlower.FlowerColor.RED, i)) {
                        case POPPY -> FlowerType.POPPY;
                        case BLUE_ORCHID -> FlowerType.BLUE_ORCHID;
                        case ALLIUM -> FlowerType.ALLIUM;
                        case HOUSTONIA -> FlowerType.HOUSTONIA;
                        case RED_TULIP -> FlowerType.RED_TULIP;
                        case ORANGE_TULIP -> FlowerType.ORANGE_TULIP;
                        case WHITE_TULIP -> FlowerType.WHITE_TULIP;
                        case PINK_TULIP -> FlowerType.PINK_TULIP;
                        case OXEYE_DAISY -> FlowerType.OXEYE_DAISY;
                        default -> FlowerType.EMPTY;
                    };
                } else if (block == Blocks.RED_MUSHROOM) {
                    blockflowerpot$enumflowertype = FlowerType.MUSHROOM_RED;
                } else if (block == Blocks.BROWN_MUSHROOM) {
                    blockflowerpot$enumflowertype = FlowerType.MUSHROOM_BROWN;
                } else if (block == Blocks.DEAD_BUSH) {
                    blockflowerpot$enumflowertype = FlowerType.DEAD_BUSH;
                } else if (block == Blocks.CACTUS) {
                    blockflowerpot$enumflowertype = FlowerType.CACTUS;
                }
            }
        }

        return state.withProperty(CONTENTS, blockflowerpot$enumflowertype);
    }

    public RenderLayer getBlockLayer() {
        return RenderLayer.CUTOUT;
    }

    public enum FlowerType implements IStringSerializable {
        EMPTY("empty"),
        POPPY("rose"),
        BLUE_ORCHID("blue_orchid"),
        ALLIUM("allium"),
        HOUSTONIA("houstonia"),
        RED_TULIP("red_tulip"),
        ORANGE_TULIP("orange_tulip"),
        WHITE_TULIP("white_tulip"),
        PINK_TULIP("pink_tulip"),
        OXEYE_DAISY("oxeye_daisy"),
        DANDELION("dandelion"),
        OAK_SAPLING("oak_sapling"),
        SPRUCE_SAPLING("spruce_sapling"),
        BIRCH_SAPLING("birch_sapling"),
        JUNGLE_SAPLING("jungle_sapling"),
        ACACIA_SAPLING("acacia_sapling"),
        DARK_OAK_SAPLING("dark_oak_sapling"),
        MUSHROOM_RED("mushroom_red"),
        MUSHROOM_BROWN("mushroom_brown"),
        DEAD_BUSH("dead_bush"),
        FERN("fern"),
        CACTUS("cactus");

        private final String name;

        FlowerType(String name) {
            this.name = name;
        }

        public String toString() {
            return this.name;
        }

        public String getName() {
            return this.name;
        }
    }
}
