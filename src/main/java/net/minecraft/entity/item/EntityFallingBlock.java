package net.minecraft.entity.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class EntityFallingBlock extends Entity {
    private IBlockState fallTile;
    public int fallTime;
    public boolean shouldDropItem = true;
    private boolean canSetAsBlock;
    private boolean hurtEntities;
    private int fallHurtMax = 40;
    private float fallHurtAmount = 2.0F;
    public NBTTagCompound tileEntityData;

    public EntityFallingBlock(World worldIn) {
        super(worldIn);
    }

    public EntityFallingBlock(World worldIn, double x, double y, double z, IBlockState fallingBlockState) {
        super(worldIn);
        this.fallTile = fallingBlockState;
        this.preventEntitySpawning = true;
        this.setSize(0.98F, 0.98F);
        this.setPosition(x, y, z);
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
        this.prevPosX = x;
        this.prevPosY = y;
        this.prevPosZ = z;
    }

    protected boolean canTriggerWalking() {
        return false;
    }

    protected void entityInit() {
    }

    public boolean canBeCollidedWith() {
        return !this.isDead;
    }

    public void onUpdate() {
        Block block = this.fallTile.getBlock();

        if (block.getMaterial() == Material.AIR) {
            this.setDead();
        } else {
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;

            if (this.fallTime++ == 0) {
                BlockPos blockpos = new BlockPos(this);

                if (this.worldObj.getBlockState(blockpos).getBlock() == block) {
                    this.worldObj.setBlockToAir(blockpos);
                } else if (!this.worldObj.isRemote) {
                    this.setDead();
                    return;
                }
            }

            this.motionY -= 0.03999999910593033D;
            this.moveEntity(this.motionX, this.motionY, this.motionZ);
            this.motionX *= 0.9800000190734863D;
            this.motionY *= 0.9800000190734863D;
            this.motionZ *= 0.9800000190734863D;

            if (!this.worldObj.isRemote) {
                BlockPos blockpos1 = new BlockPos(this);

                if (this.onGround) {
                    this.motionX *= 0.699999988079071D;
                    this.motionZ *= 0.699999988079071D;
                    this.motionY *= -0.5D;

                    if (this.worldObj.getBlockState(blockpos1).getBlock() != Blocks.PISTON_EXTENSION) {
                        this.setDead();

                        if (!this.canSetAsBlock) {
                            if (this.worldObj.canBlockBePlaced(block, blockpos1, true, Direction.UP, null, null) && !BlockFalling.canFallInto(this.worldObj, blockpos1.down()) && this.worldObj.setBlockState(blockpos1, this.fallTile, 3)) {
                                if (block instanceof BlockFalling blockFalling) {
                                    blockFalling.onEndFalling(this.worldObj, blockpos1);
                                }

                                if (this.tileEntityData != null && block instanceof ITileEntityProvider) {
                                    TileEntity tileentity = this.worldObj.getTileEntity(blockpos1);

                                    if (tileentity != null) {
                                        NBTTagCompound nbttagcompound = new NBTTagCompound();
                                        tileentity.writeToNBT(nbttagcompound);

                                        for (String s : this.tileEntityData.getKeySet()) {
                                            NBTBase nbtbase = this.tileEntityData.getTag(s);

                                            if (!s.equals("x") && !s.equals("y") && !s.equals("z")) {
                                                nbttagcompound.setTag(s, nbtbase.copy());
                                            }
                                        }

                                        tileentity.readFromNBT(nbttagcompound);
                                        tileentity.markDirty();
                                    }
                                }
                            } else if (this.shouldDropItem && this.worldObj.getGameRules().getBoolean("doEntityDrops")) {
                                this.entityDropItem(new ItemStack(block, 1, block.damageDropped(this.fallTile)), 0.0F);
                            }
                        }
                    }
                } else if (this.fallTime > 100 && !this.worldObj.isRemote && (blockpos1.getY() < 1 || blockpos1.getY() > 256) || this.fallTime > 600) {
                    if (this.shouldDropItem && this.worldObj.getGameRules().getBoolean("doEntityDrops")) {
                        this.entityDropItem(new ItemStack(block, 1, block.damageDropped(this.fallTile)), 0.0F);
                    }

                    this.setDead();
                }
            }
        }
    }

    public void fall(float distance, float damageMultiplier) {
        Block block = this.fallTile.getBlock();

        if (this.hurtEntities) {
            int i = MathHelper.ceil(distance - 1.0F);

            if (i > 0) {
                List<Entity> list = new ArrayList<>(this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.getEntityBoundingBox()));
                boolean flag = block == Blocks.ANVIL;
                DamageSource damagesource = flag ? DamageSource.ANVIL : DamageSource.FALLING_BLOCK;

                for (Entity entity : list) {
                    entity.attackEntityFrom(damagesource, Math.min(MathHelper.floor(i * this.fallHurtAmount), this.fallHurtMax));
                }

                if (flag && this.rand.nextFloat() < 0.05000000074505806D + i * 0.05D) {
                    int j = this.fallTile.getValue(BlockAnvil.DAMAGE);
                    ++j;

                    if (j > 2) {
                        this.canSetAsBlock = true;
                    } else {
                        this.fallTile = this.fallTile.withProperty(BlockAnvil.DAMAGE, j);
                    }
                }
            }
        }
    }

    protected void writeEntityToNBT(NBTTagCompound tagCompound) {
        Block block = this.fallTile != null ? this.fallTile.getBlock() : Blocks.AIR;
        ResourceLocation resourcelocation = Block.blockRegistry.getNameForObject(block);
        tagCompound.setString("Block", resourcelocation == null ? "" : resourcelocation.toString());
        tagCompound.setByte("Data", (byte) block.getMetaFromState(this.fallTile));
        tagCompound.setByte("Time", (byte) this.fallTime);
        tagCompound.setBoolean("DropItem", this.shouldDropItem);
        tagCompound.setBoolean("HurtEntities", this.hurtEntities);
        tagCompound.setFloat("FallHurtAmount", this.fallHurtAmount);
        tagCompound.setInteger("FallHurtMax", this.fallHurtMax);

        if (this.tileEntityData != null) {
            tagCompound.setTag("TileEntityData", this.tileEntityData);
        }
    }

    protected void readEntityFromNBT(NBTTagCompound tagCompund) {
        int i = tagCompund.getByte("Data") & 255;

        if (tagCompund.hasKey("Block", 8)) {
            this.fallTile = Block.getBlockFromName(tagCompund.getString("Block")).getStateFromMeta(i);
        } else if (tagCompund.hasKey("TileID", 99)) {
            this.fallTile = Block.getBlockById(tagCompund.getInteger("TileID")).getStateFromMeta(i);
        } else {
            this.fallTile = Block.getBlockById(tagCompund.getByte("Tile") & 255).getStateFromMeta(i);
        }

        this.fallTime = tagCompund.getByte("Time") & 255;
        Block block = this.fallTile.getBlock();

        if (tagCompund.hasKey("HurtEntities", 99)) {
            this.hurtEntities = tagCompund.getBoolean("HurtEntities");
            this.fallHurtAmount = tagCompund.getFloat("FallHurtAmount");
            this.fallHurtMax = tagCompund.getInteger("FallHurtMax");
        } else if (block == Blocks.ANVIL) {
            this.hurtEntities = true;
        }

        if (tagCompund.hasKey("DropItem", 99)) {
            this.shouldDropItem = tagCompund.getBoolean("DropItem");
        }

        if (tagCompund.hasKey("TileEntityData", 10)) {
            this.tileEntityData = tagCompund.getCompoundTag("TileEntityData");
        }

        if (block == null || block.getMaterial() == Material.AIR) {
            this.fallTile = Blocks.SAND.getDefaultState();
        }
    }

    public World getWorldObj() {
        return this.worldObj;
    }

    public void setHurtEntities(boolean p_145806_1_) {
        this.hurtEntities = p_145806_1_;
    }

    public boolean canRenderOnFire() {
        return false;
    }

    public void addEntityCrashInfo(CrashReportCategory category) {
        super.addEntityCrashInfo(category);

        if (this.fallTile != null) {
            Block block = this.fallTile.getBlock();
            category.addCrashSection("Imitating Block ID", Block.getIdFromBlock(block));
            category.addCrashSection("Imitating Block Data", block.getMetaFromState(this.fallTile));
        }
    }

    public IBlockState getBlock() {
        return this.fallTile;
    }
}
