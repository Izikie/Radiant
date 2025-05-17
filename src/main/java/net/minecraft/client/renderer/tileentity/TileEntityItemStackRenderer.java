package net.minecraft.client.renderer.tileentity;

import com.mojang.authlib.GameProfile;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.Direction;

public class TileEntityItemStackRenderer {
    public static final TileEntityItemStackRenderer INSTANCE = new TileEntityItemStackRenderer();
    private final TileEntityChest field_147717_b = new TileEntityChest(0);
    private final TileEntityChest field_147718_c = new TileEntityChest(1);
    private final TileEntityEnderChest enderChest = new TileEntityEnderChest();
    private final TileEntityBanner banner = new TileEntityBanner();
    private final TileEntitySkull skull = new TileEntitySkull();

    public void renderByItem(ItemStack itemStackIn) {
        if (itemStackIn.getItem() == Items.BANNER) {
            this.banner.setItemValues(itemStackIn);
            TileEntityRendererDispatcher.INSTANCE.renderTileEntityAt(this.banner, 0.0D, 0.0D, 0.0D, 0.0F);
        } else if (itemStackIn.getItem() == Items.SKULL) {
            GameProfile gameprofile = null;

            if (itemStackIn.hasTagCompound()) {
                NBTTagCompound nbttagcompound = itemStackIn.getTagCompound();

                if (nbttagcompound.hasKey("SkullOwner", 10)) {
                    gameprofile = NBTUtil.readGameProfileFromNBT(nbttagcompound.getCompoundTag("SkullOwner"));
                } else if (nbttagcompound.hasKey("SkullOwner", 8) && !nbttagcompound.getString("SkullOwner").isEmpty()) {
                    gameprofile = new GameProfile(null, nbttagcompound.getString("SkullOwner"));
                    gameprofile = TileEntitySkull.updateGameprofile(gameprofile);
                    nbttagcompound.removeTag("SkullOwner");
                    nbttagcompound.setTag("SkullOwner", NBTUtil.writeGameProfile(new NBTTagCompound(), gameprofile));
                }
            }

            if (TileEntitySkullRenderer.instance != null) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(-0.5F, 0.0F, -0.5F);
                GlStateManager.scale(2.0F, 2.0F, 2.0F);
                GlStateManager.disableCull();
                TileEntitySkullRenderer.instance.renderSkull(0.0F, 0.0F, 0.0F, Direction.UP, 0.0F, itemStackIn.getMetadata(), gameprofile, -1);
                GlStateManager.enableCull();
                GlStateManager.popMatrix();
            }
        } else {
            Block block = Block.getBlockFromItem(itemStackIn.getItem());

            if (block == Blocks.ENDER_CHEST) {
                TileEntityRendererDispatcher.INSTANCE.renderTileEntityAt(this.enderChest, 0.0D, 0.0D, 0.0D, 0.0F);
            } else if (block == Blocks.TRAPPED_CHEST) {
                TileEntityRendererDispatcher.INSTANCE.renderTileEntityAt(this.field_147718_c, 0.0D, 0.0D, 0.0D, 0.0F);
            } else {
                TileEntityRendererDispatcher.INSTANCE.renderTileEntityAt(this.field_147717_b, 0.0D, 0.0D, 0.0D, 0.0F);
            }
        }
    }
}
