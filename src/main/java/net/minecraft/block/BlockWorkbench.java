package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.stats.StatList;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.Direction;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.IInteractionObject;
import net.minecraft.world.World;

public class BlockWorkbench extends Block {
    protected BlockWorkbench() {
        super(Material.WOOD);
        this.setCreativeTab(CreativeTabs.TAB_DECORATIONS);
    }

    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, Direction side, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            playerIn.displayGui(new InterfaceCraftingTable(worldIn, pos));
            playerIn.triggerAchievement(StatList.field_181742_Z);
        }
        return true;
    }

    public static class InterfaceCraftingTable implements IInteractionObject {
        private final World world;
        private final BlockPos position;

        public InterfaceCraftingTable(World worldIn, BlockPos pos) {
            this.world = worldIn;
            this.position = pos;
        }

        public String getName() {
            return null;
        }

        public boolean hasCustomName() {
            return false;
        }

        public IChatComponent getDisplayName() {
            return new ChatComponentTranslation(Blocks.CRAFTING_TABLE.getUnlocalizedName() + ".name");
        }

        public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {
            return new ContainerWorkbench(playerInventory, this.world, this.position);
        }

        public String getGuiID() {
            return "minecraft:crafting_table";
        }
    }
}
